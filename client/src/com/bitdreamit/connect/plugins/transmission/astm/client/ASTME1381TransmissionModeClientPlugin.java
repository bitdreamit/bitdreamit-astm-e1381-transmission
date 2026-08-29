package com.bitdreamit.connect.plugins.transmission.astm.client;

import java.util.Collections;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Constants;
import com.mirth.connect.model.converters.ObjectXMLSerializer;
import com.mirth.connect.plugins.TransmissionModeClientProvider;
import com.mirth.connect.plugins.TransmissionModePlugin;

/**
 * Client-side transmission-mode plugin for ASTM E1381-02.
 *
 * <p>Extends Mirth's TransmissionModePlugin (the same base class Mirth's
 * built-in MLLPModePlugin uses). Mirth's client-side extension loader
 * (com.mirth.connect.client.ui.LoadedExtensions#initialize, called from
 * Frame#setupFrame) instantiates this class via the &lt;clientClasses&gt;
 * entry in plugin.xml.</p>
 *
 * <h2>XStream security registration (fixes ForbiddenClassException)</h2>
 *
 * <p>Mirth Connect (server AND Administrator) runs XStream with the
 * security framework enabled. The allow-list is hardcoded in
 * {@code com.mirth.connect.donkey.util.xstream.XStreamSerializer}'s
 * constructor and only covers {@code com.mirth.connect.**}-style package
 * prefixes (com.mirth.connect.model.**, com.mirth.connect.plugins.**, ...).
 * Classes from third-party packages that end up embedded in serialized
 * channel XML - such as
 * {@code com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties}
 * - are rejected on DESERIALIZATION with
 * {@code com.thoughtworks.xstream.security.ForbiddenClassException}.</p>
 *
 * <p>Serialization is NOT security-checked (which is why the server can
 * store and re-serve the channel), but every deserialization path in the
 * Administrator (channel list, channel groups, channel edit) goes through
 * {@code ObjectXMLSerializer} and needs the class to be allow-listed.
 * Unlike the server, the Administrator has no mirth.properties /
 * xstream.allowtypes mechanism, so the plugin must register the permission
 * itself.</p>
 *
 * <p>Mirth's own TCP connector client plugin (com.mirth.connect.connectors.tcp.TcpClientPlugin)
 * sets the precedent of configuring ObjectXMLSerializer from the plugin
 * constructor: LoadedExtensions.initialize() runs during Frame.setupFrame,
 * i.e. BEFORE the Channels panel triggers the first getChannelSummary
 * deserialization.</p>
 *
 * <p><b>Important:</b> use the 3-argument
 * {@link ObjectXMLSerializer#allowTypes(List, List, List)} method. Internal
 * deserialization runs on a SECOND, private "references-mode" XStream
 * instance (ObjectXMLSerializer#getXStreamWithReferences), and only the
 * 3-arg method updates both instances. Calling
 * {@code getXStream().allowTypesByWildcard(...)} directly would patch only
 * the primary instance and the error would persist.</p>
 *
 * <p>Note: LoadedExtensions skips plugins of DISABLED extensions. Keep the
 * extension enabled. Also note that Frame.setupFrame calls
 * channelPanel.retrieveGroups() BEFORE extensions are initialized; if a
 * channel using this transmission mode is placed inside a channel GROUP,
 * the Administrator will log one group-load error at startup (the channel
 * list itself recovers once this constructor has run).</p>
 */
public class ASTME1381TransmissionModeClientPlugin extends TransmissionModePlugin {

    public ASTME1381TransmissionModeClientPlugin(String name) {
        super(name);

        // Allow-list this extension's package for the Administrator's XStream
        // security framework (entries containing '*' or '?' are treated as
        // wildcards by XStream's allowTypesByWildcard).
        ObjectXMLSerializer.getInstance().allowTypes(
                null,
                Collections.singletonList("com.bitdreamit.connect.plugins.transmission.astm.**"),
                null);
    }

    @Override
    public TransmissionModeClientProvider createProvider() {
        return new ASTME1381ClientProvider();
    }

    @Override
    public String getPluginPointName() {
        return ASTME1381Constants.PLUGIN_NAME;
    }

    // NOTE: no @Override - getPluginPointDescription() is NOT declared as abstract
    // in the real Mirth 4.5.2 TransmissionModePlugin base class, so @Override fails.
    public String getPluginPointDescription() {
        return "ASTM E1381-02 lower-layer transmission framing protocol (client side). "
                + "Provides the settings panel for configuring ENQ/ACK/NAK bytes, STX/ETB/ETX "
                + "framing, checksum algorithm, frame sequencing, and per-phase timeouts.";
    }
}
