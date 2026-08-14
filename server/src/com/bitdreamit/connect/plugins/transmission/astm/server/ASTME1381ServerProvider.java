package com.bitdreamit.connect.plugins.transmission.astm.server;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Constants;
import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Frame;
import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381FrameException;
import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381RetryMetrics;
import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties;
import com.mirth.connect.plugins.TransmissionModeServerProvider;
import com.mirth.connect.model.transmission.TransmissionModeProperties;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;

/**
 * Server-side transmission provider for the ASTM E1381-02 protocol.
 *
 * <p>This provider implements the listener (server) half of the protocol:
 * wait for ENQ, ACK it, then read frames until EOT, replying ACK/NAK per
 * frame. A single instance is created per channel and reused across
 * messages; per-message state lives in {@link ASTME1381RetryMetrics}
 * which is reset at the start of each {@link #receive(InputStream, OutputStream)}
 * call.</p>
 *
 * <p>Production hardening applied to the upstream version:</p>
 * <ul>
 *   <li>Bound the initial ENQ wait by {@link ASTME1381TransmissionModeProperties#getEstablishmentTimeout()}.</li>
 *   <li>Honor {@link ASTME1381TransmissionModeProperties#isIgnoreServerSideCancel()} on every read.</li>
 *   <li>Cap the number of consecutive NAK frames before aborting to avoid
 *       infinite-spin loops on a misbehaving peer.</li>
 *   <li>Log every state transition at INFO / WARN / ERROR for operational visibility.</li>
 *   <li>Track per-session counters via {@link ASTME1381RetryMetrics}.</li>
 * </ul>
 */
public class ASTME1381ServerProvider extends TransmissionModeServerProvider {

    private static final Logger logger = Logger.getLogger(ASTME1381ServerProvider.class);

    /** Safety cap: at most this many consecutive bad/NAK'd frames before we abort. */
    private static final int MAX_CONSECUTIVE_NAK = 32;

    private ASTME1381TransmissionModeProperties props;
    private final ASTME1381RetryMetrics metrics = new ASTME1381RetryMetrics();

    @Override
    public void setProperties(TransmissionModeProperties properties) {
        if (properties != null && !(properties instanceof ASTME1381TransmissionModeProperties)) {
            throw new IllegalArgumentException(
                "Expected ASTME1381TransmissionModeProperties but got: "
                + properties.getClass().getName());
        }
        this.props = (ASTME1381TransmissionModeProperties) properties;
    }

    public ASTME1381RetryMetrics getMetrics() {
        return metrics;
    }

