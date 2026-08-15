package com.bitdreamit.connect.plugins.transmission.astm.client;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Constants;
import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties;
import com.mirth.connect.model.transmission.TransmissionModeProperties;
import com.mirth.connect.plugins.TransmissionModeClientProvider;

import javax.swing.*;
import java.util.prefs.Preferences;

/**
 * Client-side (Administrator UI) transmission provider for the ASTM E1381-02 protocol.
 *
 * <p><b>Architectural note (Mirth Connect 3.x / 4.x):</b>
 * Mirth separates transmission-mode logic into two halves:
 * <ul>
 *   <li><b>Client side</b> (this class) - runs inside the Mirth Administrator
 *       UI process. Handles the channel editor's settings panel, sample
 *       message buttons, property validation, and default property
 *       provisioning. <b>Never sends or receives bytes over the wire.</b>
 *       The Mirth framework never calls any "send" method on this class -
 *       if it did, the call would happen inside the user's desktop
 *       Administrator process, not on the server, which would obviously
 *       be wrong.</li>
 *   <li><b>Server side</b> (see {@code ASTME1381TransmissionModePlugin}
 *       and {@code ASTME1381StreamHandler}) - runs inside the Mirth
 *       Server process. The server-side {@code TransmissionModeProvider}
 *       returns a {@code StreamHandler} whose {@code read()} /
 *       {@code write()} methods actually move bytes over the wire (TCP /
 *       Serial). All ENQ / ACK / NAK / EOT / framing / checksum logic
 *       lives there.</li>
 * </ul></p>
 *
 * <p>The v1.1.4 / v1.1.5 plugin code mistakenly put a {@code send(...)}
 * method on this client provider - mirroring logic that already lived in
 * {@code ASTME1381StreamHandler.write(byte[])}. That method was never
 * called by Mirth, and (worse) in Mirth 4.5.2 it is not even declared on
 * {@code TransmissionModeClientProvider}, so the {@code @Override}
 * annotation failed to compile. v1.1.6 removes the entire send-flow
 * from this class. The full wire protocol remains in
 * {@code ASTME1381StreamHandler} (server side).</p>
 *
 * <p><b>Required abstract method overrides from
 * {@code TransmissionModeClientProvider} (Mirth 4.5.2):</b>
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
 *       they are sane. The second parameter is {@code highlight}: when
 *       {@code true}, the channel editor highlights invalid fields in
 *       red; when {@code false}, it just returns the boolean result.</li>
 *   <li>{@code resetInvalidProperties()} - resets any property currently
 *       set to an invalid value back to its spec default. Used by Mirth
 *       after {@code checkProperties} returns {@code false} to "fix"
 *       the channel configuration automatically.</li>
 *   <li>{@code getSettingsComponent()} - returns a {@link JComponent}
 *       that lets the Mirth channel editor display/edit the properties
 *       inline.</li>
 * </ul></p>
 *
 * <p><b>That is eight abstract methods.</b> There is NO {@code send()}
 * method on this class in Mirth 4.5.2 - the wire protocol is the
 * server side's responsibility.</p>
 */
public class ASTME1381ClientProvider extends TransmissionModeClientProvider {

