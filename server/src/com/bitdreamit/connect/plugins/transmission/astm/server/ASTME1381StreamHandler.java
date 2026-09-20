package com.bitdreamit.connect.plugins.transmission.astm.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
 *
 * 5. MULTI-FRAME TRANSMISSIONS (Pentra 400 / per-record ETX analyzers):
 *    The old read() returned after the FIRST ETX frame and dropped the
 *    session (sessionEstablished = false). The next read() re-entered
 *    establishSession(), which consumed the instrument's remaining frames
 *    as "stray bytes" while waiting for an ENQ that never comes, then hit
 *    the establishment timeout and killed the connection. Any analyzer that
 *    sends one record per ETX frame - which is exactly how the Horiba/ABX
 *    Pentra 400 works (manual RAA023JEN paragraphs 6.1/6.2/6.3) - lost every
 *    frame after the first.
 *    Fix: a transmission now ends ONLY on EOT (per ASTM E1381). After each
 *    ACKed frame the handler stays in the transfer state; a boundary scan
 *    consumes the peer's EOT (and any strays) and resynchronizes on ENQ.
 *    Control bytes (ENQ/EOT) are also honored inside the frame loop, so a
 *    late EOT or an immediate next-ENQ can never corrupt a frame payload.
 *
 * 6. SENDER SIDE: HOST-INITIATED ANSWER TURN + RECORD-EXACT FRAMING:
 *    The old write() called establishSession(), which in server mode WAITS
 *    for an incoming ENQ. But when Mirth has data to send (query download,
 *    host answer turn), Mirth IS the sender and per ASTM E1381 the sender
 *    seizes the line by SENDING ENQ. Analyzers that follow the standard
 *    (Pentra 400 manual RAA023JEN 6.2: "<-- <ENQ>" = Host to instrument)
 *    wait for the host's ENQ - the old code deadlocked for the whole
 *    establishment timeout and the response was always lost.
 *    Fix: write() first drains the peer's leftover bytes (its EOT from the
 *    turn that just finished), then establishes as a SENDER (ENQ -> wait
 *    ACK -> retry up to maxTransferAttempts), then frames the payload at
 *    RECORD boundaries (one record per frame, content ends with CR, ETX on
 *    every frame exactly like the manual's host examples; only over-long
 *    records are chunked with ETB), waiting ACK/NAK per frame, and ends
 *    the turn with EOT. Checksums cover FN + content + CR + ETX/ETB,
 *    Add-Mod-256, 2 uppercase hex digits - byte-for-byte the manual's
 *    algorithm.
 *
 * 7. IDLE PATIENCE (long gaps between tubes):
 *    On establishment timeout the old read() threw IOException, which makes
 *    Mirth drop the TCP connection. Instruments that keep the socket open
 *    but stay quiet between tubes (Pentra 400 does) were disconnected every
 *    establishmentTimeout. Fix: read() now returns null ("no message right
 *    now") on an idle line; Mirth's receiver loop simply calls read() again
 *    on the SAME socket. The connection stays open for as long as the
 *    instrument wants.
 *
 * All fixes are RECEIVE/SEND state-machine level and benefit EVERY channel
 * using this transmission mode concurrently - nothing Pentra-specific is
 * wired into the plugin; the business data (what to answer a query with)
 * remains 100% channel/transformer territory.
 * =========================================================================
 */
public class ASTME1381StreamHandler extends StreamHandler {

    private Logger logger = Logger.getLogger(this.getClass());
    private ASTME1381TransmissionModeProperties props;
    private int expectedSequenceNumber = 1; // ASTM frames start at 1, cycle 1-7,0
    private int transferAttemptCount = 0;
    private boolean sessionEstablished = false;

    /**
     * FIX (5): all reads go through a pushback wrapper so the boundary scan
     * can peek at the next byte (STX of a following frame) without losing it.
     * Mirth calls read()/write()/commit() from the single connection thread,
     * so no additional synchronization is required.
     */
    private final PushbackInputStream pin;

    /**
     * FIX (5): how long the boundary scan waits for the peer's next byte
     * after an ACKed frame before concluding that the transmission ended
     * without an EOT (protocol violation). Covers slow serial turnarounds;
     * over TCP/LAN the EOT or next frame is virtually always already buffered.
     */
    private static final long BOUNDARY_WINDOW_MS =
        Long.getLong("bitdreamit.astm.e1381.boundaryWindowMs", 2000L);

    /**
     * FIX (6): quiet window consumed before the host seizes the line, so a
     * late EOT from the instrument's just-finished turn lands BEFORE our ENQ
     * (avoids a one-byte collision on sluggish serial/USR-TCP232 bridges).
     * Well inside every analyzer's host-response timeout.
     */
    private static final long SEND_DRAIN_MS = 500L;

    public ASTME1381StreamHandler(InputStream inputStream, OutputStream outputStream,
                                   BatchStreamReader batchStreamReader,
                                   ASTME1381TransmissionModeProperties props) {
        super(inputStream, outputStream, batchStreamReader);
        this.props = props;
        this.pin = new PushbackInputStream(inputStream, 1);
    }

    @Override
    public byte[] read() throws IOException {
        if (!sessionEstablished) {
            // FIX (7): idle line -> null keeps Mirth's read loop alive on the
            // same socket instead of dropping the connection.
            if (!establishSession()) {
                logger.debug("ASTM E1381: line idle, no ENQ within establishment timeout - keeping connection open");
                return null;
            }
        } else if (!consumeBoundary()) {
            // FIX (5): the previous transmission ended (EOT seen, or the peer
            // went silent without an EOT). Re-arm for the next transmission;
            // if the line stays idle, hand control back to Mirth (null).
            sessionEstablished = false;
            if (!establishSession()) {
                logger.debug("ASTM E1381: line idle after transmission end - keeping connection open");
                return null;
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

            int b = pin.read();
            if (b == -1) {
                if (payload.size() > 0) {
                    break; // EOF with partial data
                }
                return null; // Clean EOF
            }

            // FIX (5): honor session-control bytes wherever they appear, not
            // only during establishment. Compliant senders never place ENQ/EOT
            // inside a frame, so this changes nothing for correct peers - but a
            // late EOT or an immediate next ENQ can no longer pollute a frame.
            if (b == props.getEnquiryByte()) {
                logger.info("ASTM E1381: ENQ received mid-stream - peer started a new transmission, resynchronizing");
                sendACK();
                expectedSequenceNumber = 1;
                transferAttemptCount = 0;
                payload.reset();
                frameBuffer.reset();
                frameStartTime = System.currentTimeMillis();
                continue;
            } else if (b == props.getEndOfTransmissionByte()) {
                logger.info("ASTM E1381: EOT received - transmission complete, returning to IDLE");
                sessionEstablished = false;
                expectedSequenceNumber = 1;
                return null;
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
                        sessionEstablished = false;
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

                // FIX (5): ETX no longer ends the session. Per ASTM E1381 the
                // transfer ends on EOT, and analyzers like the Pentra 400 send
                // every record as its own ETX frame inside one transmission.
                // The next read() runs consumeBoundary() to catch the EOT.

                return result;
            } else {
                payload.write(b);
            }
        }

        return payload.toByteArray();
    }

    /**
     * FIX (5) boundary scan. Called at the top of read() while a transfer is
     * still marked established. Consumes immediately-available control bytes:
     *   EOT  -> transfer over (returns false; caller re-establishes)
     *   ENQ  -> peer immediately started its next transfer (ACK, resync, true)
     *   STX  -> peer already streaming the next frame (unread byte, true)
     *   else -> stray byte, logged and discarded
     * Waits up to BOUNDARY_WINDOW_MS for the FIRST byte; total silence is
     * treated as "transfer ended without EOT" (returns false).
     */
    private boolean consumeBoundary() throws IOException {
        long windowStart = System.currentTimeMillis();
        long deadline = windowStart + BOUNDARY_WINDOW_MS;
        boolean sawByte = false;
        while (true) {
            if (!sawByte && System.currentTimeMillis() - windowStart > BOUNDARY_WINDOW_MS) {
                logger.warn("ASTM E1381: transfer ended without EOT (no bytes after last ACK) - treating as complete");
                return false;
            }
            if (sawByte && System.currentTimeMillis() > deadline) {
                logger.warn("ASTM E1381: boundary scan deadline exceeded - treating transfer as complete");
                return false;
            }
            if (pin.available() == 0) {
                try { Thread.sleep(10); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted in boundary scan", e);
                }
                continue;
            }
            int b = pin.read();
            sawByte = true;
            if (b == props.getEndOfTransmissionByte()) {
                logger.info("ASTM E1381: EOT received - transmission complete, returning to IDLE");
                return false;
            } else if (b == props.getEnquiryByte()) {
                logger.info("ASTM E1381: ENQ received - peer started its next transmission, ACKing and resynchronizing");
                sendACK();
                expectedSequenceNumber = 1;
                transferAttemptCount = 0;
                return true;
            } else if (b == props.getStartOfFrameByte()) {
                logger.debug("ASTM E1381: next frame already streaming - handing STX back to the frame reader");
                pin.unread(b);
                return true;
            } else {
                logger.debug("ASTM E1381: boundary scan discarded stray byte 0x" + Integer.toHexString(b & 0xFF));
            }
        }
    }

    @Override
    public void write(byte[] data) throws IOException {
        // FIX (6): Mirth has data to send -> Mirth is the SENDER. Per E1381
        // the sender seizes the line with ENQ; it must NOT wait for one.
        drainPeerBytesBeforeSend();
        establishAsSender();

        try {
            List<FrameChunk> frames = chunkIntoRecordFrames(data, props.getMaxFrameContentLength());
            int seqNum = 1;

            for (int i = 0; i < frames.size(); i++) {
                FrameChunk chunk = frames.get(i);

                ByteArrayOutputStream frame = new ByteArrayOutputStream();
                frame.write(props.getStartOfFrameByte());
                frame.write('0' + seqNum); // Sequence number as ASCII
                frame.write(chunk.data, 0, chunk.data.length);
                // FIX (6): complete records use ETX - byte-for-byte the manual's
                // host examples (RAA023JEN 6.2: every order frame ends <ETX>).
                // Only intermediate chunks of an over-long record use ETB.
                frame.write(chunk.intermediate ? props.getIntermediateEndOfFrame() : props.getEndOfFrameByte());

                if (props.isUseChecksum()) {
                    byte[] checksum = calculateChecksum(frame.toByteArray());
                    frame.write(checksum);
                }

                frame.write(getTerminatorBytes());

                sendFrameWithRetry(frame.toByteArray());

                // FIX (1): same 1-7,0 cycle for the sender side
                seqNum = (seqNum + 1) % 8;
            }
        } catch (IOException e) {
            sendEOT();
            sessionEstablished = false;
            throw e;
        }

        sendEOT();
        sessionEstablished = false;
        expectedSequenceNumber = 1;
        logger.info("ASTM E1381: host answer turn complete, EOT emitted");
    }

    /** One outbound frame's content plus its ASTM end-of-frame style. */
    private static final class FrameChunk {
        final byte[] data;
        final boolean intermediate; // true -> ETB (record continues), false -> ETX
        FrameChunk(byte[] data, boolean intermediate) { this.data = data; this.intermediate = intermediate; }
    }

    /**
     * FIX (6): record-boundary framing. Splits the payload on CR (0x0D),
     * tolerating CRLF/LF, and emits one frame per record whose content ends
     * with CR - exactly the manual's host examples (RAA023JEN 6.2):
     * &lt;STX&gt;1H|\^&amp;|||ABX...&lt;CR&gt;&lt;ETX&gt;47&lt;CR&gt;&lt;LF&gt;.
     * A record longer than (maxFrameContentLength - 1) bytes is chunked:
     * intermediate chunks are ETB-terminated (no CR), the final chunk of the
     * record carries the CR and the ETX. Frames cycle FN 1-7,0 across the turn.
     */
    private List<FrameChunk> chunkIntoRecordFrames(byte[] data, int maxFrameContentLength) {
        int maxData = Math.max(8, maxFrameContentLength - 1); // -1 room for the FN digit
        List<byte[]> records = new ArrayList<byte[]>();

        int start = 0;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == 0x0D) {
                int len = i - start;
                byte[] withCr = new byte[len + 1];
                System.arraycopy(data, start, withCr, 0, len);
                withCr[len] = 0x0D;
                records.add(withCr);
                start = i + 1;
                if (i + 1 < data.length && data[i + 1] == 0x0A) {
                    i++;      // swallow LF after CR
                    start++;
                }
            }
        }
        if (start < data.length) {
            // trailing record without CR - append one (manual-exact frames)
            int len = data.length - start;
            byte[] withCr = new byte[len + 1];
            System.arraycopy(data, start, withCr, 0, len);
            withCr[len] = 0x0D;
            records.add(withCr);
        }
        if (records.isEmpty() && data.length >= 0) {
            byte[] withCr = new byte[1];
            withCr[0] = 0x0D;
            records.add(withCr);
        }

        List<FrameChunk> frames = new ArrayList<FrameChunk>();
        for (byte[] rec : records) {
            if (rec.length <= maxData) {
                frames.add(new FrameChunk(rec, false)); // complete record -> ETX
            } else {
                int offset = 0;
                while (offset < rec.length) {
                    int len = Math.min(maxData, rec.length - offset);
                    byte[] chunk = new byte[len];
                    System.arraycopy(rec, offset, chunk, 0, len);
                    boolean lastChunk = (offset + len >= rec.length);
                    frames.add(new FrameChunk(chunk, !lastChunk)); // ETB mid-record, ETX on final chunk
                    offset += len;
                }
            }
        }
        return frames;
    }

    /** FIX (6): send one frame, wait ACK, retry on NAK/timeout (stock policy). */
    private void sendFrameWithRetry(byte[] frame) throws IOException {
        boolean acked = false;
        for (int attempt = 0; attempt < props.getMaxTransferAttempts() && !acked; attempt++) {
            outputStream.write(frame);
            outputStream.flush();

            int response = readResponse(props.getResponseTimeout());
            if (response == props.getPositiveAckByte()) {
                acked = true;
            } else if (response == props.getNegativeAckByte()) {
                logger.warn("NAK received, retrying frame (attempt " + (attempt + 1) + ")");
            } else {
                logger.warn("Unexpected response: 0x" + Integer.toHexString(response == -1 ? 0 : response & 0xFF)
                            + ", retrying frame (attempt " + (attempt + 1) + ")");
            }
        }

        if (!acked) {
            throw new IOException("Frame send failed after max attempts");
        }
    }

    /** FIX (6): sender establishment - ENQ, wait ACK, retry, then proceed. */
    private void establishAsSender() throws IOException {
        for (int attempt = 0; attempt < props.getMaxTransferAttempts(); attempt++) {
            outputStream.write(props.getEnquiryByte());
            outputStream.flush();
            logger.info("ASTM E1381: host seizes the line - ENQ sent (attempt " + (attempt + 1) + ")");

            int response = readResponse(props.getResponseTimeout());
            if (response == props.getPositiveAckByte()) {
                logger.info("ASTM E1381: ACK received for host ENQ - line acquired");
                expectedSequenceNumber = 1;
                transferAttemptCount = 0;
                return;
            } else if (response == props.getNegativeAckByte()) {
                logger.warn("ASTM E1381: NAK for host ENQ (attempt " + (attempt + 1) + ")");
            } else {
                logger.warn("ASTM E1381: no ACK for host ENQ (attempt " + (attempt + 1) + ")");
            }
            try { Thread.sleep(200); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while establishing as sender", e);
            }
        }
        throw new IOException("Cannot establish session for write: no ACK for ENQ after "
                              + props.getMaxTransferAttempts() + " attempts");
    }

    /**
     * FIX (6): consume the peer's leftover bytes (its EOT from the turn that
     * just finished, strays) before we transmit. Keeps a late EOT from
     * colliding with our ENQ on slow bridges. Bounded by SEND_DRAIN_MS.
     */
    private void drainPeerBytesBeforeSend() throws IOException {
        long deadline = System.currentTimeMillis() + SEND_DRAIN_MS;
        int drained = 0;
        while (System.currentTimeMillis() < deadline) {
            if (pin.available() == 0) {
                try { Thread.sleep(10); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while draining before send", e);
                }
                continue;
            }
            int b = pin.read();
            drained++;
            if (b == props.getEndOfTransmissionByte()) {
                logger.info("ASTM E1381: drained peer EOT before host turn");
            } else if (b == props.getEnquiryByte()) {
                logger.warn("ASTM E1381: peer ENQ consumed during pre-send drain (peer wanted the line)");
            } else if (b == props.getStartOfFrameByte()) {
                logger.warn("ASTM E1381: peer STX consumed during pre-send drain (unexpected frame from peer)");
            } else {
                logger.debug("ASTM E1381: pre-send drain discarded stray byte 0x" + Integer.toHexString(b & 0xFF));
            }
        }
        if (drained > 0) {
            logger.info("ASTM E1381: pre-send drain consumed " + drained + " byte(s)");
        }
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
        int bytesSeen = 0;
        StringBuilder firstBytes = new StringBuilder();

        if (props.isServerMode()) {
            // Server: wait for ENQ, send ACK.
            // FIX (2): EOT received here is BENIGN - it terminates the PREVIOUS
            // transfer (the sender always sends EOT after the last frame).
            // The old code returned false on EOT and read() threw, so the read
            // cycle died after every message. Per E1381 the receiver goes to
            // IDLE and keeps waiting for the next ENQ.
            int establishmentTimeout = props.getEstablishmentTimeout();
            logger.info("ASTM E1381: waiting for ENQ (0x05) from instrument, timeout=" +
                        establishmentTimeout + "ms, ENQ byte=0x" +
                        Integer.toHexString(props.getEnquiryByte() & 0xFF));
            while (!Thread.currentThread().isInterrupted()) {
                if (establishmentTimeout > 0 &&
                    System.currentTimeMillis() - startTime > establishmentTimeout) {
                    // Idle line: no instrument activity within the timeout.
                    // Log what we DID see (if anything) so the user can diagnose.
                    if (bytesSeen > 0) {
                        logger.warn("ASTM E1381: establishment timeout after seeing " + bytesSeen +
                                    " byte(s): " + firstBytes.toString() +
                                    " - expected ENQ (0x05) as first byte. " +
                                    "Check that the instrument is speaking ASTM E1381 (not raw TCP or HL7 MLLP).");
                    }
                    // FIX (7): a fully idle line is NORMAL (instrument quiet
                    // between tubes). read() converts the false return into a
                    // null (no message) instead of throwing, so Mirth keeps
                    // the socket open.
                    return false;
                }
                try {
                    if (pin.available() > 0) {
                        int b = pin.read();
                        bytesSeen++;
                        if (firstBytes.length() < 32) {
                            firstBytes.append(String.format("0x%02X ", b & 0xFF));
                        }
                        if (b == props.getEnquiryByte()) {
                            logger.info("ASTM E1381: ENQ received after " + bytesSeen +
                                        " byte(s), sending ACK");
                            sendACK();
                            sessionEstablished = true;
                            expectedSequenceNumber = 1;
                            transferAttemptCount = 0;
                            return true;
                        } else if (b == props.getEndOfTransmissionByte()) {
                            logger.info("EOT received during establishment - sender ended previous session, waiting for ENQ");
                            continue;
                        }
                        // any other byte: stray byte before ENQ - log and discard
                        if (bytesSeen <= 5) {
                            logger.info("ASTM E1381: received byte 0x" +
                                        Integer.toHexString(b & 0xFF) + " (char='" +
                                        printableChar(b) + "') while waiting for ENQ - discarding");
                        }
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
            if (pin.available() > 0) {
                return pin.read();
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
            int b = pin.read();
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

    /**
     * Convert a byte to a printable character representation for logging.
     * Control characters (0x00-0x1F) and DEL (0x7F) are shown as their
     * abbreviation (e.g. 0x05 -> "&lt;ENQ&gt;"); printable ASCII is shown as the
     * character itself; everything else is shown as "?".
     */
    private static String printableChar(int b) {
        b = b & 0xFF;
        if (b == 0x05) return "<ENQ>";
        if (b == 0x06) return "<ACK>";
        if (b == 0x15) return "<NAK>";
        if (b == 0x02) return "<STX>";
        if (b == 0x03) return "<ETX>";
        if (b == 0x17) return "<ETB>";
        if (b == 0x04) return "<EOT>";
        if (b == 0x0D) return "<CR>";
        if (b == 0x0A) return "<LF>";
        if (b == 0x00) return "<NUL>";
        if (b >= 0x20 && b < 0x7F) return String.valueOf((char) b);
        return "?";
    }
}
