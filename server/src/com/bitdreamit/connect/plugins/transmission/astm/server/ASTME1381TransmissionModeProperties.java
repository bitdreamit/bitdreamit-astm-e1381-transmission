package com.bitdreamit.mirth.astm.e1381.server;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.bitdreamit.mirth.astm.e1381.shared.ASTME1381Constants;
import com.mirth.connect.donkey.util.DonkeyElement;
import com.mirth.connect.model.datatype.DataTypePropertyDescriptor;
import com.mirth.connect.model.datatype.PropertyEditorType;
import com.mirth.connect.model.transmission.TransmissionModeProperties;

/**
 * ASTM E1381-95 Transmission Mode Properties
 * Production-grade with full frame control, validation, and connection settings
 */
public class ASTME1381TransmissionModeProperties extends TransmissionModeProperties {

    // --- Frame Settings ---
    private int enquiryByte             = ASTME1381Constants.ENQ;
    private int startOfFrameByte        = ASTME1381Constants.STX;
    private int maxFrameContentLength   = ASTME1381Constants.DEFAULT_MAX_FRAME_CONTENT_LENGTH;
    private int intermediateEndOfFrame  = ASTME1381Constants.ETB;
    private int endOfFrameByte          = ASTME1381Constants.ETX;
    private int checksumByteLength      = ASTME1381Constants.DEFAULT_CHECKSUM_BYTE_LENGTH;
    private String frameTerminator      = "0x000A"; // CR+LF
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
    private boolean serverMode = true; // true=Server, false=Client

