package com.bitdreamit.connect.plugins.transmission.astm.shared;

import java.io.ByteArrayOutputStream;

/**
 * Immutable representation of a single ASTM E1381-02 frame.
 *
 * <p>Wire format produced and consumed by this class:</p>
 * <pre>
 *   STX | FN | text... | (ETB | ETX) | CSH | CSL | CR | LF
 * </pre>
 * where the checksum is the 8-bit Add-Mod-256 of {@code FN + text + terminator},
 * rendered as two uppercase hex characters.
 *
 * <p>Frames are immutable: encode/decode operations create new instances.</p>
 */
public class ASTME1381Frame {

    private final int frameNumber;
    private final byte[] text;
    private final boolean finalFrame;

    /**
     * Build a frame.
     *
     * @param frameNumber frame sequence number, must be 0..7
     * @param text        payload bytes (may be empty, must not be null)
     * @param finalFrame  true if the frame terminates a record (ETX), false for an intermediate frame (ETB)
     * @throws IllegalArgumentException if the frame number is out of range or text is null
     */
    public ASTME1381Frame(int frameNumber, byte[] text, boolean finalFrame) {
        if (frameNumber < 0 || frameNumber > 7) {
            throw new IllegalArgumentException("Frame number must be 0-7, was: " + frameNumber);
        }
        if (text == null) {
            throw new IllegalArgumentException("Frame text must not be null");
        }
        this.frameNumber = frameNumber;
        this.text = text.clone();
        this.finalFrame = finalFrame;
    }

    /**
     * Encode this frame to its wire representation.
     *
     * @return the encoded bytes, including STX, FN, payload, terminator, checksum, CR, LF
     */
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
        byte[] checksumBytes = String.format("%02X", checksum).getBytes();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(ASTME1381Constants.STX);
        try {
            out.write(checksumScope.toByteArray());
            out.write(checksumBytes);
            out.write(ASTME1381Constants.CR);
            out.write(ASTME1381Constants.LF);
        } catch (java.io.IOException e) {
            // ByteArrayOutputStream never throws IOException on write - rethrow as unchecked
            throw new RuntimeException(e);
        }
        return out.toByteArray();
    }

    /**
     * Decode a raw byte array into a frame.
     *
     * <p>The array must include STX at position 0, an FN byte in '0'..'7', an ETB or ETX
     * terminator, two checksum bytes, and a trailing CR LF. The checksum is verified
     * against the recomputed value over FN + text + terminator.</p>
     *
     * @param raw the raw frame bytes
     * @return the decoded frame
     * @throws ASTME1381FrameException if the envelope is malformed or the checksum does not match
     */
    public static ASTME1381Frame decode(byte[] raw) throws ASTME1381FrameException {
        if (raw == null) {
            throw new ASTME1381FrameException("Frame buffer is null");
        }
        if (raw.length < 7) {
            throw new ASTME1381FrameException("Frame too short: " + raw.length + " bytes (min 7)");
        }
        if (raw[0] != ASTME1381Constants.STX) {
            throw new ASTME1381FrameException("Frame missing STX envelope, got: 0x"
                    + Integer.toHexString(raw[0] & 0xFF));
        }
        if (raw[raw.length - 1] != ASTME1381Constants.LF
                || raw[raw.length - 2] != ASTME1381Constants.CR) {
            throw new ASTME1381FrameException("Frame missing CR/LF trailer");
        }
        byte fnByte = raw[1];
        if (fnByte < '0' || fnByte > '7') {
            throw new ASTME1381FrameException("Invalid frame number byte: 0x"
                    + Integer.toHexString(fnByte & 0xFF));
        }
        int frameNumber = fnByte - '0';
        int termIndex = raw.length - 5;
        byte terminator = raw[termIndex];
        if (terminator != ASTME1381Constants.ETB && terminator != ASTME1381Constants.ETX) {
            throw new ASTME1381FrameException("Missing ETB/ETX terminator, got: 0x"
                    + Integer.toHexString(terminator & 0xFF));
        }
        boolean finalFrame = (terminator == ASTME1381Constants.ETX);
        byte[] text = new byte[termIndex - 2];
        System.arraycopy(raw, 2, text, 0, text.length);

        // Recompute checksum over FN + text + terminator (Add-Mod-256)
        int checksum = (fnByte & 0xFF);
        for (byte b : text) {
            checksum = (checksum + (b & 0xFF)) & 0xFF;
        }
        checksum = (checksum + (terminator & 0xFF)) & 0xFF;

        String expectedHex = String.format("%02X", checksum);
        String actualHex = new String(raw, termIndex + 1, 2).toUpperCase();
        if (!expectedHex.equals(actualHex)) {
            throw new ASTME1381FrameException("Checksum mismatch: expected " + expectedHex
                    + " got " + actualHex + " (frame #" + frameNumber + ")");
        }
        return new ASTME1381Frame(frameNumber, text, finalFrame);
    }

    public int getFrameNumber() {
        return frameNumber;
    }

    /** Returns a defensive copy of the frame payload. */
    public byte[] getText() {
        return text.clone();
    }

    public boolean isFinalFrame() {
        return finalFrame;
    }

    @Override
    public String toString() {
        return "ASTME1381Frame{fn=" + frameNumber + ", len=" + text.length
                + ", final=" + finalFrame + '}';
    }
}
