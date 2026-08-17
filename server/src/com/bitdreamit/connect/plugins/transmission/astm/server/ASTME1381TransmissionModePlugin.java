package com.bitdreamit.connect.plugins.transmission.astm.server;

import java.io.InputStream;
import java.io.OutputStream;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Constants;
import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties;
import com.mirth.connect.donkey.server.message.StreamHandler;
import com.mirth.connect.donkey.server.message.batch.BatchStreamReader;
import com.mirth.connect.model.converters.ObjectXMLSerializer;
import com.mirth.connect.model.transmission.TransmissionModeProperties;
import com.mirth.connect.plugins.TransmissionModeProvider;

/**
 * ASTM E1381-02 Transmission Mode Server Plugin.
 */
public class ASTME1381TransmissionModePlugin extends TransmissionModeProvider {

    /**
     * Force-register the Properties class with the SERVER-side XStream.
     */
    @Override
    public void start() {
        super.start();
        try {
            Class<?> propsClass = Class.forName(
                "com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties");

            ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();

            // Scan all fields of the serializer class hierarchy for an XStream instance
            Class<?> clazz = serializer.getClass();
            while (clazz != null) {
                for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                    try {
                        field.setAccessible(true);
                        Object obj = field.get(serializer);
                        if (obj instanceof com.thoughtworks.xstream.XStream) {
                            com.thoughtworks.xstream.XStream xStream =
                                (com.thoughtworks.xstream.XStream) obj;
                            xStream.allowTypes(new Class[] { propsClass });
                        }
                    } catch (Exception ignored) {
                    }
                }
                clazz = clazz.getSuperclass();
            }
        } catch (Exception e) {
            // Best effort
        }
    }

    public String getPluginPointName() {
        return ASTME1381Constants.PLUGIN_NAME;
    }

    public String getPluginPointDescription() {
        return "ASTM E1381-02 Lower Layer Protocol with frame sequencing, "
             + "LRC validation, and bidirectional handshaking.";
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
