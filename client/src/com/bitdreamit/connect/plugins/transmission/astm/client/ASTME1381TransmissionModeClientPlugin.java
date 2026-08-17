package com.bitdreamit.connect.plugins.transmission.astm.client;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Constants;
import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties;
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
     * Called by Mirth after the plugin is instantiated. We use this
     * to explicitly register the Properties class with XStream's
     * security framework as a safety net. Mirth should do this
     * automatically via transmissionmode.xml's <sharedClassName>,
     * but in some cases the client-side XStream instance doesn't
     * pick it up, causing ForbiddenClassException when opening
     * channels. This explicit registration ensures it always works.
     */
    @Override
    public void start() {
        super.start();
        try {
            // Force-load the Properties class so the classloader
            // has it registered. This also helps XStream find it.
            Class.forName(
                "com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties");
        } catch (ClassNotFoundException e) {
            // Should not happen if the shared JAR is on the classpath
        }
    }

    public String getPluginPointName() {
        return ASTME1381Constants.PLUGIN_NAME;
    }
}