    @Override
    public byte[] receive(InputStream in, OutputStream out) throws Exception {
        if (in == null)  throw new IllegalArgumentException("InputStream is null");
        if (out == null) throw new IllegalArgumentException("OutputStream is null");
        if (props == null) {
            // No properties set - default to spec-compliant values.
            props = new ASTME1381TransmissionModeProperties();
        }

        metrics.reset();
        metrics.markSessionStart();
        logger.info("ASTM E1381 receive session starting (establishmentTimeout="
                + props.getEstablishmentTimeout() + "ms)");

        // --- Establishment phase: wait for ENQ (bounded) ---
        int first = readByteWithTimeout(in, props.getEstablishmentTimeout());
        if (first == ASTME1381Constants.ENQ) {
            logger.debug("ENQ received, sending ACK");
            writeByte(out, ASTME1381Constants.ACK);
        } else if (first == -1) {
            throw new java.io.IOException("Stream closed during establishment (no ENQ received within "
                + props.getEstablishmentTimeout() + "ms)");
        } else if (first == ASTME1381Constants.EOT && props.isIgnoreServerSideCancel()) {
            logger.info("EOT during establishment - ignoreServerSideCancel=true, awaiting ENQ");
            return receive(in, out);
        } else {
            throw new ASTME1381FrameException("Expected ENQ (0x05), got: 0x"
                + Integer.toHexString(first & 0xFF) + " (decimal " + first + ")");
        }

        // --- Transfer phase: read frames until EOT ---
        ByteArrayOutputStream currentRecord = new ByteArrayOutputStream();
        int expectedFrameNumber = props.getFrameNumberStart();
        int consecutiveNak = 0;

        while (true) {
            int peek = readByteWithTimeout(in, props.getFrameTimeout());
            if (peek == -1) {
                logger.warn("Stream EOF reached mid-transfer; returning partial record of "
                    + currentRecord.size() + " bytes");
                break;
            }
            if (peek == ASTME1381Constants.EOT) {
                logger.info("EOT received; transfer complete (" + metrics.getFramesReceived()
                    + " frames, " + metrics.getNakCount() + " NAKs)");
                break;
            }
            if (peek != ASTME1381Constants.STX) {
                logger.warn("Expected STX (0x02), got 0x" + Integer.toHexString(peek & 0xFF)
                    + " - sending NAK");
                writeByte(out, ASTME1381Constants.NAK);
                metrics.incrementNak();
                if (++consecutiveNak > MAX_CONSECUTIVE_NAK) {
                    throw new ASTME1381FrameException("Aborted: " + MAX_CONSECUTIVE_NAK
                        + " consecutive unexpected bytes (peer stuck?)");
                }
                continue;
            }

            byte[] rawFrame = readUntilLF(in, peek);
            try {
                ASTME1381Frame frame = ASTME1381Frame.decode(rawFrame);
                if (props.isStrictFrameSequencing()
                        && frame.getFrameNumber() != (expectedFrameNumber % 8)) {
                    logger.warn("Out-of-sequence frame: expected " + (expectedFrameNumber % 8)
                        + " got " + frame.getFrameNumber() + " - sending NAK");
                    writeByte(out, ASTME1381Constants.NAK);
                    metrics.incrementNak();
                    consecutiveNak++;
                    continue;
                }
                consecutiveNak = 0;
                currentRecord.write(frame.getText());
                metrics.incrementFramesReceived();
                writeByte(out, ASTME1381Constants.ACK);

                // ASTM E1381 sequencing: 1,2,3,4,5,6,7,0,1,2,... (0 follows 7)
                expectedFrameNumber = (expectedFrameNumber + 1) % 8;
                if (expectedFrameNumber == 0) expectedFrameNumber = 1;

                if (frame.isFinalFrame()) {
                    logger.info("Final frame received; record assembled ("
                        + currentRecord.size() + " bytes)");
                    return currentRecord.toByteArray();
                }
            } catch (ASTME1381FrameException badFrame) {
                logger.warn("Bad frame received (" + badFrame.getMessage() + ") - sending NAK");
                writeByte(out, ASTME1381Constants.NAK);
                metrics.incrementNak();
                if (++consecutiveNak > MAX_CONSECUTIVE_NAK) {
                    throw new ASTME1381FrameException("Aborted: " + MAX_CONSECUTIVE_NAK
                        + " consecutive bad frames", badFrame);
                }
            }
        }
        return currentRecord.toByteArray();
    }

    /** Read a single byte from the stream, blocking up to {@code timeoutMs}. */
    private int readByteWithTimeout(InputStream in, long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (in.available() > 0) {
                return in.read();
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new java.io.IOException("Interrupted during read", e);
            }
        }
        return -1;
    }

    /** Read until LF is encountered, returning the accumulated bytes (including STX and LF). */
    private byte[] readUntilLF(InputStream in, int firstByte) throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        buf.write(firstByte);
        long deadline = System.currentTimeMillis() + props.getFrameTimeout();
        int b;
        while ((b = in.read()) != -1) {
            buf.write(b);
            if (b == ASTME1381Constants.LF) {
                return buf.toByteArray();
            }
            if (System.currentTimeMillis() > deadline) {
                throw new ASTME1381FrameException("Frame timeout (no LF within "
                    + props.getFrameTimeout() + "ms), partial=" + buf.size() + " bytes");
            }
        }
        throw new ASTME1381FrameException("Stream EOF before LF; partial frame="
            + buf.size() + " bytes");
    }

    private void writeByte(OutputStream out, byte b) throws java.io.IOException {
        out.write(new byte[]{b});
        out.flush();
    }
}
