package com.bitdreamit.connect.plugins.transmission.astm.client;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Constants;
import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties;
import com.mirth.connect.model.converters.ObjectXMLSerializer;
import com.mirth.connect.plugins.TransmissionModeClientProvider;
import com.mirth.connect.plugins.TransmissionModePlugin;

/**
 * Client-side transmission-mode plugin registration for ASTM E1381-02.
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
     * Force-register the Properties class with XStream's security framework.
     *
     * Mirth's NoTypePermission denies ALL classes by default. Only classes
     * registered via xStream.allowTypes() can be deserialized. The
     * Properties class needs to be registered on BOTH the server-side
     * and client-side XStream instances.
     *
     * The server-side registration happens via transmissionmode.xml's
     * <sharedClassName>. But the client-side XStream instance is separate
     * and sometimes doesn't pick up the registration (especially after
     * extension upgrades). This method force-registers the class using
     * reflection to access the ObjectXMLSerializer's internal XStream.
     */
    @Override
    public void start() {
        super.start();
        try {
            Class<?> propsClass = Class.forName(
                "com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties");

            // Get the ObjectXMLSerializer instance
            ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();

            // Use reflection to access the internal XStream field
            // The field name may vary across Mirth versions, so try several
            String[] possibleFieldNames = {"xStream", "xstream", "XSTREAM", "serializer"};
            boolean registered = false;

            for (String fieldName : possibleFieldNames) {
                try {
                    java.lang.reflect.Field field = ObjectXMLSerializer.class.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object obj = field.get(serializer);
                    if (obj instanceof com.thoughtworks.xstream.XStream) {
                        com.thoughtworks.xstream.XStream xStream = (com.thoughtworks.xstream.XStream) obj;
                        xStream.allowTypes(new Class[] { propsClass });
                        registered = true;
                        break;
                    }
                } catch (NoSuchFieldException ignored) {
                    // Try next field name
                }
            }

            // If direct field access didn't work, try getting XStream via
            // the XStream instance from the class itself (static field)
            if (!registered) {
                try {
                    java.lang.reflect.Field field = ObjectXMLSerializer.class.getDeclaredField("XSTREAM_ALIAS");
                    field.setAccessible(true);
                    // Try another approach
                } catch (Exception ignored) {
                }
            }

            // Last resort: try to use the XStream class directly
            if (!registered) {
                // The XStream instance might be accessible via a different path
                // Try getting it from the serializer's class hierarchy
                Class<?> clazz = serializer.getClass();
                while (clazz != null && !registered) {
                    for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                        field.setAccessible(true);
                        Object obj = field.get(serializer);
                        if (obj instanceof com.thoughtworks.xstream.XStream) {
                            com.thoughtworks.xstream.XStream xStream = (com.thoughtworks.xstream.XStream) obj;
                            xStream.allowTypes(new Class[] { propsClass });
                            registered = true;
                            break;
                        }
                    }
                    clazz = clazz.getSuperclass();
                }
            }
        } catch (Exception e) {
            // Best effort - if all reflection attempts fail, Mirth should
            // have already registered the class via transmissionmode.xml
        }
    }

    public String getPluginPointName() {
        return ASTME1381Constants.PLUGIN_NAME;
    }
}
