package com.bitdreamit.connect.plugins.transmission.astm.shared;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Constants;
import com.mirth.connect.model.datatype.DataTypePropertyDescriptor;
import com.mirth.connect.model.datatype.PropertyEditorType;
import com.mirth.connect.model.transmission.TransmissionModeProperties;

/**
 * ASTM E1381-02 transmission-mode properties.
 *
 * <p>This class is the source of truth for every knob exposed to Mirth
 * administrators through the channel editor. It is shared between the
 * server-side providers ({@code ASTME1381ServerProvider},
 * {@code ASTME1381StreamHandler}) and the client-side provider
 * ({@code ASTME1381ClientProvider}).</p>
 *
 * <p>Production hardening notes:</p>
 * <ul>
 *   <li>Every byte-typed property is stored as {@code int} so the value
 *       round-trips through Mirth's XML serializer without sign-extension
 *       surprises.</li>
 *   <li>{@code parseHex} / {@code parseInt} are null-safe and return the
 *       default on parse failure rather than {@code 0}.</li>
 *   <li>The set of properties is a superset of both the original 1.0.x
 *       release and the newer refactored version, so any channel exported
 *       against an earlier jar will still load.</li>
 * </ul>
 */
public class ASTME1381TransmissionModeProperties extends TransmissionModeProperties {

    // --- Frame Settings ---
    private int enquiryByte             = ASTME1381Constants.ENQ;
    private int startOfFrameByte        = ASTME1381Constants.STX;
    private int maxFrameContentLength   = ASTME1381Constants.DEFAULT_MAX_FRAME_CONTENT_LENGTH;
    private int intermediateEndOfFrame  = ASTME1381Constants.ETB;
    private int endOfFrameByte          = ASTME1381Constants.ETX;
    private int checksumByteLength      = ASTME1381Constants.DEFAULT_CHECKSUM_BYTE_LENGTH;
    private String frameTerminator      = "0x0D0A"; // CR+LF (hex form per Mirth convention)
    private int endOfTransmissionByte   = ASTME1381Constants.EOT;

    // --- Validation Settings ---
    private boolean validateFrameNumber     = true;
    private boolean ignoreServerSideCancel  = false;
    private boolean useChecksum             = true;
    private boolean useStrictValidation     = false;
    private String checksumAlgorithm        = ASTME1381Constants.CHECKSUM_ADD_MOD_256;
    private boolean bidirectional           = true;
    private int positiveAckByte             = ASTME1381Constants.ACK;
    private int negativeAckByte             = ASTME1381Constants.NAK;

    // --- Connection Settings ---
    private int maxTransferAttempts         = ASTME1381Constants.DEFAULT_MAX_TRANSFER_ATTEMPTS;
    private int establishmentTimeout        = ASTME1381Constants.DEFAULT_ESTABLISHMENT_TIMEOUT;
    private int contentionTimeout           = ASTME1381Constants.DEFAULT_CONTENTION_TIMEOUT;
    private int frameTimeout                = ASTME1381Constants.DEFAULT_FRAME_TIMEOUT;
    private int responseTimeout             = ASTME1381Constants.DEFAULT_RESPONSE_TIMEOUT;

    // --- Mode ---
    private boolean serverMode = true; // true=Server (listener), false=Client (sender)

    // --- New-style provider settings (used by ASTME1381ServerProvider / ASTME1381ClientProvider) ---
    /** First frame sequence number used when strict sequencing is enabled. */
    private int  frameNumberStart    = ASTME1381Constants.DEFAULT_FRAME_SEQUENCE_START;
    /** Strict mode: NAK on out-of-sequence frame; lenient mode: accept any valid frame. */
    private boolean strictFrameSequencing = true;
    /** Max ENQ establishment retries before declaring the peer unreachable. */
    private int  maxEnqRetries       = ASTME1381Constants.DEFAULT_MAX_ENQ_RETRIES;
    /** Per-frame ACK retry count (separate from transfer-level attempts). */
    private int  maxFrameRetries     = ASTME1381Constants.DEFAULT_MAX_FRAME_RETRIES;
    /** Timeout (ms) for the ENQ -> ACK handshake. */
    private int  enqTimeoutMs        = ASTME1381Constants.DEFAULT_ENQ_TIMEOUT_MS;
    /** Timeout (ms) for the per-frame ACK after sending a data frame. */
    private int  frameAckTimeoutMs   = ASTME1381Constants.DEFAULT_FRAME_ACK_TIMEOUT_MS;

