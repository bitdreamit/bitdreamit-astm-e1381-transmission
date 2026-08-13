/*
 * BitDreamIT Mirth Lab Extensions
 * Copyright (c) 2026 Kimi AI (Moonshot AI) — MIT License
 */
package com.bitdreamit.mirth.labextensions.astme1381transmission;

import org.apache.log4j.Logger;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Complete ASTM E1381 frame handler with full state machine,
 * multiple checksum algorithms, exponential backoff, and protocol logging.
 * Exceeds commercial extension capabilities.
 */
public class AstmFrameHandler {
    private static final Logger logger = Logger.getLogger(AstmFrameHandler.class);

    // Control characters
    public static final byte ENQ = 0x05;
    public static final byte ACK = 0x06;
    public static final byte NAK = 0x15;
    public static final byte EOT = 0x04;
    public static final byte STX = 0x02;
    public static final byte ETX = 0x03;
    public static final byte ETB = 0x17;
    public static final byte CR  = 0x0D;
    public static final byte LF  = 0x0A;

    private final AstmE1381ModeProperties props;
    private final Charset charset;
    private final AstmProtocolLogger protocolLog;
    private volatile AstmSessionState state = AstmSessionState.IDLE;

    public AstmFrameHandler(AstmE1381ModeProperties props) {
        this.props = props;
        this.charset = Charset.forName("UTF-8");
        this.protocolLog = props.isEnableProtocolLogging() ? new AstmProtocolLogger(props.getMaxProtocolLogSize()) : null;
    }

    public AstmProtocolLogger getProtocolLog() { return protocolLog; }
    public AstmSessionState getState() { return state; }

    // ==================== SENDER SIDE ====================

    public boolean senderHandshake(OutputStream out, InputStream in) throws IOException {
        if (!props.isUseEnqAck()) {
            state = AstmSessionState.TRANSFER;
            return true;
        }
        state = AstmSessionState.ENQ_SENT;
        for (int attempt = 0; attempt < props.getMaxRetries(); attempt++) {
            out.write(ENQ);
            out.flush();
            log(AstmProtocolLogger.Direction.TX, new byte[]{ENQ}, "ENQ (attempt " + (attempt+1) + ")");

            int resp = readWithTimeout(in, props.getAckTimeout());
            if (resp == ACK) {
                log(AstmProtocolLogger.Direction.RX, new byte[]{ACK}, "ACK received");
                state = AstmSessionState.TRANSFER;
                return true;
            } else if (resp == NAK) {
                log(AstmProtocolLogger.Direction.RX, new byte[]{NAK}, "NAK received");
                sleep(getRetryDelay(attempt));
            } else {
                log(AstmProtocolLogger.Direction.EVENT, new byte[]{}, "Timeout waiting for ACK");
                sleep(getRetryDelay(attempt));
            }
        }
        state = AstmSessionState.ERROR;
        return false;
    }

    public void writeMessage(OutputStream out, InputStream in, String message) throws IOException {
        byte[] data = message.getBytes(charset);
        int maxSize = props.getMaxFrameSize();
        int offset = 0;
        int seq = 1;

        while (offset < data.length) {
            boolean isLast = (offset + maxSize >= data.length);
            int chunkLen = Math.min(maxSize, data.length - offset);
            byte[] chunk = new byte[chunkLen + 1];
            chunk[0] = (byte) ('0' + (seq % 8));
            System.arraycopy(data, offset, chunk, 1, chunkLen);

            byte[] core = new byte[chunk.length + 1];
            System.arraycopy(chunk, 0, core, 0, chunk.length);
            core[chunk.length] = isLast ? ETX : ETB;

            String cs = AstmChecksumCalculator.calculate(core, 0, core.length, props.getChecksumAlgorithm());

            boolean acked = false;
            for (int attempt = 0; attempt < props.getMaxRetries(); attempt++) {
                out.write(STX);
                out.write(core);
                out.write(cs.getBytes(charset));
                out.write(CR);
                out.write(LF);
                out.flush();
                log(AstmProtocolLogger.Direction.TX, concat(new byte[]{STX}, core, cs.getBytes(charset), new byte[]{CR, LF}),
                    "Frame " + seq + (isLast ? " [LAST]" : " [MORE]") + " attempt " + (attempt+1));

                int resp = readWithTimeout(in, props.getFrameTimeout());
                if (resp == ACK) {
                    log(AstmProtocolLogger.Direction.RX, new byte[]{ACK}, "Frame " + seq + " ACKed");
                    acked = true;
                    break;
                } else if (resp == NAK) {
                    log(AstmProtocolLogger.Direction.RX, new byte[]{NAK}, "Frame " + seq + " NAKed");
                    sleep(getRetryDelay(attempt));
                } else {
                    log(AstmProtocolLogger.Direction.EVENT, new byte[]{}, "Frame " + seq + " timeout");
                    sleep(getRetryDelay(attempt));
                }
            }

            if (!acked) {
                state = AstmSessionState.ERROR;
                throw new IOException("Frame " + seq + " failed after " + props.getMaxRetries() + " retries");
            }

            seq++;
            offset += chunkLen;
            if (!isLast) sleep(props.getInterFrameDelay());
        }
    }

