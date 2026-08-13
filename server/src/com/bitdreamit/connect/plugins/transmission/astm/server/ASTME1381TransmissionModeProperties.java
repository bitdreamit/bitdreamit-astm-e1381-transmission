package com.bitdreamit.connect.plugins.transmission.astm.server;

import com.mirth.connect.model.transmission.TransmissionModeProperties;
import com.mirth.connect.donkey.util.DonkeyElement;

public class ASTME1381TransmissionModeProperties extends TransmissionModeProperties {
    private int enqTimeoutMs = 15000;
    private int frameAckTimeoutMs = 15000;
    private int maxEnqRetries = 6;
    private int maxFrameRetries = 6;
    private boolean strictFrameSequencing = true;
    private int frameNumberStart = 1;

    public int getEnqTimeoutMs() { return enqTimeoutMs; }
    public void setEnqTimeoutMs(int v) { this.enqTimeoutMs = v; }
    public int getFrameAckTimeoutMs() { return frameAckTimeoutMs; }
    public void setFrameAckTimeoutMs(int v) { this.frameAckTimeoutMs = v; }
    public int getMaxEnqRetries() { return maxEnqRetries; }
    public void setMaxEnqRetries(int v) { this.maxEnqRetries = v; }
    public int getMaxFrameRetries() { return maxFrameRetries; }
    public void setMaxFrameRetries(int v) { this.maxFrameRetries = v; }
    public boolean isStrictFrameSequencing() { return strictFrameSequencing; }
    public void setStrictFrameSequencing(boolean v) { this.strictFrameSequencing = v; }
    public int getFrameNumberStart() { return frameNumberStart; }
    public void setFrameNumberStart(int v) { this.frameNumberStart = v; }

    @Override
    public DonkeyElement toDonkeyElement() {
        DonkeyElement element = new DonkeyElement("properties");
        element.addChildElement("enqTimeoutMs", String.valueOf(enqTimeoutMs));
        element.addChildElement("frameAckTimeoutMs", String.valueOf(frameAckTimeoutMs));
        element.addChildElement("maxEnqRetries", String.valueOf(maxEnqRetries));
        element.addChildElement("maxFrameRetries", String.valueOf(maxFrameRetries));
        element.addChildElement("strictFrameSequencing", String.valueOf(strictFrameSequencing));
        element.addChildElement("frameNumberStart", String.valueOf(frameNumberStart));
        return element;
    }

    @Override
    public String getName() {
        return "ASTM E1381";
    }
}
