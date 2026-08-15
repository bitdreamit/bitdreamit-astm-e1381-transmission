package com.bitdreamit.connect.plugins.transmission.astm.client;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Frame;
import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381FrameException;
import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381RetryMetrics;
import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Constants;
import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties;
import com.mirth.connect.plugins.TransmissionModeClientProvider;
import com.mirth.connect.model.transmission.TransmissionModeProperties;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.ArrayList;

/**
 * Client-side transmission provider for the ASTM E1381-02 protocol.
 *
 * <p>Extends Mirth's {@code TransmissionModeClientProvider} (the abstract
 * base class for transmission-mode client-side providers in Mirth Connect
 * 3.x / 4.x). The Mirth framework instantiates this class via the
 * {@code createProvider()} factory on
 * {@code ASTME1381TransmissionModeClientPlugin}, then calls
 * {@link #setProperties(TransmissionModeProperties)} to inject the
 * channel's configured properties, and finally calls
 * {@link #send(OutputStream, InputStream, String)} for each outbound
 * message.</p>
 *
 * <p><b>Required abstract method overrides from
 * {@code TransmissionModeClientProvider} (Mirth 4.5+):</b>
 * <ul>
 *   <li>{@code getSampleLabel()} - returns the label shown next to the
 *       "Send Test Message" button in the channel editor.</li>
 *   <li>{@code getSampleValue()} - returns the sample ASTM E1381 message
 *       string (used as the default content of the "Send Test Message"
 *       text area).</li>
 *   <li>{@code getProperties()} - returns the currently-configured
 *       {@link TransmissionModeProperties} instance.</li>
 *   <li>{@code getDefaultProperties()} - returns a fresh properties bean
 *       populated with the ASTM E1381-02 spec defaults.</li>
 *   <li>{@code setProperties(TransmissionModeProperties)} - injects the
 *       channel-configured properties into this provider instance.</li>
 *   <li>{@code checkProperties(TransmissionModeProperties, boolean)} -
 *       validates the supplied properties and returns {@code true} if
 *       they are sane. The second parameter is {@code ignoreMissing}:
 *       when {@code true}, missing/zero values are treated as OK
 *       (the defaults will be filled in later); when {@code false},
 *       missing values are treated as invalid.</li>
 *   <li>{@code resetInvalidProperties()} - resets any property currently
 *       set to an invalid value back to its spec default. Used by Mirth
 *       after {@code checkProperties} returns {@code false} to "fix"
 *       the channel configuration automatically.</li>
 *   <li>{@code getSettingsComponent()} - returns a {@link JComponent}
 *       that lets the Mirth channel editor display/edit the properties
 *       inline. We return a {@link ASTME1381TransmissionModeSettingsPanel}
 *       here so the channel editor and the standalone Settings panel
 *       share the same UI.</li>
 *   <li>{@code send(OutputStream, InputStream, String)} - drives the
 *       ENQ -> ACK -> frames -> EOT flow. Note the third parameter is
 *       a {@code String}, NOT a {@code byte[]} - this matches the
 *       Mirth 3.x / 4.x {@code TransmissionModeClientProvider} abstract
 *       method signature. The original v1.0.x / v1.1.x plugin code
 *       used {@code byte[]} which never actually overrode the parent
 *       method, so Mirth would never have called it.</li>
 * </ul></p>
 */
public class ASTME1381ClientProvider extends TransmissionModeClientProvider {

    private static final Logger logger = Logger.getLogger(ASTME1381ClientProvider.class);

    /** Currently-configured properties. Lazily defaulted in {@link #ensureProps()}. */
    private ASTME1381TransmissionModeProperties props;

    /** Per-instance retry metrics; reset at the start of every send(). */
    private final ASTME1381RetryMetrics metrics = new ASTME1381RetryMetrics();

    // ------------------------------------------------------------------
    // Sample-message overrides (used by Mirth's "Send Test Message" UI)
    // ------------------------------------------------------------------