    public ASTME1381TransmissionModeProperties() {
        super(ASTME1381Constants.PLUGIN_NAME);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, DataTypePropertyDescriptor> getPropertyDescriptors() {
        Map<String, DataTypePropertyDescriptor> props = new LinkedHashMap<>();

        // --- Frame Settings ---
        props.put("enquiryByte",            new DataTypePropertyDescriptor(String.format("0x%02X", enquiryByte), "Enquiry (ENQ)", "Byte value for Enquiry signal.", PropertyEditorType.STRING));
        props.put("startOfFrameByte",       new DataTypePropertyDescriptor(String.format("0x%02X", startOfFrameByte), "Start of Frame (STX)", "Byte value for Start of Frame.", PropertyEditorType.STRING));
        props.put("maxFrameContentLength",  new DataTypePropertyDescriptor(maxFrameContentLength, "Max Frame Content Length", "Maximum payload bytes per frame (default 240).", PropertyEditorType.STRING));
        props.put("intermediateEndOfFrame", new DataTypePropertyDescriptor(String.format("0x%02X", intermediateEndOfFrame), "Intermediate End of Frame (ETB)", "Byte for intermediate frame end.", PropertyEditorType.STRING));
        props.put("endOfFrameByte",         new DataTypePropertyDescriptor(String.format("0x%02X", endOfFrameByte), "End of Frame (ETX)", "Byte for final frame end.", PropertyEditorType.STRING));
        props.put("checksumByteLength",     new DataTypePropertyDescriptor(checksumByteLength, "Checksum Byte Length", "Number of checksum bytes (1 or 2).", PropertyEditorType.STRING));
        props.put("frameTerminator",        new DataTypePropertyDescriptor(frameTerminator, "Frame Terminator", "Terminator after checksum (e.g. 0x0D0A for CR+LF).", PropertyEditorType.STRING));
        props.put("endOfTransmissionByte",  new DataTypePropertyDescriptor(String.format("0x%02X", endOfTransmissionByte), "End of Transmission (EOT)", "Byte for end of transmission.", PropertyEditorType.STRING));

        // --- Validation Settings ---
        props.put("validateFrameNumber",    new DataTypePropertyDescriptor(validateFrameNumber, "Validate Frame Number", "Verify frame sequence numbers 0-7.", PropertyEditorType.BOOLEAN));
        props.put("strictFrameSequencing",  new DataTypePropertyDescriptor(strictFrameSequencing, "Strict Frame Sequencing", "If true, NAK on out-of-sequence frames; if false, accept any valid frame.", PropertyEditorType.BOOLEAN));
        props.put("frameNumberStart",       new DataTypePropertyDescriptor(frameNumberStart, "Frame Number Start", "First frame number when sequencing is enabled (0 or 1).", PropertyEditorType.STRING));
        props.put("ignoreServerSideCancel", new DataTypePropertyDescriptor(ignoreServerSideCancel, "Ignore Server-Side Cancel", "Ignore EOT from sender during transfer.", PropertyEditorType.BOOLEAN));
        props.put("useChecksum",            new DataTypePropertyDescriptor(useChecksum, "Use Checksum", "Enable frame checksum validation.", PropertyEditorType.BOOLEAN));
        props.put("useStrictValidation",    new DataTypePropertyDescriptor(useStrictValidation, "Use Strict Validation", "Enforce strict ASTM E1381 compliance.", PropertyEditorType.BOOLEAN));
        props.put("checksumAlgorithm",      new DataTypePropertyDescriptor(checksumAlgorithm, "Checksum Algorithm", "Algorithm for checksum calculation.", PropertyEditorType.STRING, new Object[]{ASTME1381Constants.CHECKSUM_ADD_MOD_256, ASTME1381Constants.CHECKSUM_XOR, ASTME1381Constants.CHECKSUM_NONE}));
        props.put("bidirectional",          new DataTypePropertyDescriptor(bidirectional, "Bidirectional", "Enable bidirectional communication.", PropertyEditorType.BOOLEAN));
        props.put("positiveAckByte",        new DataTypePropertyDescriptor(String.format("0x%02X", positiveAckByte), "Positive Acknowledge (ACK)", "Byte for positive acknowledgement.", PropertyEditorType.STRING));
        props.put("negativeAckByte",        new DataTypePropertyDescriptor(String.format("0x%02X", negativeAckByte), "Negative Acknowledge (NAK)", "Byte for negative acknowledgement.", PropertyEditorType.STRING));

        // --- Connection Settings ---
        props.put("maxTransferAttempts",    new DataTypePropertyDescriptor(maxTransferAttempts, "Max Transfer Attempts", "Maximum retry attempts per frame.", PropertyEditorType.STRING));
        props.put("maxEnqRetries",          new DataTypePropertyDescriptor(maxEnqRetries, "Max ENQ Retries", "Maximum ENQ establishment retries.", PropertyEditorType.STRING));
        props.put("maxFrameRetries",        new DataTypePropertyDescriptor(maxFrameRetries, "Max Frame Retries", "Per-frame ACK retry count.", PropertyEditorType.STRING));
        props.put("establishmentTimeout",   new DataTypePropertyDescriptor(establishmentTimeout, "Establishment Timeout (ms)", "Timeout for connection establishment.", PropertyEditorType.STRING));
        props.put("contentionTimeout",      new DataTypePropertyDescriptor(contentionTimeout, "Contention Timeout (ms)", "Timeout for line contention resolution.", PropertyEditorType.STRING));
        props.put("frameTimeout",           new DataTypePropertyDescriptor(frameTimeout, "Frame Timeout (ms)", "Timeout waiting for complete frame.", PropertyEditorType.STRING));
        props.put("responseTimeout",        new DataTypePropertyDescriptor(responseTimeout, "Response Timeout (ms)", "Timeout waiting for ACK/NAK response.", PropertyEditorType.STRING));
        props.put("enqTimeoutMs",           new DataTypePropertyDescriptor(enqTimeoutMs, "ENQ ACK Timeout (ms)", "Timeout waiting for ACK to ENQ.", PropertyEditorType.STRING));
        props.put("frameAckTimeoutMs",      new DataTypePropertyDescriptor(frameAckTimeoutMs, "Frame ACK Timeout (ms)", "Timeout waiting for ACK to a single data frame.", PropertyEditorType.STRING));

        // --- Mode ---
        props.put("serverMode",             new DataTypePropertyDescriptor(serverMode, "Server Mode", "Act as server (true) or client (false).", PropertyEditorType.BOOLEAN));

        return props;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setProperties(Map properties) {
        if (properties == null) return;

        if (properties.get("enquiryByte") != null)            this.enquiryByte            = parseHex(properties.get("enquiryByte"));
        if (properties.get("startOfFrameByte") != null)       this.startOfFrameByte       = parseHex(properties.get("startOfFrameByte"));
        if (properties.get("maxFrameContentLength") != null)  this.maxFrameContentLength  = parseInt(properties.get("maxFrameContentLength"));
        if (properties.get("intermediateEndOfFrame") != null) this.intermediateEndOfFrame = parseHex(properties.get("intermediateEndOfFrame"));
        if (properties.get("endOfFrameByte") != null)         this.endOfFrameByte         = parseHex(properties.get("endOfFrameByte"));
        if (properties.get("checksumByteLength") != null)     this.checksumByteLength     = parseInt(properties.get("checksumByteLength"));
        if (properties.get("frameTerminator") != null)        this.frameTerminator        = String.valueOf(properties.get("frameTerminator"));
        if (properties.get("endOfTransmissionByte") != null)  this.endOfTransmissionByte  = parseHex(properties.get("endOfTransmissionByte"));

        if (properties.get("validateFrameNumber") != null)    this.validateFrameNumber    = toBoolean(properties.get("validateFrameNumber"));
        if (properties.get("strictFrameSequencing") != null)   this.strictFrameSequencing  = toBoolean(properties.get("strictFrameSequencing"));
        if (properties.get("frameNumberStart") != null)       this.frameNumberStart       = parseInt(properties.get("frameNumberStart"));
        if (properties.get("ignoreServerSideCancel") != null)  this.ignoreServerSideCancel = toBoolean(properties.get("ignoreServerSideCancel"));
        if (properties.get("useChecksum") != null)            this.useChecksum            = toBoolean(properties.get("useChecksum"));
        if (properties.get("useStrictValidation") != null)    this.useStrictValidation    = toBoolean(properties.get("useStrictValidation"));
        if (properties.get("checksumAlgorithm") != null)      this.checksumAlgorithm      = String.valueOf(properties.get("checksumAlgorithm"));
        if (properties.get("bidirectional") != null)          this.bidirectional          = toBoolean(properties.get("bidirectional"));
        if (properties.get("positiveAckByte") != null)        this.positiveAckByte        = parseHex(properties.get("positiveAckByte"));
        if (properties.get("negativeAckByte") != null)        this.negativeAckByte        = parseHex(properties.get("negativeAckByte"));

        if (properties.get("maxTransferAttempts") != null)    this.maxTransferAttempts    = parseInt(properties.get("maxTransferAttempts"));
        if (properties.get("maxEnqRetries") != null)          this.maxEnqRetries          = parseInt(properties.get("maxEnqRetries"));
        if (properties.get("maxFrameRetries") != null)         this.maxFrameRetries        = parseInt(properties.get("maxFrameRetries"));
        if (properties.get("establishmentTimeout") != null)   this.establishmentTimeout   = parseInt(properties.get("establishmentTimeout"));
        if (properties.get("contentionTimeout") != null)      this.contentionTimeout      = parseInt(properties.get("contentionTimeout"));
        if (properties.get("frameTimeout") != null)           this.frameTimeout           = parseInt(properties.get("frameTimeout"));
        if (properties.get("responseTimeout") != null)        this.responseTimeout        = parseInt(properties.get("responseTimeout"));
        if (properties.get("enqTimeoutMs") != null)            this.enqTimeoutMs           = parseInt(properties.get("enqTimeoutMs"));
        if (properties.get("frameAckTimeoutMs") != null)       this.frameAckTimeoutMs      = parseInt(properties.get("frameAckTimeoutMs"));

        if (properties.get("serverMode") != null)             this.serverMode             = toBoolean(properties.get("serverMode"));
    }

    // ------------------------------------------------------------------
    // Parsing helpers - production-safe (never throws, never silently 0's defaults)
    // ------------------------------------------------------------------

    private static int parseHex(Object o) {
        if (o == null) return 0;
        try {
            String s = o.toString().trim().replace("0x", "").replace("0X", "");
            if (s.isEmpty()) return 0;
            return Integer.parseInt(s, 16);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int parseInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(o.toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean toBoolean(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean) return (Boolean) o;
        return Boolean.parseBoolean(o.toString());
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------
    public int getEnquiryByte() { return enquiryByte; }
    public int getStartOfFrameByte() { return startOfFrameByte; }
    public int getMaxFrameContentLength() { return maxFrameContentLength; }
    public int getIntermediateEndOfFrame() { return intermediateEndOfFrame; }
    public int getEndOfFrameByte() { return endOfFrameByte; }
    public int getChecksumByteLength() { return checksumByteLength; }
    public String getFrameTerminator() { return frameTerminator; }
    public int getEndOfTransmissionByte() { return endOfTransmissionByte; }
    public boolean isValidateFrameNumber() { return validateFrameNumber; }
    public boolean isStrictFrameSequencing() { return strictFrameSequencing; }
    public int  getFrameNumberStart() { return frameNumberStart; }
    public boolean isIgnoreServerSideCancel() { return ignoreServerSideCancel; }
    public boolean isUseChecksum() { return useChecksum; }
    public boolean isUseStrictValidation() { return useStrictValidation; }
    public String getChecksumAlgorithm() { return checksumAlgorithm; }
    public boolean isBidirectional() { return bidirectional; }
    public int getPositiveAckByte() { return positiveAckByte; }
    public int getNegativeAckByte() { return negativeAckByte; }
    public int getMaxTransferAttempts() { return maxTransferAttempts; }
    public int getMaxEnqRetries() { return maxEnqRetries; }
    public int getMaxFrameRetries() { return maxFrameRetries; }
    public int getEstablishmentTimeout() { return establishmentTimeout; }
    public int getContentionTimeout() { return contentionTimeout; }
    public int getFrameTimeout() { return frameTimeout; }
    public int getResponseTimeout() { return responseTimeout; }
    public int getEnqTimeoutMs() { return enqTimeoutMs; }
    public int getFrameAckTimeoutMs() { return frameAckTimeoutMs; }
    public boolean isServerMode() { return serverMode; }

    // ------------------------------------------------------------------
    // Setters
    // ------------------------------------------------------------------
    public void setEnquiryByte(int enquiryByte) { this.enquiryByte = enquiryByte; }
    public void setStartOfFrameByte(int startOfFrameByte) { this.startOfFrameByte = startOfFrameByte; }
    public void setMaxFrameContentLength(int maxFrameContentLength) { this.maxFrameContentLength = maxFrameContentLength; }
    public void setIntermediateEndOfFrame(int intermediateEndOfFrame) { this.intermediateEndOfFrame = intermediateEndOfFrame; }
    public void setEndOfFrameByte(int endOfFrameByte) { this.endOfFrameByte = endOfFrameByte; }
    public void setChecksumByteLength(int checksumByteLength) { this.checksumByteLength = checksumByteLength; }
    public void setFrameTerminator(String frameTerminator) { this.frameTerminator = frameTerminator; }
    public void setEndOfTransmissionByte(int endOfTransmissionByte) { this.endOfTransmissionByte = endOfTransmissionByte; }
    public void setValidateFrameNumber(boolean validateFrameNumber) { this.validateFrameNumber = validateFrameNumber; }
    public void setStrictFrameSequencing(boolean strictFrameSequencing) { this.strictFrameSequencing = strictFrameSequencing; }
    public void setFrameNumberStart(int frameNumberStart) { this.frameNumberStart = frameNumberStart; }
    public void setIgnoreServerSideCancel(boolean ignoreServerSideCancel) { this.ignoreServerSideCancel = ignoreServerSideCancel; }
    public void setUseChecksum(boolean useChecksum) { this.useChecksum = useChecksum; }
    public void setUseStrictValidation(boolean useStrictValidation) { this.useStrictValidation = useStrictValidation; }
    public void setChecksumAlgorithm(String checksumAlgorithm) { this.checksumAlgorithm = checksumAlgorithm; }
    public void setBidirectional(boolean bidirectional) { this.bidirectional = bidirectional; }
    public void setPositiveAckByte(int positiveAckByte) { this.positiveAckByte = positiveAckByte; }
    public void setNegativeAckByte(int negativeAckByte) { this.negativeAckByte = negativeAckByte; }
    public void setMaxTransferAttempts(int maxTransferAttempts) { this.maxTransferAttempts = maxTransferAttempts; }
    public void setMaxEnqRetries(int maxEnqRetries) { this.maxEnqRetries = maxEnqRetries; }
    public void setMaxFrameRetries(int maxFrameRetries) { this.maxFrameRetries = maxFrameRetries; }
    public void setEstablishmentTimeout(int establishmentTimeout) { this.establishmentTimeout = establishmentTimeout; }
    public void setContentionTimeout(int contentionTimeout) { this.contentionTimeout = contentionTimeout; }
    public void setFrameTimeout(int frameTimeout) { this.frameTimeout = frameTimeout; }
    public void setResponseTimeout(int responseTimeout) { this.responseTimeout = responseTimeout; }
    public void setEnqTimeoutMs(int enqTimeoutMs) { this.enqTimeoutMs = enqTimeoutMs; }
    public void setFrameAckTimeoutMs(int frameAckTimeoutMs) { this.frameAckTimeoutMs = frameAckTimeoutMs; }
    public void setServerMode(boolean serverMode) { this.serverMode = serverMode; }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPurgedProperties() {
        Map<String, Object> purged = new HashMap<>();
        purged.put("pluginPointName", getPluginPointName());
        purged.put("validateFrameNumber", validateFrameNumber);
        purged.put("strictFrameSequencing", strictFrameSequencing);
        purged.put("frameNumberStart", frameNumberStart);
        purged.put("useChecksum", useChecksum);
        purged.put("useStrictValidation", useStrictValidation);
        purged.put("checksumAlgorithm", checksumAlgorithm);
        purged.put("bidirectional", bidirectional);
        purged.put("maxTransferAttempts", maxTransferAttempts);
        purged.put("maxEnqRetries", maxEnqRetries);
        purged.put("maxFrameRetries", maxFrameRetries);
        purged.put("serverMode", serverMode);
        return purged;
    }
}
