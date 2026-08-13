package com.bitdreamit.connect.plugins.transmission.astm.server;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Constants;
import com.mirth.connect.model.ExtensionPermission;
import com.mirth.connect.plugins.TransmissionModePlugin;
import com.mirth.connect.plugins.TransmissionModeClientProvider;
import com.mirth.connect.plugins.TransmissionModeServerProvider;
import com.mirth.connect.model.transmission.TransmissionModeProperties;
import java.util.Map;
import java.util.Properties;

public class ASTME1381TransmissionModePlugin extends TransmissionModePlugin {

    @Override
    public void init(Properties properties) {}

    @Override
    public void update(Properties properties) {}

    @Override
    public Properties getDefaultProperties() {
        return new Properties();
    }

    @Override
    public ExtensionPermission[] getExtensionPermissions() {
        return new ExtensionPermission[0];
    }

    @Override
    public Map<String, Object> getObjectsForSwaggerExamples() {
        return null;
    }

    @Override
    public String getPluginPointName() {
        return ASTME1381Constants.PLUGIN_POINT_NAME;
    }

    @Override
    public void start() {}

    @Override
    public void stop() {}

    @Override
    public TransmissionModeClientProvider getTransmissionModeClientProvider() {
        return new ASTME1381ClientProvider();
    }

    @Override
    public TransmissionModeServerProvider getTransmissionModeServerProvider() {
        return new ASTME1381ServerProvider();
    }

    @Override
    public TransmissionModeProperties getDefaultProperties() {
        return new ASTME1381TransmissionModeProperties();
    }
}
