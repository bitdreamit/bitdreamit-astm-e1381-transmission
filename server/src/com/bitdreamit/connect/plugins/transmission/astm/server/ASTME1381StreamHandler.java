package com.bitdreamit.connect.plugins.transmission.astm.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.apache.log4j.Logger;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Constants;
import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties;
import com.mirth.connect.donkey.server.message.StreamHandler;
import com.mirth.connect.donkey.server.message.batch.BatchStreamReader;

/**
 * ASTM E1381-95 Stream Handler
 * Handles STX/ETX/ETB framing, LRC/Checksum validation, sequence numbers 0-7,
 * ACK/NAK handshaking, and ENQ/EOT session control.
 */
public class ASTME1381StreamHandler extends StreamHandler {

    private Logger logger = Logger.getLogger(this.getClass());
    private ASTME1381TransmissionModeProperties props;
    private int expectedSequenceNumber = 1; // ASTM frames start at 1, wrap 0-7
    private int transferAttemptCount = 0;
    private boolean sessionEstablished = false;

    public ASTME1381StreamHandler(InputStream inputStream, OutputStream outputStream,
                                   BatchStreamReader batchStreamReader,
                                   ASTME1381TransmissionModeProperties props) {
        super(inputStream, outputStream, batchStreamReader);
        this.props = props;
    }

    @Override
    public byte[] read() throws IOException {
        if (!sessionEstablished) {
            if (!establishSession()) {
                throw new IOException("ASTM E1381 session establishment failed");
            }
        }

        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        ByteArrayOutputStream frameBuffer = new ByteArrayOutputStream();
        boolean frameComplete = false;
        boolean isIntermediate = false;
        long frameStartTime = System.currentTimeMillis();

        while (!frameComplete) {
            if (System.currentTimeMillis() - frameStartTime > props.getFrameTimeout()) {
                logger.error("ASTM frame timeout exceeded (" + props.getFrameTimeout() + "ms)");
                throw new IOException("Frame timeout");
            }

            int b = inputStream.read();
            if (b == -1) {
                if (payload.size() > 0) {
                    break; // EOF with partial data
                }
                return null; // Clean EOF
            }

            frameBuffer.write(b);

            if (b == props.getStartOfFrameByte()) {
                // Start of new frame - reset payload (skip any garbage before STX)
                payload.reset();
                frameBuffer.reset();
                frameBuffer.write(b);
                frameStartTime = System.currentTimeMillis();
            } else if (b == props.getIntermediateEndOfFrame() || b == props.getEndOfFrameByte()) {
                isIntermediate = (b == props.getIntermediateEndOfFrame());
                // Read checksum and terminator
                byte[] checksumBytes = readChecksum();
                byte[] terminatorBytes = readTerminator();

                if (checksumBytes != null) {
                    frameBuffer.write(checksumBytes);
                }
                if (terminatorBytes != null) {
                    frameBuffer.write(terminatorBytes);
                }

                // Validate checksum
                if (props.isUseChecksum() && !validateChecksum(payload.toByteArray(), b, checksumBytes)) {
                    transferAttemptCount++;
                    if (transferAttemptCount >= props.getMaxTransferAttempts()) {
                        logger.error("Max transfer attempts exceeded. Sending EOT.");
                        sendEOT();
                        throw new IOException("Max transfer attempts exceeded");
                    }
                    sendNAK();
                    payload.reset();
                    frameBuffer.reset();
                    continue; // Retry
                }

                // Validate sequence number
                if (props.isValidateFrameNumber() && payload.size() > 0) {
                    int seqNum = payload.toByteArray()[0] - '0'; // ASCII digit
                    if (seqNum < 0 || seqNum > 7) {
                        logger.error("Invalid ASTM sequence number: " + seqNum);
                        sendNAK();
                        payload.reset();
                        continue;
                    }
                    if (seqNum != expectedSequenceNumber) {
                        logger.warn("Sequence number mismatch. Expected " + expectedSequenceNumber + ", got " + seqNum);
                        // ASTM spec: NAK and retry
                        sendNAK();
                        payload.reset();
                        continue;
                    }
                    expectedSequenceNumber = (expectedSequenceNumber + 1) % 8;
                    if (expectedSequenceNumber == 0) expectedSequenceNumber = 1; // ASTM uses 1-7,0
                }

                // Strip sequence number from payload if present
                byte[] result = payload.toByteArray();
                if (result.length > 0 && result[0] >= '0' && result[0] <= '7') {
                    result = Arrays.copyOfRange(result, 1, result.length);
                }

                sendACK();
                transferAttemptCount = 0;
                frameComplete = true;

                if (!isIntermediate) {
                    // Final frame - expect EOT or next ENQ
                    sessionEstablished = false;
                }

                return result;
            } else {
                payload.write(b);
            }
        }

        return payload.toByteArray();
    }

