package com.bitdreamit.connect.plugins.transmission.astm.client;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Constants;
import com.mirth.connect.model.converters.ObjectXMLSerializer;
import com.mirth.connect.plugins.TransmissionModeClientProvider;
import com.mirth.connect.plugins.TransmissionModePlugin;

/**
 * Client-side transmission-mode plugin for ASTM E1381-02.
 *
 * <p>Follows the same pattern as Mirth's built-in MLLP plugin, with
 * one addition: the <code>start()</code> method uses pure reflection
 * (no XStream import) to register our Properties class with XStream's
 * security framework. Mirth auto-allows <code>com.mirth.connect.*</code>
 * classes (like MLLP's Properties), but NOT third-party classes like
 * <code>com.bitdreamit.*</code>. This explicit registration bridges
 * that gap.</p>
 */
public class ASTME1381TransmissionModeClientPlugin extends TransmissionModePlugin {

    public ASTME1381TransmissionModeClientPlugin(String name) {
        super(name);
    }

    @Override
    public TransmissionModeClientProvider createProvider() {
        return new ASTME1381ClientProvider();
    }

    /**
     * Register our Properties class with XStream via pure reflection.
     * No compile-time dependency on com.thoughtworks.xstream.
     */
    @Override
    public void start() {
        super.start();
        registerWithXStream();
    }

    private void registerWithXStream() {
        try {
            // Load our Properties class
            Class<?> propsClass = Class.forName(
                "com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties");

            // Get Mirth's serializer instance
            ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();

            // Use reflection to find and call xStream.allowTypes(Class[])
            // The XStream instance is stored somewhere in the serializer.
            // We scan all fields (instance + static) in the class hierarchy.
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
            // Best effort - if reflection fails, Mirth should still
            // register the class via its extension loading mechanism
        }
    }

    public String getPluginPointName() {
        return ASTME1381Constants.PLUGIN_NAME;
    }
}