    /**
     * Label shown next to the "Send Test Message" button in the Mirth
     * channel editor. Override of the abstract method declared on
     * {@code TransmissionModeClientProvider}.
     */
    @Override
    public String getSampleLabel() {
        return "ASTM E1381 Sample";
    }

    /**
     * Sample ASTM E1381 message used by Mirth's "Send Test Message"
     * feature in the channel editor. The value is a minimal but valid
     * ASTM E1381-02 application-layer payload: it is NOT pre-encoded
     * with STX / checksum / CR / LF because
     * {@link #send(OutputStream, InputStream, String)} will re-frame
     * whatever payload it receives - the sample value represents the
     * application-layer payload, not the wire format.
     *
     * <p>Override of the abstract method declared on
     * {@code TransmissionModeClientProvider}.</p>
     */
    @Override
    public String getSampleValue() {
        // Minimal ASTM E1381 application-layer payload. The send() method
        // will wrap this into STX/FN/payload/ETX/checksum/CR/LF frames.
        // The payload below follows the ASTM E1394 (clinical chemistry)
        // record layout that is commonly carried over E1381 framing.
        return "H|\\^&|||ASTM|||||P|1\r"
             + "P|1|||Patient^Test||||||||||||||\r"
             + "O|1|SAMPLE01||ALL||||||||O|||||||\r"
             + "R|1|^^GLU^GLUCOSE|180|mg/dL|70-105|N|||2024\r"
             + "L|1|N\r";
    }

    // ------------------------------------------------------------------
    // Properties lifecycle overrides (used by Mirth's channel editor)
    // ------------------------------------------------------------------

    /**
     * Returns the currently-configured properties, or a fresh defaults
     * bean if none has been injected yet. Never returns {@code null}.
     *
     * <p>Override of the abstract method declared on
     * {@code TransmissionModeClientProvider}.</p>
     */
    @Override
    public TransmissionModeProperties getProperties() {
        ensureProps();
        return props;
    }

    /**
     * Returns a fresh properties bean populated with the ASTM E1381-02
     * spec defaults. Used by Mirth's channel editor when the user
     * creates a new ASTM E1381 transmission mode instance.
     *
     * <p>Each call returns a NEW instance so callers may safely mutate
     * the returned bean without affecting this provider's state.</p>
     *
     * <p>Override of the abstract method declared on
     * {@code TransmissionModeClientProvider}.</p>
     */
    @Override
    public TransmissionModeProperties getDefaultProperties() {
        return new ASTME1381TransmissionModeProperties();
    }

    /**
     * Injects the channel-configured properties into this provider
     * instance. Called by the Mirth framework before any
     * {@link #send(OutputStream, InputStream, String)} invocation.
     *
     * <p>Override of the abstract method declared on
     * {@code TransmissionModeClientProvider}.</p>
     */
    @Override
    public void setProperties(TransmissionModeProperties properties) {
        if (properties == null) {
            // Defensive: Mirth should never call us with null, but if it
            // does, default to spec-compliant values rather than NPE-ing
            // later in send().
            this.props = new ASTME1381TransmissionModeProperties();
            return;
        }
        if (!(properties instanceof ASTME1381TransmissionModeProperties)) {
            throw new IllegalArgumentException(
                "Expected ASTME1381TransmissionModeProperties but got: "
                + properties.getClass().getName());
        }
        this.props = (ASTME1381TransmissionModeProperties) properties;
    }

