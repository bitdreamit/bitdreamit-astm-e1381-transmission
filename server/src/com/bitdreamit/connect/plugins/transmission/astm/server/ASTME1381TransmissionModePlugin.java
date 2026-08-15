package com.bitdreamit.connect.plugins.transmission.astm.server;

import java.io.InputStream;
import java.io.OutputStream;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Constants;
import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties;
import com.mirth.connect.donkey.server.message.StreamHandler;
import com.mirth.connect.donkey.server.message.batch.BatchStreamReader;
import com.mirth.connect.model.transmission.TransmissionModeProperties;
import com.mirth.connect.plugins.TransmissionModeProvider;

/**
 * ASTM E1381-02 Transmission Mode Server Plugin.
 *
 * <p>Registers the ASTM E1381 framing protocol with Mirth Connect's TCP/Serial
 * connectors by providing a {@link ASTME1381StreamHandler} factory.</p>
 *
 * <p><b>API note (Mirth Connect 4.5+):</b> the server-side abstract base class
 * is {@code com.mirth.connect.plugins.TransmissionModeProvider} (it
 * {@code implements ServerPlugin} and declares the single abstract method
 * {@code getStreamHandler(InputStream, OutputStream, BatchStreamReader,
 * TransmissionModeProperties)}). Do NOT confuse it with
 * {@code com.mirth.connect.plugins.TransmissionModePlugin}, which is the
 * <i>client-side</i> abstract class (it extends {@code ClientPlugin} and
 * declares {@code createProvider()}). The two have similar names but very
 * different responsibilities - the original plugin code (v1.0.x / v1.1.x)
 * mixed them up, hence the historical
 * "cannot find symbol: class TransmissionModePlugin" compile errors on the
 * server module.</p>
 *
 * <p>Methods inherited from {@code TransmissionModeProvider} (real overrides):
 * <ul>
 *   <li>{@code getStreamHandler(...)} - abstract, must override</li>
 *   <li>{@code start()} - default empty impl, may override</li>
 *   <li>{@code stop()} - default empty impl, may override</li>
 * </ul></p>
 *
 * <p>Methods below that are NOT declared on {@code TransmissionModeProvider}
 * (and therefore not annotated with {@code @Override}):
 * <ul>
 *   <li>{@code getPluginPointName()} - declared on {@code ServerPlugin} /
 *       the broader Plugin interface in some Mirth versions, but not always.
 *       Kept as a plain method so the build does not break if the parent
 *       interface changes.</li>
 *   <li>{@code getPluginPointDescription()} - same situation.</li>
 *   <li>{@code getDefaultProperties()} - utility method used by Mirth's
 *       channel editor when creating a new transmission mode instance;
 *       declared on the {@code ServerPlugin} subtype that exposes it
 *       (not always present). Kept as a plain method.</li>
 * </ul></p>
 */
public class ASTME1381TransmissionModePlugin extends TransmissionModeProvider {

    /**
     * Returns the human-readable name of this transmission mode plugin
     * ("ASTM E1381"). Used by the Mirth channel editor when labelling
     * the transmission mode dropdown.
     *
     * <p>Not annotated {@code @Override} because the declaring interface
     * ({@code ServerPlugin} / {@code Plugin}) is not guaranteed to expose
     * this method in every Mirth Connect 4.x point release - removing
     * the annotation keeps the build tolerant of minor API drift.</p>
     */
    public String getPluginPointName() {
        return ASTME1381Constants.PLUGIN_NAME;
    }

    /**
     * Returns a short description of this transmission mode plugin,
     * shown in the Mirth channel editor.
     *
     * <p>Same situation as {@link #getPluginPointName()} - kept as a
     * plain method without {@code @Override}.</p>
     */
    public String getPluginPointDescription() {
        return "ASTM E1381-02 Lower Layer Protocol with frame sequencing, "
             + "LRC validation, and bidirectional handshaking.";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns a new {@link ASTME1381StreamHandler} bound to the given
     * streams and configured with the supplied
     * {@link ASTME1381TransmissionModeProperties}. Called once per
     * message by Mirth's TCP/Serial connector framework.</p>
     */
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

    /**
     * Returns a fresh properties bean populated with the ASTM E1381-02
     * spec defaults. Used by Mirth's channel editor when the user creates
     * a new ASTM E1381 transmission mode instance.
     *
     * <p>Not annotated {@code @Override} - the declaring interface is not
     * guaranteed to expose this method in every Mirth 4.x point release.</p>
     */
    public TransmissionModeProperties getDefaultProperties() {
        return new ASTME1381TransmissionModeProperties();
    }
}
