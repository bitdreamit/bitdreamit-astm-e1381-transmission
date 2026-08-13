package com.bitdreamit.connect.plugins.transmission.astm.test;

import com.bitdreamit.connect.plugins.transmission.astm.server.ASTME1381Frame;
import com.bitdreamit.connect.plugins.transmission.astm.server.ASTME1381FrameException;
import org.junit.Test;
import static org.junit.Assert.*;

public class ASTME1381FrameTest {

    @Test
    public void testEncodeDecodeRoundTrip() throws Exception {
        String text = "H|\^&|||Analyzer^1.0|||||HOST||P|1|20260813120000";
        byte[] textBytes = text.getBytes();
        for (int fn = 0; fn <= 7; fn++) {
            for (boolean finalFrame : new boolean[]{true, false}) {
                ASTME1381Frame original = new ASTME1381Frame(fn, textBytes, finalFrame);
                byte[] encoded = original.encode();
                ASTME1381Frame decoded = ASTME1381Frame.decode(encoded);
                assertEquals(fn, decoded.getFrameNumber());
                assertArrayEquals(textBytes, decoded.getText());
                assertEquals(finalFrame, decoded.isFinalFrame());
            }
        }
    }

    @Test
    public void testEmptyTextFrame() throws Exception {
        ASTME1381Frame frame = new ASTME1381Frame(1, new byte[0], true);
        byte[] encoded = frame.encode();
        ASTME1381Frame decoded = ASTME1381Frame.decode(encoded);
        assertEquals(0, decoded.getText().length);
        assertTrue(decoded.isFinalFrame());
    }

    @Test
    public void testMaxLengthFrame() throws Exception {
        byte[] text = new byte[240];
        for (int i = 0; i < 240; i++) text[i] = (byte) ('A' + (i % 26));
        ASTME1381Frame frame = new ASTME1381Frame(3, text, false);
        byte[] encoded = frame.encode();
        assertEquals(240 + 7, encoded.length); // STX + FN + 240 + ETB + CS1 + CS2 + CR + LF
        ASTME1381Frame decoded = ASTME1381Frame.decode(encoded);
        assertArrayEquals(text, decoded.getText());
    }

    @Test(expected = ASTME1381FrameException.class)
    public void testBadChecksumThrows() throws Exception {
        byte[] raw = new byte[]{
            0x02, '1', 'A', 'B', 'C',
            0x17, '0', '0', 0x0D, 0x0A
        };
        ASTME1381Frame.decode(raw);
    }

    @Test(expected = ASTME1381FrameException.class)
    public void testMissingSTXThrows() throws Exception {
        byte[] raw = new byte[]{
            '1', 'A', 'B', 'C',
            0x17, '0', '1', 0x0D, 0x0A
        };
        ASTME1381Frame.decode(raw);
    }
}