    /**
     * Validates the supplied properties and returns {@code true} if they
     * are sane. Mirth calls this from the channel editor when the user
     * clicks "Save Changes" or "Validate".
     *
     * @param properties    the properties to validate. If {@code null},
     *                      returns {@code false} (cannot validate null).
     * @param ignoreMissing when {@code true}, missing/zero values are
     *                      treated as OK (the defaults will be filled
     *                      in by {@link #resetInvalidProperties()});
     *                      when {@code false}, missing values are
     *                      treated as invalid.
     * @return {@code true} if every property is within its valid range;
     *         {@code false} otherwise.
     */
    @Override
    public boolean checkProperties(TransmissionModeProperties properties, boolean ignoreMissing) {
        if (properties == null) return false;
        if (!(properties instanceof ASTME1381TransmissionModeProperties)) return false;

        ASTME1381TransmissionModeProperties p =
            (ASTME1381TransmissionModeProperties) properties;

        // --- Frame bytes: must be a single byte (0-255). ---
        if (!inByteRange(p.getEnquiryByte()))           return false;
        if (!inByteRange(p.getStartOfFrameByte()))      return false;
        if (!inByteRange(p.getIntermediateEndOfFrame()))return false;
        if (!inByteRange(p.getEndOfFrameByte()))       return false;
        if (!inByteRange(p.getEndOfTransmissionByte()))return false;
        if (!inByteRange(p.getPositiveAckByte()))      return false;
        if (!inByteRange(p.getNegativeAckByte()))      return false;

        // --- Frame content / checksum lengths: 1-65535. ---
        if (!inPositiveRange(p.getMaxFrameContentLength(), ignoreMissing)) return false;
        if (p.getChecksumByteLength() != 1 && p.getChecksumByteLength() != 2) return false;

        // --- Checksum algorithm: must be one of the known identifiers. ---
        String algo = p.getChecksumAlgorithm();
        if (algo == null
            || (!algo.equals(ASTME1381Constants.CHECKSUM_ADD_MOD_256)
                && !algo.equals(ASTME1381Constants.CHECKSUM_XOR)
                && !algo.equals(ASTME1381Constants.CHECKSUM_NONE))) {
            return false;
        }

        // --- Timeouts: must be positive integers (or zero if ignoreMissing). ---
        if (!inPositiveRange(p.getMaxTransferAttempts(),  ignoreMissing)) return false;
        if (!inPositiveRange(p.getMaxEnqRetries(),         ignoreMissing)) return false;
        if (!inPositiveRange(p.getMaxFrameRetries(),       ignoreMissing)) return false;
        if (!inPositiveRange(p.getEstablishmentTimeout(),  ignoreMissing)) return false;
        if (!inPositiveRange(p.getContentionTimeout(),     ignoreMissing)) return false;
        if (!inPositiveRange(p.getFrameTimeout(),          ignoreMissing)) return false;
        if (!inPositiveRange(p.getResponseTimeout(),       ignoreMissing)) return false;
        if (!inPositiveRange(p.getEnqTimeoutMs(),          ignoreMissing)) return false;
        if (!inPositiveRange(p.getFrameAckTimeoutMs(),     ignoreMissing)) return false;

        // --- Frame number start: must be 0 or 1. ---
        if (p.getFrameNumberStart() != 0 && p.getFrameNumberStart() != 1) return false;

        return true;
    }

