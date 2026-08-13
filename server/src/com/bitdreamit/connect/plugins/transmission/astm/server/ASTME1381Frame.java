package com.bitdreamit.connect.plugins.transmission.astm.server;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Constants;
import java.io.ByteArrayOutputStream;

public class ASTME1381Frame {
    private int frameNumber;
    private byte[] text;
    private boolean finalFrame;

    public ASTME1381Frame(int frameNumber, byte[] text, boolean finalFrame) {
        if (frameNumber < 0 || frameNumber > 7) {
            throw new IllegalArgumentException("Frame number must be 0-7");
        }
        this.frameNumber = frameNumber;
        this.text = text;
        this.finalFrame = finalFrame;
    }

    public byte[] encode() {
        byte terminator = finalFrame ? ASTME1381Constants.ETX : ASTME1381Constants.ETB;
        byte fnByte = (byte) ('0' + frameNumber);

        ByteArrayOutputStream checksumScope = new ByteArrayOutputStream();
        checksumScope.write(fnByte);
        checksumScope.write(text, 0, text.length);
        checksumScope.write(terminator);

        int checksum = 0;
        for (byte b : checksumScope.toByteArray()) {
            checksum = (checksum + (b & 0xFF)) & 0xFF;
        }
        String hex = String.format("%02X", checksum);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(ASTME1381Constants.STX);
        try {
            out.write(checksumScope.toByteArray());
            out.write(hex.getBytes());
            out.write(ASTME1381Constants.CR);
            out.write(ASTME1381Constants.LF);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return out.toByteArray();
    }

    public static ASTME1381Frame decode(byte[] raw) throws ASTME1381FrameException {
        if (raw.length < 7 || raw[0] != ASTME1381Constants.STX
                || raw[raw.length - 1] != ASTME1381Constants.LF
                || raw[raw.length - 2] != ASTME1381Constants.CR) {
            throw new ASTME1381FrameException("Frame missing STX/CR/LF envelope");
        }
        byte fnByte = raw[1];
        if (fnByte < '0' || fnByte > '7') {
            throw new ASTME1381FrameException("Invalid frame number byte: " + fnByte);
        }
        int frameNumber = fnByte - '0';
        int termIndex = raw.length - 5;
        byte terminator = raw[termIndex];
        if (terminator != ASTME1381Constants.ETB && terminator != ASTME1381Constants.ETX) {
            throw new ASTME1381FrameException("Missing ETB/ETX terminator");
        }
        boolean finalFrame = (terminator == ASTME1381Constants.ETX);
        byte[] text = new byte[termIndex - 2];
        System.arraycopy(raw, 2, text, 0, text.length);

        int checksum = (fnByte & 0xFF);
        for (byte b : text) checksum = (checksum + (b & 0xFF)) & 0xFF;
        checksum = (checksum + (terminator & 0xFF)) & 0xFF;

        String expectedHex = String.format("%02X", checksum);
        String actualHex = new String(raw, termIndex + 1, 2).toUpperCase();
        if (!expectedHex.equals(actualHex)) {
            throw new ASTME1381FrameException("Checksum mismatch: expected " + expectedHex + " got " + actualHex);
        }
        return new ASTME1381Frame(frameNumber, text, finalFrame);
    }

    public int getFrameNumber() { return frameNumber; }
    public byte[] getText() { return text; }
    public boolean isFinalFrame() { return finalFrame; }
}
