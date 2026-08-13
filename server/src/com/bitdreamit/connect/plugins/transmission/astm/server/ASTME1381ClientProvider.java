package com.bitdreamit.connect.plugins.transmission.astm.server;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Constants;
import com.mirth.connect.plugins.TransmissionModeClientProvider;
import com.mirth.connect.model.transmission.TransmissionModeProperties;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.ArrayList;

public class ASTME1381ClientProvider extends TransmissionModeClientProvider {

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
    public void send(OutputStream out, InputStream in, byte[] data) throws Exception {
        metrics.reset();
        List<byte[]> records = splitIntoRecords(data);
        establishConnection(out, in);
        try {
            int frameNumber = props.getFrameNumberStart();
            for (byte[] record : records) {
                frameNumber = sendRecordChunked(out, in, record, frameNumber);
            }
        } finally {
            out.write(new byte[]{ASTME1381Constants.EOT});
            out.flush();
        }
    }

    private void establishConnection(OutputStream out, InputStream in) throws Exception {
        for (int attempt = 1; attempt <= props.getMaxEnqRetries(); attempt++) {
            out.write(new byte[]{ASTME1381Constants.ENQ});
            out.flush();
            int response = readByteWithTimeout(in, props.getEnqTimeoutMs());
            if (response == ASTME1381Constants.ACK) return;
            if (response == ASTME1381Constants.NAK) {
                metrics.incrementNak();
                metrics.incrementEnqRetry();
                long backoff = Math.min(500L * (1L << (attempt - 1)), 8000L);
                Thread.sleep(backoff);
                continue;
            }
            metrics.incrementEnqRetry();
        }
        throw new ASTME1381FrameException("No ACK to ENQ after " + props.getMaxEnqRetries() + " attempts (metrics: " + metrics.getEnqRetries() + " retries, " + metrics.getNakCount() + " NAKs)");
    }

    private int sendRecordChunked(OutputStream out, InputStream in, byte[] record, int frameNumber) throws Exception {
        int offset = 0;
        while (offset < record.length) {
            int len = Math.min(ASTME1381Constants.MAX_FRAME_TEXT_LENGTH, record.length - offset);
            boolean isLastChunkOfRecord = (offset + len) >= record.length;
            byte[] chunk = new byte[len];
            System.arraycopy(record, offset, chunk, 0, len);

            ASTME1381Frame frame = new ASTME1381Frame(frameNumber % 8, chunk, isLastChunkOfRecord);
            sendFrameWithRetry(out, in, frame);

            offset += len;
            frameNumber = (frameNumber + 1) % 8;
            if (frameNumber == 0) frameNumber = 1;
        }
        return frameNumber;
    }

    private void sendFrameWithRetry(OutputStream out, InputStream in, ASTME1381Frame frame) throws Exception {
        for (int attempt = 1; attempt <= props.getMaxFrameRetries(); attempt++) {
            out.write(frame.encode());
            out.flush();
            int response = readByteWithTimeout(in, props.getFrameAckTimeoutMs());
            if (response == ASTME1381Constants.ACK) return;
            metrics.incrementFrameRetry();
            if (response == ASTME1381Constants.NAK) {
                metrics.incrementNak();
            }
        }
        throw new ASTME1381FrameException("Frame " + frame.getFrameNumber() + " not ACKed after " + props.getMaxFrameRetries() + " retries (total frame retries: " + metrics.getFrameRetries() + ")");
    }

    private int readByteWithTimeout(InputStream in, long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (in.available() > 0) {
                return in.read();
            }
            Thread.sleep(10);
        }
        return -1;
    }

    private List<byte[]> splitIntoRecords(byte[] data) {
        List<byte[]> list = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == ASTME1381Constants.CR || data[i] == ASTME1381Constants.LF) {
                if (i > start) {
                    byte[] record = new byte[i - start];
                    System.arraycopy(data, start, record, 0, record.length);
                    list.add(record);
                }
                start = i + 1;
            }
        }
        if (start < data.length) {
            byte[] record = new byte[data.length - start];
            System.arraycopy(data, start, record, 0, record.length);
            list.add(record);
        }
        if (list.isEmpty()) list.add(data);
        return list;
    }
}
