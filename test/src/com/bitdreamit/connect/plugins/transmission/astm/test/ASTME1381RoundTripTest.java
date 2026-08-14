package com.bitdreamit.connect.plugins.transmission.astm.test;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Constants;
import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Frame;
import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381FrameException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * End-to-end round-trip tests verifying that what {@link ASTME1381Frame#encode()}
 * produces matches what a peer implementing ASTM E1381-02 would emit, byte
 * for byte.
 */
public class ASTME1381RoundTripTest {

    @Test
    public void testWireFormatMatchesSpec() {
        // Build a frame with a known payload and verify the exact wire bytes.
        // Payload = "ABC", FN=1, ETX (final)
        // Checksum = (0x31 + 'A' + 'B' + 'C' + 0x03) & 0xFF
        //          = (49 + 65 + 66 + 67 + 3) & 0xFF = 250 = 0xFA
        byte[] text = "ABC".getBytes();
        ASTME1381Frame frame = new ASTME1381Frame(1, text, true);
        byte[] wire = frame.encode();

        assertEquals(10, wire.length); // STX FN ABC ETX CS CS CR LF = 1+1+3+1+2+1+1 = 10
        assertEquals(ASTME1381Constants.STX, wire[0]);
        assertEquals('1', wire[1]);
        assertEquals('A', wire[2]);
        assertEquals('B', wire[3]);
        assertEquals('C', wire[4]);
        assertEquals(ASTME1381Constants.ETX, wire[5]);
        assertEquals('F', wire[6]); // checksum high nibble
        assertEquals('A', wire[7]); // checksum low nibble
        assertEquals(ASTME1381Constants.CR, wire[8]);
        assertEquals(ASTME1381Constants.LF, wire[9]);
    }

    @Test
    public void testIntermediateFrameUsesETB() {
        byte[] text = "Hello".getBytes();
        ASTME1381Frame frame = new ASTME1381Frame(3, text, false);  // intermediate
        byte[] wire = frame.encode();

        // Wire format: STX(0) FN(1) text(2..6) ETB(7) CS(8) CS(9) CR(10) LF(11)
        assertEquals(12, wire.length);
        assertEquals(ASTME1381Constants.STX, wire[0]);
        assertEquals('3', wire[1]);
        assertEquals(ASTME1381Constants.ETB, wire[7]);  // intermediate terminator
    }

    @Test
    public void testAllFrameNumbersRoundTrip() throws ASTME1381FrameException {
        byte[] text = "test".getBytes();
        for (int fn = 0; fn <= 7; fn++) {
            for (boolean isFinal : new boolean[]{true, false}) {
                ASTME1381Frame original = new ASTME1381Frame(fn, text, isFinal);
                byte[] encoded = original.encode();
                ASTME1381Frame decoded = ASTME1381Frame.decode(encoded);
                assertEquals("Frame number mismatch for fn=" + fn, fn, decoded.getFrameNumber());
                assertArrayEquals("Text mismatch for fn=" + fn + " final=" + isFinal,
                        text, decoded.getText());
                assertEquals("Final flag mismatch for fn=" + fn, isFinal, decoded.isFinalFrame());
            }
        }
    }

    @Test
    public void testMaxPayloadLengthRoundTrip() throws ASTME1381FrameException {
        // ASTM E1381-02 recommends max 240 bytes payload per frame.
        byte[] text = new byte[ASTME1381Constants.MAX_FRAME_TEXT_LENGTH];
        for (int i = 0; i < text.length; i++) {
            text[i] = (byte) ('A' + (i % 26));
        }
        ASTME1381Frame frame = new ASTME1381Frame(5, text, true);
        byte[] wire = frame.encode();
        // STX + FN + 240 + ETX + 2 checksum + CR + LF = 247 bytes
        assertEquals(247, wire.length);

        ASTME1381Frame decoded = ASTME1381Frame.decode(wire);
        assertArrayEquals(text, decoded.getText());
        assertEquals(5, decoded.getFrameNumber());
        assertTrue(decoded.isFinalFrame());
    }

    @Test
    public void testBinaryPayloadRoundTrip() throws ASTME1381FrameException {
        // ASTM payloads can contain any byte except the control bytes (STX/ETX/ETB/EOT/ENQ/ACK/NAK).
        // Use a payload that includes all printable ASCII chars.
        byte[] text = new byte[128];
        for (int i = 0; i < 128; i++) {
            text[i] = (byte) i;
        }
        // Skip the control bytes - replace them with '.'
        for (int i = 0; i < text.length; i++) {
            byte b = text[i];
            if (b == ASTME1381Constants.STX || b == ASTME1381Constants.ETX
                    || b == ASTME1381Constants.ETB || b == ASTME1381Constants.EOT
                    || b == ASTME1381Constants.ENQ || b == ASTME1381Constants.ACK
                    || b == ASTME1381Constants.NAK) {
                text[i] = '.';
            }
        }
        ASTME1381Frame frame = new ASTME1381Frame(7, text, true);
        byte[] wire = frame.encode();
        ASTME1381Frame decoded = ASTME1381Frame.decode(wire);
        assertArrayEquals(text, decoded.getText());
    }

    @Test
    public void testEmptyPayloadRoundTrip() throws ASTME1381FrameException {
        ASTME1381Frame frame = new ASTME1381Frame(0, new byte[0], true);
        byte[] wire = frame.encode();
        // STX FN ETX CS CS CR LF = 7 bytes
        assertEquals(7, wire.length);
        ASTME1381Frame decoded = ASTME1381Frame.decode(wire);
        assertEquals(0, decoded.getText().length);
        assertTrue(decoded.isFinalFrame());
        assertEquals(0, decoded.getFrameNumber());
    }

    @Test
    public void testFrameNumberWrapAround() throws ASTME1381FrameException {
        // After frame 7, the next frame should be 0 (per ASTM E1381-02).
        // We just verify all 8 frame numbers are valid here.
        for (int fn = 0; fn <= 7; fn++) {
            ASTME1381Frame f = new ASTME1381Frame(fn, "x".getBytes(), true);
            ASTME1381Frame decoded = ASTME1381Frame.decode(f.encode());
            assertEquals(fn, decoded.getFrameNumber());
        }
    }

    @Test
    public void testToString() {
        ASTME1381Frame f = new ASTME1381Frame(2, "abc".getBytes(), false);
        String s = f.toString();
        assertTrue(s.contains("fn=2"));
        assertTrue(s.contains("len=3"));
        assertTrue(s.contains("final=false"));
    }
}