    @Override
    public void write(byte[] data) throws IOException {
        if (!sessionEstablished) {
            if (!establishSession()) {
                throw new IOException("Cannot establish session for write");
            }
        }

        // Chunk data into frames respecting maxFrameContentLength
        int offset = 0;
        boolean moreData = true;
        int seqNum = 1;

        while (moreData) {
            int chunkSize = Math.min(data.length - offset, props.getMaxFrameContentLength() - 1); // -1 for seq num
            boolean isLast = (offset + chunkSize >= data.length);

            ByteArrayOutputStream frame = new ByteArrayOutputStream();
            frame.write(props.getStartOfFrameByte());
            frame.write('0' + seqNum); // Sequence number as ASCII
            frame.write(data, offset, chunkSize);
            frame.write(isLast ? props.getEndOfFrameByte() : props.getIntermediateEndOfFrame());

            if (props.isUseChecksum()) {
                byte[] checksum = calculateChecksum(frame.toByteArray());
                frame.write(checksum);
            }

            frame.write(getTerminatorBytes());

            // Send with retry
            boolean acked = false;
            for (int attempt = 0; attempt < props.getMaxTransferAttempts() && !acked; attempt++) {
                outputStream.write(frame.toByteArray());
                outputStream.flush();

                int response = readResponse(props.getResponseTimeout());
                if (response == props.getPositiveAckByte()) {
                    acked = true;
                } else if (response == props.getNegativeAckByte()) {
                    logger.warn("NAK received, retrying frame (attempt " + (attempt + 1) + ")");
                } else {
                    logger.warn("Unexpected response: 0x" + Integer.toHexString(response));
                }
            }

            if (!acked) {
                sendEOT();
                throw new IOException("Frame send failed after max attempts");
            }

            seqNum = (seqNum + 1) % 8;
            if (seqNum == 0) seqNum = 1;
            offset += chunkSize;
            moreData = !isLast;
        }

        sendEOT();
        sessionEstablished = false;
    }

    @Override
    public void commit(boolean success) throws IOException {
        if (!success) {
            sendNAK();
        }
    }

    // --- Session Establishment ---

    private boolean establishSession() throws IOException {
        long startTime = System.currentTimeMillis();

        if (props.isServerMode()) {
            // Server: wait for ENQ, send ACK
            while (System.currentTimeMillis() - startTime < props.getEstablishmentTimeout()) {
                if (inputStream.available() > 0) {
                    int b = inputStream.read();
                    if (b == props.getEnquiryByte()) {
                        sendACK();
                        sessionEstablished = true;
                        expectedSequenceNumber = 1;
                        transferAttemptCount = 0;
                        return true;
                    } else if (b == props.getEndOfTransmissionByte() && !props.isIgnoreServerSideCancel()) {
                        logger.info("EOT received during establishment - canceling session");
                        return false;
                    }
                }
                try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        } else {
            // Client: send ENQ, wait for ACK
            for (int attempt = 0; attempt < props.getMaxTransferAttempts(); attempt++) {
                outputStream.write(props.getEnquiryByte());
                outputStream.flush();
                int response = readResponse(props.getResponseTimeout());
                if (response == props.getPositiveAckByte()) {
                    sessionEstablished = true;
                    expectedSequenceNumber = 1;
                    transferAttemptCount = 0;
                    return true;
                }
                try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }
        return false;
    }

    // --- Control Signals ---

    private void sendACK() throws IOException {
        outputStream.write(props.getPositiveAckByte());
        outputStream.flush();
    }

    private void sendNAK() throws IOException {
        outputStream.write(props.getNegativeAckByte());
        outputStream.flush();
    }

    private void sendEOT() throws IOException {
        outputStream.write(props.getEndOfTransmissionByte());
        outputStream.flush();
    }

    private int readResponse(int timeout) throws IOException {
        long deadline = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < deadline) {
            if (inputStream.available() > 0) {
                return inputStream.read();
            }
            try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        return -1;
    }

    // --- Checksum & Terminator ---

    private byte[] readChecksum() throws IOException {
        if (!props.isUseChecksum()) return new byte[0];
        byte[] checksum = new byte[props.getChecksumByteLength()];
        for (int i = 0; i < checksum.length; i++) {
            int b = inputStream.read();
            if (b == -1) return null;
            checksum[i] = (byte) b;
        }
        return checksum;
    }

    private byte[] readTerminator() throws IOException {
        String term = props.getFrameTerminator();
        if (term == null || term.isEmpty()) return new byte[]{ASTME1381Constants.CR, ASTME1381Constants.LF};

        // Parse hex string like "0x000A" or "0x0D0A"
        term = term.replace("0x", "").replace("0X", "");
        if (term.length() % 2 != 0) term = "0" + term;
        byte[] bytes = new byte[term.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(term.substring(i*2, i*2+2), 16);
        }
        return bytes;
    }

    private byte[] getTerminatorBytes() {
        try {
            return readTerminator();
        } catch (IOException e) {
            return new byte[]{ASTME1381Constants.CR, ASTME1381Constants.LF};
        }
    }

    private byte[] calculateChecksum(byte[] frameData) {
        // ASTM E1381: checksum covers sequence number + text + ETX/ETB
        // Skip STX byte in calculation
        int start = 1; // after STX
        int sum = 0;
        for (int i = start; i < frameData.length; i++) {
            sum += frameData[i] & 0xFF;
        }

        String checksumStr;
        if (ASTME1381Constants.CHECKSUM_XOR.equals(props.getChecksumAlgorithm())) {
            int xor = 0;
            for (int i = start; i < frameData.length; i++) {
                xor ^= frameData[i] & 0xFF;
            }
            checksumStr = String.format("%02X", xor);
        } else if (ASTME1381Constants.CHECKSUM_NONE.equals(props.getChecksumAlgorithm())) {
            return new byte[0];
        } else {
            // Add Mod 256 (default ASTM)
            sum = sum % 256;
            checksumStr = String.format("%02X", sum);
        }

        return checksumStr.getBytes();
    }

    private boolean validateChecksum(byte[] payload, int endByte, byte[] receivedChecksum) {
        if (receivedChecksum == null || receivedChecksum.length == 0) return true;

        // Reconstruct frame for checksum: STX + payload + endByte
        ByteArrayOutputStream frame = new ByteArrayOutputStream();
        frame.write(props.getStartOfFrameByte());
        // Re-add sequence number if it was stripped
        // For validation we need the raw frame - simplified here
        frame.write(payload, 0, payload.length);
        frame.write(endByte);

        byte[] calculated = calculateChecksum(frame.toByteArray());
        return Arrays.equals(calculated, receivedChecksum);
    }
}