    public ASTME1381TransmissionModeProperties() {
        super(ASTME1381Constants.PLUGIN_NAME);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, DataTypePropertyDescriptor> getPropertyDescriptors() {
        Map<String, DataTypePropertyDescriptor> props = new LinkedHashMap<>();

        // Frame Settings
        props.put("enquiryByte",            new DataTypePropertyDescriptor(String.format("0x%02X", enquiryByte), "Enquiry (ENQ)", "Byte value for Enquiry signal.", PropertyEditorType.STRING));
        props.put("startOfFrameByte",       new DataTypePropertyDescriptor(String.format("0x%02X", startOfFrameByte), "Start of Frame (STX)", "Byte value for Start of Frame.", PropertyEditorType.STRING));
        props.put("maxFrameContentLength",  new DataTypePropertyDescriptor(maxFrameContentLength, "Max Frame Content Length", "Maximum payload bytes per frame (default 240).", PropertyEditorType.STRING));
        props.put("intermediateEndOfFrame", new DataTypePropertyDescriptor(String.format("0x%02X", intermediateEndOfFrame), "Intermediate End of Frame (ETB)", "Byte for intermediate frame end.", PropertyEditorType.STRING));
        props.put("endOfFrameByte",         new DataTypePropertyDescriptor(String.format("0x%02X", endOfFrameByte), "End of Frame (ETX)", "Byte for final frame end.", PropertyEditorType.STRING));
        props.put("checksumByteLength",     new DataTypePropertyDescriptor(checksumByteLength, "Checksum Byte Length", "Number of checksum bytes (1 or 2).", PropertyEditorType.STRING));
        props.put("frameTerminator",        new DataTypePropertyDescriptor(frameTerminator, "Frame Terminator", "Terminator after checksum (e.g. 0x000A for CR+LF).", PropertyEditorType.STRING));
        props.put("endOfTransmissionByte",  new DataTypePropertyDescriptor(String.format("0x%02X", endOfTransmissionByte), "End of Transmission (EOT)", "Byte for end of transmission.", PropertyEditorType.STRING));

        // Validation Settings
        props.put("validateFrameNumber",    new DataTypePropertyDescriptor(validateFrameNumber, "Validate Frame Number", "Verify frame sequence numbers 0-7.", PropertyEditorType.BOOLEAN));
        props.put("ignoreServerSideCancel", new DataTypePropertyDescriptor(ignoreServerSideCancel, "Ignore Server-Side Cancel", "Ignore EOT from sender during transfer.", PropertyEditorType.BOOLEAN));
        props.put("useChecksum",            new DataTypePropertyDescriptor(useChecksum, "Use Checksum", "Enable frame checksum validation.", PropertyEditorType.BOOLEAN));
        props.put("useStrictValidation",    new DataTypePropertyDescriptor(useStrictValidation, "Use Strict Validation", "Enforce strict ASTM E1381 compliance.", PropertyEditorType.BOOLEAN));
        props.put("checksumAlgorithm",      new DataTypePropertyDescriptor(checksumAlgorithm, "Checksum Algorithm", "Algorithm for checksum calculation.", PropertyEditorType.STRING, new Object[]{ASTME1381Constants.CHECKSUM_ADD_MOD_256, ASTME1381Constants.CHECKSUM_XOR, ASTME1381Constants.CHECKSUM_NONE}));
        props.put("bidirectional",          new DataTypePropertyDescriptor(bidirectional, "Bidirectional", "Enable bidirectional communication.", PropertyEditorType.BOOLEAN));
        props.put("positiveAckByte",        new DataTypePropertyDescriptor(String.format("0x%02X", positiveAckByte), "Positive Acknowledge (ACK)", "Byte for positive acknowledgement.", PropertyEditorType.STRING));
        props.put("negativeAckByte",        new DataTypePropertyDescriptor(String.format("0x%02X", negativeAckByte), "Negative Acknowledge (NAK)", "Byte for negative acknowledgement.", PropertyEditorType.STRING));

        // Connection Settings
        props.put("maxTransferAttempts",    new DataTypePropertyDescriptor(maxTransferAttempts, "Max Transfer Attempts", "Maximum retry attempts per frame.", PropertyEditorType.STRING));
        props.put("establishmentTimeout",   new DataTypePropertyDescriptor(establishmentTimeout, "Establishment Timeout (ms)", "Timeout for connection establishment.", PropertyEditorType.STRING));
        props.put("contentionTimeout",      new DataTypePropertyDescriptor(contentionTimeout, "Contention Timeout (ms)", "Timeout for line contention resolution.", PropertyEditorType.STRING));
        props.put("frameTimeout",           new DataTypePropertyDescriptor(frameTimeout, "Frame Timeout (ms)", "Timeout waiting for complete frame.", PropertyEditorType.STRING));
        props.put("responseTimeout",        new DataTypePropertyDescriptor(responseTimeout, "Response Timeout (ms)", "Timeout waiting for ACK/NAK response.", PropertyEditorType.STRING));

        // Mode
        props.put("serverMode",             new DataTypePropertyDescriptor(serverMode, "Server Mode", "Act as server (true) or client (false).", PropertyEditorType.BOOLEAN));

        return props;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setProperties(Map properties) {
        if (properties == null) return;

        if (properties.get("enquiryByte") != null)            this.enquiryByte           = parseHex((String) properties.get("enquiryByte"));
        if (properties.get("startOfFrameByte") != null)       this.startOfFrameByte      = parseHex((String) properties.get("startOfFrameByte"));
        if (properties.get("maxFrameContentLength") != null)  this.maxFrameContentLength = parseInt(properties.get("maxFrameContentLength"));
        if (properties.get("intermediateEndOfFrame") != null) this.intermediateEndOfFrame= parseHex((String) properties.get("intermediateEndOfFrame"));
        if (properties.get("endOfFrameByte") != null)         this.endOfFrameByte        = parseHex((String) properties.get("endOfFrameByte"));
        if (properties.get("checksumByteLength") != null)     this.checksumByteLength    = parseInt(properties.get("checksumByteLength"));
        if (properties.get("frameTerminator") != null)        this.frameTerminator       = (String) properties.get("frameTerminator");
        if (properties.get("endOfTransmissionByte") != null)  this.endOfTransmissionByte = parseHex((String) properties.get("endOfTransmissionByte"));

        if (properties.get("validateFrameNumber") != null)    this.validateFrameNumber    = (Boolean) properties.get("validateFrameNumber");
        if (properties.get("ignoreServerSideCancel") != null)   this.ignoreServerSideCancel = (Boolean) properties.get("ignoreServerSideCancel");
        if (properties.get("useChecksum") != null)            this.useChecksum            = (Boolean) properties.get("useChecksum");
        if (properties.get("useStrictValidation") != null)    this.useStrictValidation    = (Boolean) properties.get("useStrictValidation");
        if (properties.get("checksumAlgorithm") != null)        this.checksumAlgorithm      = (String) properties.get("checksumAlgorithm");
        if (properties.get("bidirectional") != null)          this.bidirectional          = (Boolean) properties.get("bidirectional");
        if (properties.get("positiveAckByte") != null)        this.positiveAckByte        = parseHex((String) properties.get("positiveAckByte"));
        if (properties.get("negativeAckByte") != null)        this.negativeAckByte        = parseHex((String) properties.get("negativeAckByte"));

        if (properties.get("maxTransferAttempts") != null)    this.maxTransferAttempts    = parseInt(properties.get("maxTransferAttempts"));
        if (properties.get("establishmentTimeout") != null)   this.establishmentTimeout   = parseInt(properties.get("establishmentTimeout"));
        if (properties.get("contentionTimeout") != null)        this.contentionTimeout      = parseInt(properties.get("contentionTimeout"));
        if (properties.get("frameTimeout") != null)             this.frameTimeout           = parseInt(properties.get("frameTimeout"));
        if (properties.get("responseTimeout") != null)          this.responseTimeout        = parseInt(properties.get("responseTimeout"));

        if (properties.get("serverMode") != null)             this.serverMode             = (Boolean) properties.get("serverMode");
    }

    private int parseHex(String s) {
        try {
            s = s.trim().replace("0x", "").replace("0X", "");
            return Integer.parseInt(s, 16);
        } catch (Exception e) {
            return 0;
        }
    }

    private int parseInt(Object o) {
        try {
            if (o instanceof Number) return ((Number) o).intValue();
            return Integer.parseInt(o.toString().trim());
        } catch (Exception e) {
            return 0;
        }
    }

    // Getters
    public int getEnquiryByte() { return enquiryByte; }
    public int getStartOfFrameByte() { return startOfFrameByte; }
    public int getMaxFrameContentLength() { return maxFrameContentLength; }
    public int getIntermediateEndOfFrame() { return intermediateEndOfFrame; }
    public int getEndOfFrameByte() { return endOfFrameByte; }
    public int getChecksumByteLength() { return checksumByteLength; }
    public String getFrameTerminator() { return frameTerminator; }
    public int getEndOfTransmissionByte() { return endOfTransmissionByte; }
    public boolean isValidateFrameNumber() { return validateFrameNumber; }
    public boolean isIgnoreServerSideCancel() { return ignoreServerSideCancel; }
    public boolean isUseChecksum() { return useChecksum; }
    public boolean isUseStrictValidation() { return useStrictValidation; }
    public String getChecksumAlgorithm() { return checksumAlgorithm; }
    public boolean isBidirectional() { return bidirectional; }
    public int getPositiveAckByte() { return positiveAckByte; }
    public int getNegativeAckByte() { return negativeAckByte; }
    public int getMaxTransferAttempts() { return maxTransferAttempts; }
    public int getEstablishmentTimeout() { return establishmentTimeout; }
    public int getContentionTimeout() { return contentionTimeout; }
    public int getFrameTimeout() { return frameTimeout; }
    public int getResponseTimeout() { return responseTimeout; }
    public boolean isServerMode() { return serverMode; }

    // Setters
    public void setEnquiryByte(int enquiryByte) { this.enquiryByte = enquiryByte; }
    public void setStartOfFrameByte(int startOfFrameByte) { this.startOfFrameByte = startOfFrameByte; }
    public void setMaxFrameContentLength(int maxFrameContentLength) { this.maxFrameContentLength = maxFrameContentLength; }
    public void setIntermediateEndOfFrame(int intermediateEndOfFrame) { this.intermediateEndOfFrame = intermediateEndOfFrame; }
    public void setEndOfFrameByte(int endOfFrameByte) { this.endOfFrameByte = endOfFrameByte; }
    public void setChecksumByteLength(int checksumByteLength) { this.checksumByteLength = checksumByteLength; }
    public void setFrameTerminator(String frameTerminator) { this.frameTerminator = frameTerminator; }
    public void setEndOfTransmissionByte(int endOfTransmissionByte) { this.endOfTransmissionByte = endOfTransmissionByte; }
    public void setValidateFrameNumber(boolean validateFrameNumber) { this.validateFrameNumber = validateFrameNumber; }
    public void setIgnoreServerSideCancel(boolean ignoreServerSideCancel) { this.ignoreServerSideCancel = ignoreServerSideCancel; }
    public void setUseChecksum(boolean useChecksum) { this.useChecksum = useChecksum; }
    public void setUseStrictValidation(boolean useStrictValidation) { this.useStrictValidation = useStrictValidation; }
    public void setChecksumAlgorithm(String checksumAlgorithm) { this.checksumAlgorithm = checksumAlgorithm; }
    public void setBidirectional(boolean bidirectional) { this.bidirectional = bidirectional; }
    public void setPositiveAckByte(int positiveAckByte) { this.positiveAckByte = positiveAckByte; }
    public void setNegativeAckByte(int negativeAckByte) { this.negativeAckByte = negativeAckByte; }
    public void setMaxTransferAttempts(int maxTransferAttempts) { this.maxTransferAttempts = maxTransferAttempts; }
    public void setEstablishmentTimeout(int establishmentTimeout) { this.establishmentTimeout = establishmentTimeout; }
    public void setContentionTimeout(int contentionTimeout) { this.contentionTimeout = contentionTimeout; }
    public void setFrameTimeout(int frameTimeout) { this.frameTimeout = frameTimeout; }
    public void setResponseTimeout(int responseTimeout) { this.responseTimeout = responseTimeout; }
    public void setServerMode(boolean serverMode) { this.serverMode = serverMode; }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPurgedProperties() {
        Map<String, Object> purged = new HashMap<>();
        purged.put("pluginPointName", getPluginPointName());
        purged.put("validateFrameNumber", validateFrameNumber);
        purged.put("useChecksum", useChecksum);
        purged.put("useStrictValidation", useStrictValidation);
        purged.put("checksumAlgorithm", checksumAlgorithm);
        purged.put("bidirectional", bidirectional);
        purged.put("maxTransferAttempts", maxTransferAttempts);
        purged.put("serverMode", serverMode);
        return purged;
    }
}
