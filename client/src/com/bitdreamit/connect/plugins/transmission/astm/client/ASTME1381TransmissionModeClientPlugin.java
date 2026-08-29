package com.bitdreamit.connect.plugins.transmission.astm.client;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Constants;
import com.mirth.connect.plugins.TransmissionModeClientProvider;
import com.mirth.connect.plugins.TransmissionModePlugin;

/**
 * Client-side transmission-mode plugin for ASTM E1381-02.
 *
 * Extends Mirth's TransmissionModePlugin (the same base class Mirth's
 * built-in MLLPModePlugin uses). Mirth's client-side extension loader
 * instantiates this class via the <clientClasses> entry in plugin.xml.
 */
public class ASTME1381TransmissionModeClientPlugin extends TransmissionModePlugin {

    public ASTME1381TransmissionModeClientPlugin(String name) {
        super(name);
    }

    @Override
    public TransmissionModeClientProvider createProvider() {
        return new ASTME1381ClientProvider();
    }

    @Override
    public String getPluginPointName() {
        return ASTME1381Constants.PLUGIN_NAME;
    }

    @Override
    public String getPluginPointDescription() {
        return "ASTM E1381-02 lower-layer transmission framing protocol (client side). "
             + "Provides the settings panel for configuring ENQ/ACK/NAK bytes, STX/ETB/ETX "
             + "framing, checksum algorithm, frame sequencing, and per-phase timeouts.";
    }
}

