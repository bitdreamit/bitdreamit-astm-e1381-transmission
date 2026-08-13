/*
 * BitDreamIT Mirth Lab Extensions
 * Copyright (c) 2026 Kimi AI (Moonshot AI) — MIT License
 */
package com.bitdreamit.mirth.labextensions.astme1381transmission;

import com.mirth.connect.plugins.transmissionmode.TransmissionModePlugin;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * ASTM E1381 Transmission Mode plugin for Mirth Connect.
 * Full state-machine implementation with protocol logging.
 */
public class AstmE1381TransmissionMode implements TransmissionModePlugin {
    private static final Logger logger = Logger.getLogger(AstmE1381TransmissionMode.class);

    @Override
    public String getPluginPointName() { return "ASTM E1381"; }

    @Override
    public Object getFrameDelimiter() { return null; } // Custom framing

    @Override
    public String read(InputStream in, OutputStream out, Object pluginProperties) throws IOException {
        AstmE1381ModeProperties props = (AstmE1381ModeProperties) pluginProperties;
        AstmFrameHandler handler = new AstmFrameHandler(props);

        logger.info("ASTM E1381 read started, dialect=" + props.getDialect());

        if (props.isUseEnqAck()) {
            boolean ok = handler.receiverHandshake(in, out);
            if (!ok) {
                throw new IOException("ASTM ENQ/ACK handshake failed");
            }
        }

        String message = handler.readMessage(in, out);
        logger.info("ASTM E1381 read complete, state=" + handler.getState());
        return message;
    }

    @Override
    public void write(OutputStream out, InputStream in, Object data, Object pluginProperties) throws IOException {
        AstmE1381ModeProperties props = (AstmE1381ModeProperties) pluginProperties;
        AstmFrameHandler handler = new AstmFrameHandler(props);

        logger.info("ASTM E1381 write started, dialect=" + props.getDialect());

        if (props.isUseEnqAck()) {
            boolean ok = handler.senderHandshake(out, in);
            if (!ok) {
                throw new IOException("ASTM ENQ/ACK handshake failed");
            }
        }

        handler.writeMessage(out, in, (String) data);
        handler.sendEot(out);
        logger.info("ASTM E1381 write complete, state=" + handler.getState());
    }
}