    /** Currently-configured properties. Lazily defaulted in {@link #ensureProps()}. */
    private ASTME1381TransmissionModeProperties props;

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
     * with STX / checksum / CR / LF because the server-side
     * {@code ASTME1381StreamHandler.write(byte[])} will re-frame
     * whatever payload it receives - the sample value represents the
     * application-layer payload, not the wire format.
     *
     * <p>Override of the abstract method declared on
     * {@code TransmissionModeClientProvider}.</p>
     */
    @Override
    public String getSampleValue() {
        // Minimal ASTM E1381 application-layer payload. The server-side
        // StreamHandler.write() will wrap this into STX/FN/payload/ETX/
        // checksum/CR/LF frames. The payload below follows the ASTM E1394
        // (clinical chemistry) record layout that is commonly carried
        // over E1381 framing.
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
     * instance. Called by the Mirth framework when the channel editor
     * loads or saves the channel configuration.
     *
     * <p>Override of the abstract method declared on
     * {@code TransmissionModeClientProvider}.</p>
     */
    @Override
    public void setProperties(TransmissionModeProperties properties) {
        if (properties == null) {
            // Defensive: Mirth should never call us with null, but if it
            // does, default to spec-compliant values rather than NPE-ing
            // later in getProperties() / checkProperties().
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
     * @param highlight     when {@code true}, the channel editor
     *                      highlights invalid fields in red; when
     *                      {@code false}, it just uses the boolean
     *                      return value. This implementation does not
     *                      differentiate - it always validates the same
     *                      way and lets Mirth handle highlighting via
     *                      the boolean result + {@link #resetInvalidProperties()}.
     * @return {@code true} if every property is within its valid range;
     *         {@code false} otherwise.
     */
    @Override
    public boolean checkProperties(TransmissionModeProperties properties, boolean highlight) {
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

        // --- Frame content / checksum lengths. ---
        if (p.getMaxFrameContentLength() <= 0) return false;
        if (p.getChecksumByteLength() != 1 && p.getChecksumByteLength() != 2) return false;

        // --- Checksum algorithm: must be one of the known identifiers. ---
        String algo = p.getChecksumAlgorithm();
        if (algo == null
            || (!algo.equals(ASTME1381Constants.CHECKSUM_ADD_MOD_256)
                && !algo.equals(ASTME1381Constants.CHECKSUM_XOR)
                && !algo.equals(ASTME1381Constants.CHECKSUM_NONE))) {
            return false;
        }

        // --- Timeouts / retry counts: must be positive integers. ---
        if (p.getMaxTransferAttempts()  <= 0) return false;
        if (p.getMaxEnqRetries()        <= 0) return false;
        if (p.getMaxFrameRetries()      <= 0) return false;
        if (p.getEstablishmentTimeout() <= 0) return false;
        if (p.getContentionTimeout()     <= 0) return false;
        if (p.getFrameTimeout()         <= 0) return false;
        if (p.getResponseTimeout()       <= 0) return false;
        if (p.getEnqTimeoutMs()         <= 0) return false;
        if (p.getFrameAckTimeoutMs()    <= 0) return false;

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
        if (props.getResponseTimeout()       <= 0) props.setResponseTimeout(ASTME1381Constants.DEFAULT_RESPONSE_TIMEOUT);
        if (props.getEnqTimeoutMs()         <= 0) props.setEnqTimeoutMs(ASTME1381Constants.DEFAULT_ENQ_TIMEOUT_MS);
        if (props.getFrameAckTimeoutMs()    <= 0) props.setFrameAckTimeoutMs(ASTME1381Constants.DEFAULT_FRAME_ACK_TIMEOUT_MS);

        // Reset frame number start (must be 0 or 1).
        if (props.getFrameNumberStart() != 0 && props.getFrameNumberStart() != 1) {
            props.setFrameNumberStart(ASTME1381Constants.DEFAULT_FRAME_SEQUENCE_START);
        }

        // Persist the reset values back to user Preferences so the
        // settings panel (which reads from Preferences) stays in sync
        // with the channel-configured bean. The channel XML serializer
        // also stores these values in the channel itself.
        persistToPreferences();
    }

    /**
     * Returns a {@link JComponent} that lets the Mirth channel editor
     * display and edit the properties inline (without popping up a
     * separate dialog).
     *
     * <p>We return a {@link ASTME1381TransmissionModeSettingsPanel}
     * here so the channel editor and the standalone Settings panel
     * share the same UI component. The panel reads/writes the user's
     * {@link Preferences} for persistence; the actual channel
     * configuration is stored separately by Mirth's channel XML
     * serializer (which uses the field getters/setters on
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
     * Persist the current props back to user Preferences so the settings
     * panel stays in sync. Called by {@link #resetInvalidProperties()}.
     */
    private void persistToPreferences() {
        // The settings panel reads from Preferences.userNodeForPackage(
        // ASTME1381TransmissionModeSettingsPanel.class) using the
        // "com.bitdreamit.astm.e1381." prefix. We mirror those keys here
        // so the panel shows the reset values the next time it loads.
        Preferences p = Preferences.userNodeForPackage(ASTME1381TransmissionModeSettingsPanel.class);
        final String PREFIX = "com.bitdreamit.astm.e1381.";
        p.put(PREFIX + "enquiry",            String.format("0x%02X", props.getEnquiryByte()));
        p.put(PREFIX + "stx",                 String.format("0x%02X", props.getStartOfFrameByte()));
        p.put(PREFIX + "maxContentLength",   Integer.toString(props.getMaxFrameContentLength()));
        p.put(PREFIX + "etb",                 String.format("0x%02X", props.getIntermediateEndOfFrame()));
        p.put(PREFIX + "etx",                 String.format("0x%02X", props.getEndOfFrameByte()));
        p.put(PREFIX + "checksumLength",     Integer.toString(props.getChecksumByteLength()));
        p.put(PREFIX + "frameTerminator",     props.getFrameTerminator());
        p.put(PREFIX + "eot",                 String.format("0x%02X", props.getEndOfTransmissionByte()));
        p.put(PREFIX + "ack",                 String.format("0x%02X", props.getPositiveAckByte()));
        p.put(PREFIX + "nak",                 String.format("0x%02X", props.getNegativeAckByte()));
        p.put(PREFIX + "maxTransferAttempts", Integer.toString(props.getMaxTransferAttempts()));
        p.put(PREFIX + "establishmentTimeout",Integer.toString(props.getEstablishmentTimeout()));
        p.put(PREFIX + "contentionTimeout",  Integer.toString(props.getContentionTimeout()));
        p.put(PREFIX + "frameTimeout",       Integer.toString(props.getFrameTimeout()));
        p.put(PREFIX + "responseTimeout",    Integer.toString(props.getResponseTimeout()));
        p.put(PREFIX + "checksumAlgorithm",   props.getChecksumAlgorithm());
        p.putBoolean(PREFIX + "validateFrameNumber", props.isValidateFrameNumber());
        p.putBoolean(PREFIX + "ignoreServerCancel",   props.isIgnoreServerSideCancel());
        p.putBoolean(PREFIX + "useChecksum",          props.isUseChecksum());
        p.putBoolean(PREFIX + "strictValidation",     props.isUseStrictValidation());
        p.putBoolean(PREFIX + "bidirectional",        props.isBidirectional());
        p.putBoolean(PREFIX + "serverMode",          props.isServerMode());
    }
}
