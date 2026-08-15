package com.bitdreamit.connect.plugins.transmission.astm.client;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Constants;
import com.mirth.connect.client.ui.AbstractSettingsPanel;
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
 * <p>Methods inherited from {@code TransmissionModePlugin} (real overrides):
 * <ul>
 *   <li>{@code createProvider()} - abstract, must override (returns our
 *       {@link ASTME1381ClientProvider})</li>
 *   <li>{@code start()} - default empty impl, may override</li>
 *   <li>{@code stop()} - default empty impl, may override</li>
 *   <li>{@code reset()} - default empty impl, may override</li>
 * </ul></p>
 *
 * <p>Methods below that are NOT declared on {@code TransmissionModePlugin}
 * directly (and therefore not annotated with {@code @Override}):
 * <ul>
 *   <li>{@code getSettingsPanel()} - declared on {@code ClientPlugin}
 *       in some Mirth versions; kept as a plain method so the build
 *       does not break if the parent class layout changes.</li>
 *   <li>{@code getPluginPointName()} - declared on {@code Plugin}
 *       in some Mirth versions; same treatment.</li>
 * </ul></p>
 */
public class ASTME1381TransmissionModeClientPlugin extends TransmissionModePlugin {

    public ASTME1381TransmissionModeClientPlugin(String name) {
        super(name);
    }

    /**
     * Factory method required by {@code TransmissionModePlugin}.
     *
     * <p>Returns a new {@link ASTME1381ClientProvider} instance. The
     * returned provider implements the actual send-side ASTM E1381
     * flow (ENQ -> ACK -> frames -> EOT).</p>
     */
    @Override
    public TransmissionModeClientProvider createProvider() {
        return new ASTME1381ClientProvider();
    }

    /**
     * Returns the settings panel that the Mirth channel editor will
     * display when the user picks "ASTM E1381" as the transmission mode.
     *
     * <p>Not annotated {@code @Override} - the declaring class
     * ({@code ClientPlugin}) is not guaranteed to expose this method in
     * every Mirth 4.x point release.</p>
     */
    public AbstractSettingsPanel getSettingsPanel() {
        return new ASTME1381TransmissionModeSettingsPanel("ASTM E1381");
    }

    /**
     * Returns the human-readable name of this transmission mode, shown
     * in the Mirth channel editor's transmission mode dropdown.
     *
     * <p>Not annotated {@code @Override} - same reason as
     * {@link #getSettingsPanel()}.</p>
     */
    public String getPluginPointName() {
        return ASTME1381Constants.PLUGIN_NAME;
    }
}
