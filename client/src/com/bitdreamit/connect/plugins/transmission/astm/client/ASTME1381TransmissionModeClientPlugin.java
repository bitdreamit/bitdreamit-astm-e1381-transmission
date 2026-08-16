package com.bitdreamit.connect.plugins.transmission.astm.client;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Constants;
import com.mirth.connect.plugins.TransmissionModeClientProvider;
import com.mirth.connect.plugins.TransmissionModePlugin;

/**
 * Client-side transmission-mode plugin registration for ASTM E1381-02.
 *
 * <p><b>API note (Mirth Connect 4.5+):</b> the client-side abstract base
 * class is {@code com.mirth.connect.plugins.TransmissionModePlugin}
 * (NOT {@code TransmissionModeClientPlugin}, which does NOT exist in
 * this Mirth version despite the symmetric-looking name).
 * {@code TransmissionModePlugin} extends {@code ClientPlugin} and
 * declares the single abstract method {@code createProvider()}, which
 * returns a {@link TransmissionModeClientProvider} instance.</p>
 *
 * <p>The original v1.0.x / v1.1.x plugin code mistakenly imported
 * {@code TransmissionModeClientPlugin} (which does not exist), causing
 * the "cannot find symbol: class TransmissionModeClientPlugin" compile
 * error. The fix is to import and extend {@code TransmissionModePlugin}
 * instead - despite the confusing name, that is the client-side class.</p>
 *
 * <p><b>Settings panel note (v1.2.4):</b> the previous version had a
 * {@code getSettingsPanel()} method that returned an
 * {@code AbstractSettingsPanel}. This method was NOT called by Mirth's
 * channel editor (Mirth calls {@code getSettingsComponent()} on the
 * {@link ASTME1381ClientProvider} instead), and it created an unnecessary
 * dependency on {@code AbstractSettingsPanel} which caused the settings
 * panel construction to fail silently. The method has been removed.</p>
 */
public class ASTME1381TransmissionModeClientPlugin extends TransmissionModePlugin {

    public ASTME1381TransmissionModeClientPlugin(String name) {
        super(name);
    }

    /**
     * Factory method required by {@code TransmissionModePlugin}.
     *
     * <p>Returns a new {@link ASTME1381ClientProvider} instance. The
     * returned provider implements the channel editor UI hooks
     * (sample messages, property validation, settings component).</p>
     */
    @Override
    public TransmissionModeClientProvider createProvider() {
        return new ASTME1381ClientProvider();
    }

    /**
     * Returns the human-readable name of this transmission mode, shown
     * in the Mirth channel editor's transmission mode dropdown.
     *
     * <p>Not annotated {@code @Override} - the declaring class
     * ({@code ClientPlugin} / {@code Plugin}) is not guaranteed to
     * expose this method in every Mirth 4.x point release.</p>
     */
    public String getPluginPointName() {
        return ASTME1381Constants.PLUGIN_NAME;
    }
}
