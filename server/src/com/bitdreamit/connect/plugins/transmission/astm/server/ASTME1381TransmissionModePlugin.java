package com.bitdreamit.connect.plugins.transmission.astm.server;

import java.io.InputStream;
import java.io.OutputStream;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Constants;
import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties;
import com.mirth.connect.donkey.server.message.StreamHandler;
import com.mirth.connect.donkey.server.message.batch.BatchStreamReader;
import com.mirth.connect.model.transmission.TransmissionModeProperties;
import com.mirth.connect.plugins.TransmissionModeProvider;
import com.mirth.connect.model.converters.ObjectXMLSerializer;

/**
 * Server-side transmission-mode plugin for ASTM E1381-02.
 *
 * Extends Mirth's TransmissionModeProvider (the same base class Mirth's
 * built-in MLLPModeProvider uses). Mirth's DefaultExtensionController
 * instantiates this class via the <serverClasses> entry in plugin.xml
 * and registers it with the TransmissionModeController.
 */
public class ASTME1381TransmissionModePlugin extends TransmissionModeProvider {

    public ASTME1381TransmissionModePlugin() {
        super();
        whiteListPropertiesClass();
    }

    /**
     * Mirth Connect 4.x deserializes channel XML through a security whitelist.
     * The transmission-mode properties class embedded in channel exports must be
     * allowed explicitly, otherwise the channel is stored as an invalid
     * placeholder ("Verify all required extensions are loaded correctly").
     */
    private void whiteListPropertiesClass() {
        try {
            ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();
            java.lang.reflect.Method method = serializer.getClass()
                    .getMethod("allowTypes", java.util.List.class, java.util.List.class, java.util.List.class);
            java.util.List<String> allowedTypes = new java.util.ArrayList<String>();
            allowedTypes.add(ASTME1381TransmissionModeProperties.class.getCanonicalName());
            method.invoke(serializer, allowedTypes, null, null);
            System.out.println("ASTM E1381 transmission mode properties whitelisted in XStream");
        } catch (NoSuchMethodException e) {
            System.out.println("Old Mirth Connect version, ASTM E1381 whitelist not required");
        } catch (Exception e) {
            System.err.println("Error whitelisting ASTM E1381 transmission mode properties: " + e);
        }
    }

    @Override
    public String getPluginPointName() {
        return ASTME1381Constants.PLUGIN_NAME;
    }

    // NOTE: no @Override - getPluginPointDescription() is NOT declared as abstract
    // in the real Mirth 4.5.2 TransmissionModeProvider base class, so @Override fails.
    public String getPluginPointDescription() {
        return "ASTM E1381-02 lower-layer transmission framing protocol for Mirth Connect. "
             + "Implements ENQ/ACK/NAK establishment, STX/ETB/ETX framing with Add-Mod-256 "
             + "checksum, 0-7 frame sequencing, per-frame retry, and per-phase timeouts. "
             + "Transport-agnostic: works with TCP Listener / TCP Sender and Serial Connector.";
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

    // NOTE: no @Override - getDefaultProperties() is NOT declared in the real Mirth 4.5.2
    // TransmissionModeProvider base class, so @Override fails.
    public TransmissionModeProperties getDefaultProperties() {
        return new ASTME1381TransmissionModeProperties();
    }
}

