/*
 * BitDreamIT Mirth Lab Extensions
 * Copyright (c) 2026 Kimi AI (Moonshot AI) — MIT License
 */
package com.bitdreamit.mirth.labextensions.astme1381transmission;

import com.mirth.connect.plugins.transmissionmode.TransmissionModeProperties;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * ASTM E1381 transmission properties with all commercial features + extras.
 */
@XStreamAlias("astmE1381ModeProperties")
public class AstmE1381ModeProperties extends TransmissionModeProperties {
    private static final long serialVersionUID = 1L;

    // Handshake
    private boolean useEnqAck = true;
    private int enqTimeout = 1000;
    private int ackTimeout = 1000;

    // Framing
    private boolean useChecksum = true;
    private String checksumAlgorithm = "SUM_MOD_256"; // SUM_MOD_256, LRC, CRC8, CUSTOM
    private int maxFrameSize = 240;
    private int interFrameDelay = 100;

    // Retry
    private int maxRetries = 3;
    private boolean useExponentialBackoff = true;
    private int baseRetryDelay = 100;
    private int maxRetryDelay = 2000;

    // Timeouts
    private int frameTimeout = 5000;
    private int eotTimeout = 2000;
    private int sessionTimeout = 30000;

    // Dialect
    private String dialect = "LIS02-A"; // LIS02-A, LIS01-A, VENDOR_CUSTOM
    private boolean useIntermediateRecords = true;

    // Keepalive
    private boolean enableKeepalive = false;
    private int keepaliveInterval = 60000;

    // Protocol logging
    private boolean enableProtocolLogging = false;
    private int maxProtocolLogSize = 1000;

    public AstmE1381ModeProperties() {
        super("ASTM E1381");
    }

    // Getters & Setters
    public boolean isUseEnqAck() { return useEnqAck; }
    public void setUseEnqAck(boolean useEnqAck) { this.useEnqAck = useEnqAck; }
    public int getEnqTimeout() { return enqTimeout; }
    public void setEnqTimeout(int enqTimeout) { this.enqTimeout = enqTimeout; }
    public int getAckTimeout() { return ackTimeout; }
    public void setAckTimeout(int ackTimeout) { this.ackTimeout = ackTimeout; }
    public boolean isUseChecksum() { return useChecksum; }
    public void setUseChecksum(boolean useChecksum) { this.useChecksum = useChecksum; }
    public String getChecksumAlgorithm() { return checksumAlgorithm; }
    public void setChecksumAlgorithm(String checksumAlgorithm) { this.checksumAlgorithm = checksumAlgorithm; }
    public int getMaxFrameSize() { return maxFrameSize; }
    public void setMaxFrameSize(int maxFrameSize) { this.maxFrameSize = maxFrameSize; }
    public int getInterFrameDelay() { return interFrameDelay; }
    public void setInterFrameDelay(int interFrameDelay) { this.interFrameDelay = interFrameDelay; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public boolean isUseExponentialBackoff() { return useExponentialBackoff; }
    public void setUseExponentialBackoff(boolean useExponentialBackoff) { this.useExponentialBackoff = useExponentialBackoff; }
    public int getBaseRetryDelay() { return baseRetryDelay; }
    public void setBaseRetryDelay(int baseRetryDelay) { this.baseRetryDelay = baseRetryDelay; }
    public int getMaxRetryDelay() { return maxRetryDelay; }
    public void setMaxRetryDelay(int maxRetryDelay) { this.maxRetryDelay = maxRetryDelay; }
    public int getFrameTimeout() { return frameTimeout; }
    public void setFrameTimeout(int frameTimeout) { this.frameTimeout = frameTimeout; }
    public int getEotTimeout() { return eotTimeout; }
    public void setEotTimeout(int eotTimeout) { this.eotTimeout = eotTimeout; }
    public int getSessionTimeout() { return sessionTimeout; }
    public void setSessionTimeout(int sessionTimeout) { this.sessionTimeout = sessionTimeout; }
    public String getDialect() { return dialect; }
    public void setDialect(String dialect) { this.dialect = dialect; }
    public boolean isUseIntermediateRecords() { return useIntermediateRecords; }
    public void setUseIntermediateRecords(boolean useIntermediateRecords) { this.useIntermediateRecords = useIntermediateRecords; }
    public boolean isEnableKeepalive() { return enableKeepalive; }
    public void setEnableKeepalive(boolean enableKeepalive) { this.enableKeepalive = enableKeepalive; }
    public int getKeepaliveInterval() { return keepaliveInterval; }
    public void setKeepaliveInterval(int keepaliveInterval) { this.keepaliveInterval = keepaliveInterval; }
    public boolean isEnableProtocolLogging() { return enableProtocolLogging; }
    public void setEnableProtocolLogging(boolean enableProtocolLogging) { this.enableProtocolLogging = enableProtocolLogging; }
    public int getMaxProtocolLogSize() { return maxProtocolLogSize; }
    public void setMaxProtocolLogSize(int maxProtocolLogSize) { this.maxProtocolLogSize = maxProtocolLogSize; }

    @Override
    public String getPluginPointName() { return "ASTM E1381"; }
    @Override
    public TransmissionModeProperties create() { return new AstmE1381ModeProperties(); }
}