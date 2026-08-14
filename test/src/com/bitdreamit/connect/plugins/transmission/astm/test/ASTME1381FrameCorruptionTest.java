package com.bitdreamit.connect.plugins.transmission.astm.test;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Frame;
import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381FrameException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Verifies that {@link ASTME1381Frame#decode(byte[])} rejects malformed
 * input rather than silently producing a corrupt frame.
 */
public class ASTME1381FrameCorruptionTest {

    @Test(expected = ASTME1381FrameException.class)
    public void testNullBufferRejected() throws ASTME1381FrameException {
        ASTME1381Frame.decode(null);
    }

    @Test(expected = ASTME1381FrameException.class)
    public void testTooShortBufferRejected() throws ASTME1381FrameException {
        // Only 5 bytes - minimum is 7 (STX FN ETX CS CS CR LF)
        ASTME1381Frame.decode(new byte[]{0x02, '1', 0x03, 'A', 'B'});
    }

    @Test(expected = ASTME1381FrameException.class)
    public void testMissingSTXRejected() throws ASTME1381FrameException {
        byte[] raw = new byte[]{
            'X', '1', 'A', 'B', 'C', 0x03, '0', '0', 0x0D, 0x0A
        };
        ASTME1381Frame.decode(raw);
    }

    @Test(expected = ASTME1381FrameException.class)
    public void testMissingCRLFRejected() throws ASTME1381FrameException {
        byte[] raw = new byte[]{
            0x02, '1', 'A', 'B', 'C', 0x03, 'A', 'B', 'X', 'Y'
        };
        ASTME1381Frame.decode(raw);
    }

    @Test(expected = ASTME1381FrameException.class)
    public void testInvalidFrameNumberRejected() throws ASTME1381FrameException {
        // FN = '8' is invalid (only 0-7 are valid)
        byte[] raw = new byte[]{
            0x02, '8', 'A', 0x03, '0', '0', 0x0D, 0x0A
        };
        ASTME1381Frame.decode(raw);
    }

    @Test(expected = ASTME1381FrameException.class)
    public void testMissingTerminatorRejected() throws ASTME1381FrameException {
        // terminator position contains a regular byte (e.g. 'X') instead of ETB/ETX
        byte[] raw = new byte[]{
            0x02, '1', 'A', 'X', '0', '0', 0x0D, 0x0A
        };
        ASTME1381Frame.decode(raw);
    }

    @Test(expected = ASTME1381FrameException.class)
    public void testChecksumMismatchRejected() throws ASTME1381FrameException {
        // Checksum bytes deliberately wrong
        byte[] raw = new byte[]{
            0x02, '1', 'A', 'B', 'C', 0x17, 'F', 'F', 0x0D, 0x0A
        };
        ASTME1381Frame.decode(raw);
    }

    @Test
    public void testDecodeProducesDefensiveCopy() throws ASTME1381FrameException {
        byte[] text = "Hello ASTM".getBytes();
        ASTME1381Frame frame = new ASTME1381Frame(2, text, true);
        byte[] encoded = frame.encode();
        ASTME1381Frame decoded = ASTME1381Frame.decode(encoded);

        byte[] text1 = decoded.getText();
        text1[0] = (byte) 'X';   // mutate the returned array
        byte[] text2 = decoded.getText();
        assertEquals("Hello ASTM".charAt(0), (char) text2[0]); // not mutated
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFrameConstructorRejectsNegativeFrameNumber() {
        new ASTME1381Frame(-1, new byte[]{}, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFrameConstructorRejectsFrameNumberAboveSeven() {
        new ASTME1381Frame(8, new byte[]{}, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFrameConstructorRejectsNullText() {
        new ASTME1381Frame(0, null, true);
    }
}
