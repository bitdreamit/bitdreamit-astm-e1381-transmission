package com.bitdreamit.connect.plugins.transmission.astm.server;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Constants;
import com.mirth.connect.plugins.TransmissionModeServerProvider;
import com.mirth.connect.model.transmission.TransmissionModeProperties;
import java.io.InputStream;
import java.io.OutputStream;

public class ASTME1381ServerProvider extends TransmissionModeServerProvider {

    private ASTME1381TransmissionModeProperties props;
    private final ASTME1381RetryMetrics metrics = new ASTME1381RetryMetrics();

    @Override
    public void setProperties(TransmissionModeProperties properties) {
        this.props = (ASTME1381TransmissionModeProperties) properties;
    }

    public ASTME1381RetryMetrics getMetrics() {
        return metrics;
    }

    @Override
    public byte[] receive(InputStream in, OutputStream out) throws Exception {
        metrics.reset();
        int first = in.read();
        if (first != ASTME1381Constants.ENQ) {
            throw new ASTME1381FrameException("Expected ENQ (0x05), got: 0x" + Integer.toHexString(first & 0xFF) + " (decimal " + first + ")");
        }
        out.write(new byte[]{ASTME1381Constants.ACK});
        out.flush();

        java.io.ByteArrayOutputStream currentRecord = new java.io.ByteArrayOutputStream();
        int expectedFrameNumber = props.getFrameNumberStart();

        while (true) {
            int peek = in.read();
            if (peek == ASTME1381Constants.EOT) {
                break;
            }
            if (peek != ASTME1381Constants.STX) {
                out.write(new byte[]{ASTME1381Constants.NAK});
                out.flush();
                metrics.incrementNak();
                continue;
            }
            byte[] rawFrame = readUntilLF(in, peek);
            try {
                ASTME1381Frame frame = ASTME1381Frame.decode(rawFrame);
                if (props.isStrictFrameSequencing() && frame.getFrameNumber() != (expectedFrameNumber % 8)) {
                    out.write(new byte[]{ASTME1381Constants.NAK});
                    out.flush();
                    metrics.incrementNak();
                    continue;
                }
                currentRecord.write(frame.getText());
                out.write(new byte[]{ASTME1381Constants.ACK});
                out.flush();

                expectedFrameNumber = (expectedFrameNumber + 1) % 8;
                if (expectedFrameNumber == 0) expectedFrameNumber = 1;

                if (frame.isFinalFrame()) {
                    return currentRecord.toByteArray();
                }
            } catch (ASTME1381FrameException badFrame) {
                out.write(new byte[]{ASTME1381Constants.NAK});
                out.flush();
                metrics.incrementNak();
            }
        }
        return currentRecord.toByteArray();
    }

    private byte[] readUntilLF(InputStream in, int firstByte) throws Exception {
        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        buf.write(firstByte);
        int b;
        while ((b = in.read()) != -1) {
            buf.write(b);
            if (b == ASTME1381Constants.LF) break;
        }
        return buf.toByteArray();
    }
}
