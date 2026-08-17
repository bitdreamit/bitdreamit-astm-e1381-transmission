package com.bitdreamit.connect.plugins.transmission.astm.server;

import java.io.InputStream;
import java.io.OutputStream;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Constants;
import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties;
import com.mirth.connect.donkey.server.message.StreamHandler;
import com.mirth.connect.donkey.server.message.batch.BatchStreamReader;
import com.mirth.connect.model.transmission.TransmissionModeProperties;
import com.mirth.connect.plugins.TransmissionModeProvider;

/**
 * Server-side transmission-mode plugin for ASTM E1381-02.
 *
 * Follows the exact same pattern as Mirth's built-in MLLP plugin.
 * Clean MLLP-style pattern - extends FrameModeProperties
 * auto-allows them.
 */
public class ASTME1381TransmissionModePlugin extends TransmissionModeProvider {

    public String getPluginPointName() {
        return ASTME1381Constants.PLUGIN_NAME;
    }

    @Override
    public StreamHandler getStreamHandler(InputStream inputStream,
                                          OutputStream outputStream,
                                          BatchStreamReader batchStreamReader,
                                          TransmissionModeProperties properties) {
        ASTME1381TransmissionModeProperties props =
            (ASTME1381TransmissionModeProperties) properties;
        return new ASTME1381StreamHandler(inputStream, outputStream,
                                          batchStreamReader, props);
    }

    public TransmissionModeProperties getDefaultProperties() {
        return new ASTME1381TransmissionModeProperties();
    }
}
