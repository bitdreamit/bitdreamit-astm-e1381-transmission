package com.bitdreamit.mirth.astm.e1381.client;

import com.bitdreamit.mirth.astm.e1381.shared.ASTME1381Constants;
import com.mirth.connect.client.ui.AbstractSettingsPanel;
import com.mirth.connect.plugins.ClientPlugin;

public class ASTME1381ClientPlugin extends ClientPlugin {

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

    @Override
    public AbstractSettingsPanel getSettingsPanel() {
        return new ASTME1381TransmissionModeSettingsPanel("ASTM E1381 Settings");
    }

    @Override
    public String getSettingsPanelName() {
        return "ASTM E1381";
    }
}