    /**
     * Resets any property currently set to an invalid value back to
     * its spec default. Called by Mirth after {@link #checkProperties}
     * returns {@code false} to "auto-fix" the channel configuration.
     *
     * <p>Operates in-place on the bean returned by {@link #getProperties()}.</p>
     */
    @Override
    public void resetInvalidProperties() {
        ensureProps();

        // Reset out-of-range bytes to spec defaults.
        if (!inByteRange(props.getEnquiryByte()))            props.setEnquiryByte(ASTME1381Constants.ENQ);
        if (!inByteRange(props.getStartOfFrameByte()))       props.setStartOfFrameByte(ASTME1381Constants.STX);
        if (!inByteRange(props.getIntermediateEndOfFrame()))props.setIntermediateEndOfFrame(ASTME1381Constants.ETB);
        if (!inByteRange(props.getEndOfFrameByte()))         props.setEndOfFrameByte(ASTME1381Constants.ETX);
        if (!inByteRange(props.getEndOfTransmissionByte())) props.setEndOfTransmissionByte(ASTME1381Constants.EOT);
        if (!inByteRange(props.getPositiveAckByte()))        props.setPositiveAckByte(ASTME1381Constants.ACK);
        if (!inByteRange(props.getNegativeAckByte()))        props.setNegativeAckByte(ASTME1381Constants.NAK);

        // Reset lengths.
        if (props.getMaxFrameContentLength() <= 0) props.setMaxFrameContentLength(ASTME1381Constants.DEFAULT_MAX_FRAME_CONTENT_LENGTH);
        if (props.getChecksumByteLength() != 1 && props.getChecksumByteLength() != 2) props.setChecksumByteLength(ASTME1381Constants.DEFAULT_CHECKSUM_BYTE_LENGTH);

        // Reset checksum algorithm.
        String algo = props.getChecksumAlgorithm();
        if (algo == null
            || (!algo.equals(ASTME1381Constants.CHECKSUM_ADD_MOD_256)
                && !algo.equals(ASTME1381Constants.CHECKSUM_XOR)
                && !algo.equals(ASTME1381Constants.CHECKSUM_NONE))) {
            props.setChecksumAlgorithm(ASTME1381Constants.CHECKSUM_ADD_MOD_256);
        }

        // Reset timeouts.
        if (props.getMaxTransferAttempts()  <= 0) props.setMaxTransferAttempts(ASTME1381Constants.DEFAULT_MAX_TRANSFER_ATTEMPTS);
        if (props.getMaxEnqRetries()        <= 0) props.setMaxEnqRetries(ASTME1381Constants.DEFAULT_MAX_ENQ_RETRIES);
        if (props.getMaxFrameRetries()      <= 0) props.setMaxFrameRetries(ASTME1381Constants.DEFAULT_MAX_FRAME_RETRIES);
        if (props.getEstablishmentTimeout() <= 0) props.setEstablishmentTimeout(ASTME1381Constants.DEFAULT_ESTABLISHMENT_TIMEOUT);
        if (props.getContentionTimeout()     <= 0) props.setContentionTimeout(ASTME1381Constants.DEFAULT_CONTENTION_TIMEOUT);
        if (props.getFrameTimeout()         <= 0) props.setFrameTimeout(ASTME1381Constants.DEFAULT_FRAME_TIMEOUT);
        if (props.getResponseTimeout()      <= 0) props.setResponseTimeout(ASTME1381Constants.DEFAULT_RESPONSE_TIMEOUT);
        if (props.getEnqTimeoutMs()         <= 0) props.setEnqTimeoutMs(ASTME1381Constants.DEFAULT_ENQ_TIMEOUT_MS);
        if (props.getFrameAckTimeoutMs()    <= 0) props.setFrameAckTimeoutMs(ASTME1381Constants.DEFAULT_FRAME_ACK_TIMEOUT_MS);

        // Reset frame number start (must be 0 or 1).
        if (props.getFrameNumberStart() != 0 && props.getFrameNumberStart() != 1) {
            props.setFrameNumberStart(ASTME1381Constants.DEFAULT_FRAME_SEQUENCE_START);
        }
    }

    /**
     * Returns a {@link JComponent} that lets the Mirth channel editor
     * display and edit the properties inline (without popping up a
     * separate dialog).
     *
     * <p>We return a {@link ASTME1381TransmissionModeSettingsPanel}
     * here so the channel editor and the standalone Settings panel
     * share the same UI component. The panel reads/writes the user's
     * {@link java.util.prefs.Preferences} for persistence; the actual
     * channel configuration is stored separately by Mirth's channel
     * XML serializer (which uses the field getters/setters on
     * {@link ASTME1381TransmissionModeProperties}).</p>
     *
     * <p>Override of the abstract method declared on
     * {@code TransmissionModeClientProvider}.</p>
     */
    @Override
    public JComponent getSettingsComponent() {
        return new ASTME1381TransmissionModeSettingsPanel("ASTM E1381");
    }

    // ------------------------------------------------------------------
    // send() - drives the ASTM E1381-02 wire protocol
    // ------------------------------------------------------------------

