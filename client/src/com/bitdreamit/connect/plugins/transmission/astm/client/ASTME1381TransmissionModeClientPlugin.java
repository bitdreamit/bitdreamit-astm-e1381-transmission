package com.bitdreamit.connect.plugins.transmission.astm.client;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Constants;
import com.mirth.connect.client.ui.AbstractSettingsPanel;
import com.mirth.connect.plugins.TransmissionModeClientPlugin;

public class ASTME1381TransmissionModeClientPlugin extends TransmissionModeClientPlugin {

    public ASTME1381TransmissionModeClientPlugin(String name) {
        super(name);
    }

    @Override
    public AbstractSettingsPanel getSettingsPanel() {
        return new ASTME1381TransmissionModeSettingsPanel("ASTM E1381");
    }

    @Override
    public String getPluginPointName() {
        return ASTME1381Constants.PLUGIN_NAME;
    }

    @Override
    public void start() {}

    @Override
    public void stop() {}

    @Override
    public void reset() {}
}
