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
 *
 * =========================================================================
 * FIXES:
 *
 * 1. FRAME NUMBER WRAP (no-data / NAK-storm bug):
 *    The old code did:
 *        expectedSequenceNumber = (expectedSequenceNumber + 1) % 8;
 *        if (expectedSequenceNumber == 0) expectedSequenceNumber = 1;
 *    which SKIPS frame number 0. Per ASTM E1381 the receiver numbers frames
 *    1,2,...,7,0,1,2,...  With the old code any message with >= 8 frames
 *    (very common for lab result exports) failed: frame "0" arrived, the
 *    handler expected "1", sent NAK, the instrument resent "0", NAK again,
 *    until maxTransferAttempts -> EOT -> message lost.
 *    Fix: remove the "skip 0" line so the cycle is 1-7,0 as the standard requires.
 *
 * 2. EOT DURING SESSION ESTABLISHMENT:
 *    The sender normally sends EOT after every completed transfer. The old
 *    establishSession() treated that EOT as a cancel (return false) and
 *    read() threw IOException, killing the read cycle after EVERY message.
 *    Per the E1381 receiver state machine, EOT simply returns the receiver
 *    to IDLE. Fix: consume EOT, log it, and keep waiting for the next ENQ.
 *
 * 3. NAK RETRY TIMEOUT:
 *    frameStartTime is now reset when a frame is NAKed and resent, so the
 *    retry gets a fair timeout window instead of inheriting the failed
 *    attempt's elapsed time.
 *
 * 4. CLEAN SHUTDOWN:
 *    Poll loops now honor thread interruption (throw IOException) so
 *    stopping the connector breaks out of a blocking ENQ wait immediately.
 * =========================================================================
 */
public class ASTME1381StreamHandler extends StreamHandler {

    private Logger logger = Logger.getLogger(this.getClass());
    private ASTME1381TransmissionModeProperties props;
    private int expectedSequenceNumber = 1; // ASTM frames start at 1, cycle 1-7,0
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
                    // FIX (3): give the resent frame a fresh timeout window
                    frameStartTime = System.currentTimeMillis();
                    continue; // Retry
                }

                // Validate sequence number
                if (props.isValidateFrameNumber() && payload.size() > 0) {
                    int seqNum = payload.toByteArray()[0] - '0'; // ASCII digit
                    if (seqNum < 0 || seqNum > 7) {
                        logger.error("Invalid ASTM sequence number: " + seqNum);
                        sendNAK();
                        payload.reset();
                        frameStartTime = System.currentTimeMillis();
                        continue;
                    }
                    if (seqNum != expectedSequenceNumber) {
                        logger.warn("Sequence number mismatch. Expected " + expectedSequenceNumber + ", got " + seqNum);
                        // ASTM spec: NAK and retry
                        sendNAK();
                        payload.reset();
                        frameStartTime = System.currentTimeMillis();
                        continue;
                    }
                    // FIX (1): ASTM E1381 frame numbers cycle 1,2,...,7,0,1,...
                    // The old code forced 0 back to 1, skipping frame number 0 entirely,
                    // which NAK-looped every message with >= 8 frames.
                    expectedSequenceNumber = (expectedSequenceNumber + 1) % 8;
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
                    // Final frame - sender will send EOT; next read() re-establishes
                    // the session on the next ENQ. (FIX 2 makes that EOT benign.)
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

            // FIX (1): same 1-7,0 cycle for the sender side
            seqNum = (seqNum + 1) % 8;
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
            // Server: wait for ENQ, send ACK.
            // FIX (2): EOT received here is BENIGN — it terminates the PREVIOUS
            // transfer (the sender always sends EOT after the last frame).
            // The old code returned false on EOT and read() threw, so the read
            // cycle died after every message. Per E1381 the receiver goes to
            // IDLE and keeps waiting for the next ENQ.
            int establishmentTimeout = props.getEstablishmentTimeout();
            while (!Thread.currentThread().isInterrupted()) {
                if (establishmentTimeout > 0 &&
                    System.currentTimeMillis() - startTime > establishmentTimeout) {
                    // Idle line: no instrument activity within the timeout.
                    // Caller retries — the port stays open.
                    return false;
                }
                try {
                    if (inputStream.available() > 0) {
                        int b = inputStream.read();
                        if (b == props.getEnquiryByte()) {
                            sendACK();
                            sessionEstablished = true;
                            expectedSequenceNumber = 1;
                            transferAttemptCount = 0;
                            return true;
                        } else if (b == props.getEndOfTransmissionByte()) {
                            logger.info("EOT received during establishment - sender ended previous session, waiting for ENQ");
                            continue;
                        }
                        // any other byte: stray byte before ENQ — discard
                    }
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    // FIX (4): allow the connector to stop the reader thread cleanly
                    throw new IOException("Interrupted while waiting for ENQ", e);
                } catch (IOException e) {
                    throw e;
                }
            }
            return false;
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
                try { Thread.sleep(500); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for ACK", e);
                }
            }
            return false;
        }
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
            try { Thread.sleep(10); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // FIX (4): clean shutdown support
                throw new IOException("Interrupted while waiting for response", e);
            }
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
