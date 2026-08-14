package com.bitdreamit.connect.plugins.transmission.astm.server;

import java.io.InputStream;
import java.io.OutputStream;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Constants;
import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties;
import com.mirth.connect.donkey.server.message.StreamHandler;
import com.mirth.connect.donkey.server.message.batch.BatchStreamReader;
import com.mirth.connect.model.transmission.TransmissionModeProperties;
import com.mirth.connect.plugins.TransmissionModePlugin;

/**
 * ASTM E1381-95 Transmission Mode Server Plugin
 * Registers the ASTM framing protocol with Mirth Connect TCP/Serial connectors.
 */
public class ASTME1381TransmissionModePlugin extends TransmissionModePlugin {

    @Override
    public String getPluginPointName() {
        return ASTME1381Constants.PLUGIN_NAME;
    }

    @Override
    public String getPluginPointDescription() {
        return "ASTM E1381-95 Lower Layer Protocol with frame sequencing, LRC validation, and bidirectional handshaking.";
    }

    @Override
    public void start() {}

    @Override
    public void stop() {}

    @Override
    public StreamHandler getStreamHandler(InputStream inputStream, OutputStream outputStream,
                                           BatchStreamReader batchStreamReader,
                                           TransmissionModeProperties properties) {
        ASTME1381TransmissionModeProperties props = (ASTME1381TransmissionModeProperties) properties;
        return new ASTME1381StreamHandler(inputStream, outputStream, batchStreamReader, props);
    }

    @Override
    public TransmissionModeProperties getDefaultProperties() {
        return new ASTME1381TransmissionModeProperties();
    }
}