    /**
     * Drives the ASTM E1381-02 send flow: ENQ -> ACK -> STX/FN/payload/
     * ETX|ETB/checksum/CR/LF per frame -> ACK per frame -> EOT.
     *
     * <p>Override of the abstract method declared on Mirth's
     * {@code TransmissionModeClientProvider}. The third parameter is a
     * {@code String} (per the Mirth 3.x / 4.x API contract), which we
     * convert to bytes using ISO-8859-1 so that char codes 0-255 map
     * 1:1 to byte values 0-255 - this is essential for ASTM E1381
     * because the protocol is byte-oriented and may carry arbitrary
     * 8-bit values (e.g. control bytes embedded in payload).</p>
     *
     * @param out     the wire output stream (TCP / Serial / etc.)
     * @param in      the wire input stream (used to read ACK/NAK)
     * @param message the application-layer message to send (UTF-8 or
     *                ASCII text; will be re-framed by this method)
     * @throws Exception if the peer does not ACK after the configured
     *                   number of retries, or on I/O error
     */
    @Override
    public void send(OutputStream out, InputStream in, String message) throws Exception {
        if (out == null) throw new IllegalArgumentException("OutputStream is null");
        if (in == null)  throw new IllegalArgumentException("InputStream is null");
        ensureProps();
        // Convert the String to bytes using ISO-8859-1 so that bytes 0-255
        // round-trip exactly. ASTM E1381 is byte-oriented; using UTF-8
        // would mangle any byte > 0x7F that the channel may have encoded
        // into the String. ISO-8859-1 is a strict superset of ASCII so
        // plain-ASCII messages are unaffected.
        byte[] data = (message == null)
            ? new byte[0]
            : message.getBytes(Charset.forName("ISO-8859-1"));
        if (data.length == 0) {
            logger.debug("send() called with empty payload - skipping");
            return;
        }
        metrics.reset();
        metrics.markSessionStart();
        List<byte[]> records = splitIntoRecords(data);
        logger.info("ASTM E1381 send session starting (" + records.size()
            + " record(s), " + data.length + " bytes)");
        establishConnection(out, in);
        try {
            int frameNumber = props.getFrameNumberStart();
            for (byte[] record : records) {
                frameNumber = sendRecordChunked(out, in, record, frameNumber);
            }
        } finally {
            out.write(new byte[]{ASTME1381Constants.EOT});
            out.flush();
            logger.info("ASTM E1381 send complete (sent=" + metrics.getFramesSent()
                + ", retries=" + metrics.getFrameRetries()
                + ", naks=" + metrics.getNakCount() + ")");
        }
    }

    // ------------------------------------------------------------------
    // Accessor for the per-instance retry metrics (for unit tests / dashboards)
    // ------------------------------------------------------------------

