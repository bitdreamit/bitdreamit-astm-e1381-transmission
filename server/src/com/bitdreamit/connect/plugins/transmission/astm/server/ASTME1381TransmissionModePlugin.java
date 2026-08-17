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
 * Server-side transmission-mode plugin for ASTM E1381-02.
 */
public class ASTME1381TransmissionModePlugin extends TransmissionModeProvider {

    /**
     * Register our Properties class with XStream via pure reflection.
     * Same pattern as the client plugin - see comment there.
     */
    @Override
    public void start() {
        super.start();
        registerWithXStream();
    }

    private void registerWithXStream() {
        try {
            Class<?> propsClass = Class.forName(
                "com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties");

            ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();

            Class<?> xStreamClass = Class.forName("com.thoughtworks.xstream.XStream");
            java.lang.reflect.Method allowMethod = xStreamClass.getMethod(
                "allowTypes", Class[].class);

            Class<?> clazz = ObjectXMLSerializer.class;
            while (clazz != null) {
                for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                    try {
                        field.setAccessible(true);
                        Object fieldValue = field.get(serializer);
                        if (fieldValue != null && xStreamClass.isInstance(fieldValue)) {
                            allowMethod.invoke(fieldValue,
                                (Object) new Class[] { propsClass });
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
