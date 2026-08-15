package com.bitdreamit.connect.plugins.transmission.astm.client;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Frame;
import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381FrameException;
import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381RetryMetrics;
import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Constants;
import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties;
import com.mirth.connect.plugins.TransmissionModeClientProvider;
import com.mirth.connect.model.transmission.TransmissionModeProperties;
import org.apache.log4j.Logger;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.ArrayList;

/**
 * Client-side transmission provider for the ASTM E1381-02 protocol.
 *
 * <p>Extends Mirth's {@code TransmissionModeClientProvider} (the abstract
 * base class for transmission-mode client-side providers in Mirth Connect
 * 4.5+). The Mirth framework instantiates this class via the
 * {@code createProvider()} factory on
 * {@code ASTME1381TransmissionModeClientPlugin}, then calls
 * {@link #setProperties(TransmissionModeProperties)} to inject the
 * channel's configured properties, and finally calls
 * {@link #send(OutputStream, InputStream, byte[])} for each outbound
 * message.</p>
 *
 * <p><b>Required overrides from {@code TransmissionModeClientProvider}:</b>
 * <ul>
 *   <li>{@code getSampleValue()} - returns a sample ASTM E1381 message
 *       string (used by the channel editor's "Send Test Message"
 *       feature).</li>
 *   <li>{@code send(...)} - drives the ENQ -> ACK -> frames -> EOT flow.</li>
 * </ul></p>
 *
 * <p>{@code setProperties(TransmissionModeProperties)} is declared on the
 * parent and is annotated {@code @Override}.</p>
 */
public class ASTME1381ClientProvider extends TransmissionModeClientProvider {

    private static final Logger logger = Logger.getLogger(ASTME1381ClientProvider.class);

    /**
     * Sample ASTM E1381 message used by Mirth's "Send Test Message"
     * feature in the channel editor. The value is a minimal but valid
     * ASTM E1381-02 frame sequence: one record split across a single
     * final frame (FN=1, ETX). It is NOT pre-encoded with STX/checksum/
     * CR/LF because {@link #send(OutputStream, InputStream, byte[])}
     * will re-frame whatever payload it receives - the sample value
     * represents the application-layer payload, not the wire format.
     *
     * <p>Override of the abstract method declared on
     * {@code TransmissionModeClientProvider}.</p>
     */
    @Override
    public String getSampleValue() {
        // Minimal ASTM E1381 application-layer payload. The send() method
        // will wrap this into STX/FN/payload/ETX/checksum/CR/LF frames.
        // The payload below follows the ASTM E1394 (clinical chemistry)
        // record layout that is commonly carried over E1381 framing.
        return "H|\\^&|||ASTM|||||P|1\r"
             + "P|1|||Patient^Test||||||||||||||\r"
             + "O|1|SAMPLE01||ALL||||||||O|||||||\r"
             + "R|1|^^GLU^GLUCOSE|180|mg/dL|70-105|N|||2024\r"
             + "L|1|N\r";
    }

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
        if (out == null) throw new IllegalArgumentException("OutputStream is null");
        if (in == null)  throw new IllegalArgumentException("InputStream is null");
        if (data == null || data.length == 0) {
            logger.debug("send() called with empty payload - skipping");
            return;
        }
        metrics.reset();
        metrics.markSessionStart();
        List<byte[]> records = splitIntoRecords(data);
        logger.info("ASTM E1381 send session starting (" + records.size()
            + " record(s), " + data.length + " bytes)");
        establishConnection(out, in);
        try {
            int frameNumber = props.getFrameNumberStart();
            for (byte[] record : records) {
                frameNumber = sendRecordChunked(out, in, record, frameNumber);
            }
        } finally {
            out.write(new byte[]{ASTME1381Constants.EOT});
            out.flush();
            logger.info("ASTM E1381 send complete (sent=" + metrics.getFramesSent()
                + ", retries=" + metrics.getFrameRetries()
                + ", naks=" + metrics.getNakCount() + ")");
        }
    }

    private void establishConnection(OutputStream out, InputStream in) throws Exception {
        long backoffBase = ASTME1381Constants.DEFAULT_ENQ_BACKOFF_BASE_MS;
        long backoffCap = ASTME1381Constants.DEFAULT_ENQ_BACKOFF_CAP_MS;
        for (int attempt = 1; attempt <= props.getMaxEnqRetries(); attempt++) {
            out.write(new byte[]{ASTME1381Constants.ENQ});
            out.flush();
            logger.debug("ENQ sent (attempt " + attempt + "/" + props.getMaxEnqRetries() + ")");
            int response = readByteWithTimeout(in, props.getEnqTimeoutMs());
            if (response == ASTME1381Constants.ACK) {
                logger.info("ASTM E1381 session established after " + attempt + " ENQ attempt(s)");
                return;
            }
            if (response == ASTME1381Constants.NAK) {
                metrics.incrementNak();
                metrics.incrementEnqRetry();
                long backoff = Math.min(backoffBase * (1L << (attempt - 1)), backoffCap);
                logger.warn("NAK on ENQ attempt " + attempt + ", backing off " + backoff + "ms");
                Thread.sleep(backoff);
                continue;
            }
            metrics.incrementEnqRetry();
            logger.warn("No ACK/NAK to ENQ (response=" + response + ") on attempt " + attempt);
        }
        throw new ASTME1381FrameException("No ACK to ENQ after " + props.getMaxEnqRetries()
            + " attempts (metrics: " + metrics.getEnqRetries() + " retries, "
            + metrics.getNakCount() + " NAKs)");
    }

    private int sendRecordChunked(OutputStream out, InputStream in, byte[] record, int frameNumber) throws Exception {
        int offset = 0;
        while (offset < record.length) {
            int len = Math.min(props.getMaxFrameContentLength(), record.length - offset);
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
            metrics.incrementFramesSent();
            int response = readByteWithTimeout(in, props.getFrameAckTimeoutMs());
            if (response == ASTME1381Constants.ACK) {
                logger.debug("ACK received for frame #" + frame.getFrameNumber());
                return;
            }
            metrics.incrementFrameRetry();
            if (response == ASTME1381Constants.NAK) {
                metrics.incrementNak();
                logger.warn("NAK received for frame #" + frame.getFrameNumber()
                    + " (attempt " + attempt + "/" + props.getMaxFrameRetries() + ")");
            } else {
                logger.warn("No ACK/NAK (response=" + response + ") for frame #"
                    + frame.getFrameNumber() + " (attempt " + attempt + ")");
            }
        }
        throw new ASTME1381FrameException("Frame " + frame.getFrameNumber() + " not ACKed after "
            + props.getMaxFrameRetries() + " retries (total frame retries: "
            + metrics.getFrameRetries() + ")");
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