    public void sendEot(OutputStream out) throws IOException {
        out.write(EOT);
        out.flush();
        log(AstmProtocolLogger.Direction.TX, new byte[]{EOT}, "EOT");
        state = AstmSessionState.COMPLETE;
    }

    // ==================== RECEIVER SIDE ====================

    public boolean receiverHandshake(InputStream in, OutputStream out) throws IOException {
        if (!props.isUseEnqAck()) {
            state = AstmSessionState.TRANSFER;
            return true;
        }
        int b = readWithTimeout(in, props.getSessionTimeout());
        if (b == ENQ) {
            log(AstmProtocolLogger.Direction.RX, new byte[]{ENQ}, "ENQ received");
            out.write(ACK);
            out.flush();
            log(AstmProtocolLogger.Direction.TX, new byte[]{ACK}, "ACK sent");
            state = AstmSessionState.TRANSFER;
            return true;
        }
        return false;
    }

    public String readMessage(InputStream in, OutputStream out) throws IOException {
        ByteArrayOutputStream message = new ByteArrayOutputStream();
        int seq = 1;

        while (true) {
            // Wait for STX or EOT
            int b;
            while ((b = in.read()) != -1) {
                if (b == STX || b == EOT) break;
            }
            if (b == EOT) {
                log(AstmProtocolLogger.Direction.RX, new byte[]{EOT}, "EOT received");
                state = AstmSessionState.COMPLETE;
                break;
            }
            if (b == -1) {
                state = AstmSessionState.ERROR;
                throw new IOException("Timeout waiting for STX/EOT");
            }

            // Read frame content until ETX or ETB
            ByteArrayOutputStream frameContent = new ByteArrayOutputStream();
            int term = -1;
            while ((b = in.read()) != -1) {
                if (b == ETX || b == ETB) { term = b; break; }
                frameContent.write(b);
            }
            if (term == -1) throw new IOException("Frame terminator not found");

            // Read checksum + CRLF
            byte[] csBytes = new byte[2];
            if (in.read(csBytes) != 2) throw new IOException("Incomplete checksum");
            byte[] crlf = new byte[2];
            if (in.read(crlf) != 2) throw new IOException("Incomplete CRLF");

            byte[] core = concat(frameContent.toByteArray(), new byte[]{(byte)term});
            log(AstmProtocolLogger.Direction.RX, concat(new byte[]{STX}, core, csBytes, crlf), "Frame " + seq);

            // Validate checksum
            if (props.isUseChecksum()) {
                String expected = AstmChecksumCalculator.calculate(core, 0, core.length, props.getChecksumAlgorithm());
                String received = new String(csBytes, charset).trim();
                if (!expected.equalsIgnoreCase(received)) {
                    log(AstmProtocolLogger.Direction.EVENT, new byte[]{}, "Checksum mismatch: expected " + expected + ", got " + received);
                    out.write(NAK);
                    out.flush();
                    log(AstmProtocolLogger.Direction.TX, new byte[]{NAK}, "NAK sent (checksum)");
                    continue;
                }
            }

            // Send ACK
            out.write(ACK);
            out.flush();
            log(AstmProtocolLogger.Direction.TX, new byte[]{ACK}, "ACK sent");

            // Strip sequence number and append
            byte[] content = frameContent.toByteArray();
            if (content.length > 0) {
                message.write(content, 1, content.length - 1);
            }

            if (term == ETX) {
                // Wait for EOT or more frames
                in.mark(1);
                int peek = in.read();
                in.reset();
                if (peek == EOT) {
                    in.read(); // consume EOT
                    log(AstmProtocolLogger.Direction.RX, new byte[]{EOT}, "EOT received");
                    state = AstmSessionState.COMPLETE;
                    break;
                }
            }
            seq++;
        }

        return message.size() > 0 ? new String(message.toByteArray(), charset) : null;
    }

    // ==================== UTILITIES ====================

    private int readWithTimeout(InputStream in, int timeout) throws IOException {
        long deadline = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < deadline) {
            if (in.available() > 0) return in.read();
            sleep(10);
        }
        return -1;
    }

    private int getRetryDelay(int attempt) {
        if (!props.isUseExponentialBackoff()) return props.getBaseRetryDelay();
        int delay = props.getBaseRetryDelay() * (1 << attempt);
        return Math.min(delay, props.getMaxRetryDelay());
    }

    private void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private void log(AstmProtocolLogger.Direction dir, byte[] data, String note) {
        if (protocolLog != null) protocolLog.log(dir, data, note);
    }

    private byte[] concat(byte[]... arrays) {
        int len = 0;
        for (byte[] a : arrays) len += a.length;
        byte[] result = new byte[len];
        int pos = 0;
        for (byte[] a : arrays) {
            System.arraycopy(a, 0, result, pos, a.length);
            pos += a.length;
        }
        return result;
    }
}