    public ASTME1381RetryMetrics getMetrics() {
        return metrics;
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /** Lazily default the props field to spec-compliant defaults if null. */
    private void ensureProps() {
        if (props == null) {
            props = new ASTME1381TransmissionModeProperties();
        }
    }

    /** Returns true iff v is a valid unsigned byte (0-255). */
    private static boolean inByteRange(int v) {
        return v >= 0 && v <= 0xFF;
    }

    /**
     * Returns true iff v is positive (or zero when {@code ignoreMissing}).
     * Used by {@link #checkProperties} to validate lengths and timeouts.
     */
    private static boolean inPositiveRange(int v, boolean ignoreMissing) {
        if (v > 0) return true;
        if (v == 0) return ignoreMissing;
        return false;
    }

    private void establishConnection(OutputStream out, InputStream in) throws Exception {
        long backoffBase = ASTME1381Constants.DEFAULT_ENQ_BACKOFF_BASE_MS;
        long backoffCap = ASTME1381Constants.DEFAULT_ENQ_BACKOFF_CAP_MS;
        for (int attempt = 1; attempt <= props.getMaxEnqRetries(); attempt++) {
            out.write(new byte[]{ASTME1381Constants.ENQ});
            out.flush();
            logger.debug("ENQ sent (attempt " + attempt + "/" + props.getMaxEnqRetries() + ")");
            int response = readByteWithTimeout(in, props.getEnqTimeoutMs());
            if (response == ASTME1381Constants.ACK) {
                logger.info("ASTM E1381 session established after " + attempt + " ENQ attempt(s)");
                return;
            }
            if (response == ASTME1381Constants.NAK) {
                metrics.incrementNak();
                metrics.incrementEnqRetry();
                long backoff = Math.min(backoffBase * (1L << (attempt - 1)), backoffCap);
                logger.warn("NAK on ENQ attempt " + attempt + ", backing off " + backoff + "ms");
                Thread.sleep(backoff);
                continue;
            }
            metrics.incrementEnqRetry();
            logger.warn("No ACK/NAK to ENQ (response=" + response + ") on attempt " + attempt);
        }
        throw new ASTME1381FrameException("No ACK to ENQ after " + props.getMaxEnqRetries()
            + " attempts (metrics: " + metrics.getEnqRetries() + " retries, "
            + metrics.getNakCount() + " NAKs)");
    }

    private int sendRecordChunked(OutputStream out, InputStream in, byte[] record, int frameNumber) throws Exception {
        int offset = 0;
        while (offset < record.length) {
            int len = Math.min(props.getMaxFrameContentLength(), record.length - offset);
            boolean isLastChunkOfRecord = (offset + len) >= record.length;
            byte[] chunk = new byte[len];
            System.arraycopy(record, offset, chunk, 0, len);

            ASTME1381Frame frame = new ASTME1381Frame(frameNumber % 8, chunk, isLastChunkOfRecord);
            sendFrameWithRetry(out, in, frame);

            offset += len;
            frameNumber = (frameNumber + 1) % 8;
            if (frameNumber == 0) frameNumber = 1;
        }
        return frameNumber;
    }

    private void sendFrameWithRetry(OutputStream out, InputStream in, ASTME1381Frame frame) throws Exception {
        for (int attempt = 1; attempt <= props.getMaxFrameRetries(); attempt++) {
            out.write(frame.encode());
            out.flush();
            metrics.incrementFramesSent();
            int response = readByteWithTimeout(in, props.getFrameAckTimeoutMs());
            if (response == ASTME1381Constants.ACK) {
                logger.debug("ACK received for frame #" + frame.getFrameNumber());
                return;
            }
            metrics.incrementFrameRetry();
            if (response == ASTME1381Constants.NAK) {
                metrics.incrementNak();
                logger.warn("NAK received for frame #" + frame.getFrameNumber()
                    + " (attempt " + attempt + "/" + props.getMaxFrameRetries() + ")");
            } else {
                logger.warn("No ACK/NAK (response=" + response + ") for frame #"
                    + frame.getFrameNumber() + " (attempt " + attempt + ")");
            }
        }
        throw new ASTME1381FrameException("Frame " + frame.getFrameNumber() + " not ACKed after "
            + props.getMaxFrameRetries() + " retries (total frame retries: "
            + metrics.getFrameRetries() + ")");
    }

    private int readByteWithTimeout(InputStream in, long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (in.available() > 0) {
                return in.read();
            }
            Thread.sleep(10);
        }
        return -1;
    }

    private List<byte[]> splitIntoRecords(byte[] data) {
        List<byte[]> list = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == ASTME1381Constants.CR || data[i] == ASTME1381Constants.LF) {
                if (i > start) {
                    byte[] record = new byte[i - start];
                    System.arraycopy(data, start, record, 0, record.length);
                    list.add(record);
                }
                start = i + 1;
            }
        }
        if (start < data.length) {
            byte[] record = new byte[data.length - start];
            System.arraycopy(data, start, record, 0, record.length);
            list.add(record);
        }
        if (list.isEmpty()) list.add(data);
        return list;
    }
}
