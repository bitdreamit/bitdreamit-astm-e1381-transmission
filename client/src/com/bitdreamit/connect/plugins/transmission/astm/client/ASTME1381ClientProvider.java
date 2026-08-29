package com.bitdreamit.connect.plugins.transmission.astm.client;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Constants;
import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties;
import com.mirth.connect.model.transmission.TransmissionModeProperties;
import com.mirth.connect.plugins.TransmissionModeClientProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Client-side provider for ASTM E1381-02.
 *
 * <p>Mirrors Mirth's {@code MLLPModeClientProvider}: returns a small
 * {@link ASTME1381SettingsPanel} (a wrench-icon button) from
 * {@link #getSettingsComponent()}. When the user clicks the button,
 * the {@link ASTME1381SettingsDialog} modal dialog opens.</p>
 *
 * <p><b>Save wiring (v1.3.3):</b> The dialog takes the provider's
 * current {@code props} reference in its constructor, loads every field
 * from it on open, and writes the field values back into the SAME
 * {@code props} object on OK. Because Mirth holds the same reference
 * (it called {@link #setProperties(TransmissionModeProperties)} earlier),
 * Mirth's channel-serialization picks up the changes automatically - no
 * property-change event is needed. This was the root cause of the
 * "Save button doesn't save" bug in v1.3.2.</p>
 */
public class ASTME1381ClientProvider extends TransmissionModeClientProvider {

    private ASTME1381TransmissionModeProperties props;
    private ASTME1381SettingsPanel settingsPanel;
    private Frame parentFrame;

    @Override
    public String getSampleLabel() {
        return "ASTM E1381 Sample";
    }

    @Override
    public String getSampleValue() {
        return "H|\\^&|||ASTM|||||P|1\r"
             + "P|1|||Patient^Test||||||||||||||\r"
             + "O|1|SAMPLE01||ALL||||||||O|||||||\r"
             + "R|1|^^GLU^GLUCOSE|180|mg/dL|70-105|N|||2024\r"
             + "L|1|N\r";
    }

    @Override
    public TransmissionModeProperties getProperties() {
        ensureProps();
        return props;
    }

    @Override
    public TransmissionModeProperties getDefaultProperties() {
        return new ASTME1381TransmissionModeProperties();
    }

    @Override
    public void setProperties(TransmissionModeProperties properties) {
        if (properties == null) {
            this.props = new ASTME1381TransmissionModeProperties();
        } else if (!(properties instanceof ASTME1381TransmissionModeProperties)) {
            throw new IllegalArgumentException(
                "Expected ASTME1381TransmissionModeProperties but got: "
                + properties.getClass().getName());
        } else {
            this.props = (ASTME1381TransmissionModeProperties) properties;
        }
    }

    @Override
    public boolean checkProperties(TransmissionModeProperties properties, boolean highlight) {
        if (properties == null) return false;
        if (!(properties instanceof ASTME1381TransmissionModeProperties)) return false;
        ASTME1381TransmissionModeProperties p = (ASTME1381TransmissionModeProperties) properties;
        if (p.getMaxFrameContentLength() <= 0) return false;
        if (p.getEstablishmentTimeout() <= 0) return false;
        if (p.getFrameTimeout() <= 0) return false;
        if (p.getResponseTimeout() <= 0) return false;
        if (p.getMaxTransferAttempts() <= 0) return false;
        if (p.getChecksumByteLength() < 1 || p.getChecksumByteLength() > 2) return false;
        return true;
    }

    @Override
    public void resetInvalidProperties() {
        ensureProps();
        if (props.getMaxFrameContentLength() <= 0)
            props.setMaxFrameContentLength(ASTME1381Constants.DEFAULT_MAX_FRAME_CONTENT_LENGTH);
        if (props.getEstablishmentTimeout() <= 0)
            props.setEstablishmentTimeout(ASTME1381Constants.DEFAULT_ESTABLISHMENT_TIMEOUT);
        if (props.getFrameTimeout() <= 0)
            props.setFrameTimeout(ASTME1381Constants.DEFAULT_FRAME_TIMEOUT);
        if (props.getResponseTimeout() <= 0)
            props.setResponseTimeout(ASTME1381Constants.DEFAULT_RESPONSE_TIMEOUT);
        if (props.getMaxTransferAttempts() <= 0)
            props.setMaxTransferAttempts(ASTME1381Constants.DEFAULT_MAX_TRANSFER_ATTEMPTS);
        if (props.getChecksumByteLength() < 1 || props.getChecksumByteLength() > 2)
            props.setChecksumByteLength(ASTME1381Constants.DEFAULT_CHECKSUM_BYTE_LENGTH);
    }

    /**
     * Returns a small panel with a "Frame Settings" (wrench icon) button.
     * Clicking the button opens the {@link ASTME1381SettingsDialog} modal.
     *
     * <p>The panel is created lazily ONCE per provider instance - Mirth
     * may call this method many times during the channel editor's
     * lifetime, and we want the same panel reference each time so its
     * event listeners don't accumulate.</p>
     */
    @Override
    public JComponent getSettingsComponent() {
        if (settingsPanel == null) {
            settingsPanel = new ASTME1381SettingsPanel();
            settingsPanel.setActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    openSettingsDialog();
                }
            });
        }
        return settingsPanel;
    }

    /**
     * Open the settings dialog. Loads the current props into the dialog,
     * shows it modally, and on OK the dialog has already written the
     * field values back into the SAME props reference (so Mirth's
     * channel-serialization picks up the changes automatically).
     */
    private void openSettingsDialog() {
        ensureProps();

        // Find the parent frame by walking up the component hierarchy
        Frame frame = parentFrame;
        if (frame == null && settingsPanel != null) {
            Component c = settingsPanel;
            while (c != null && !(c instanceof Frame)) {
                c = c.getParent();
            }
            if (c instanceof Frame) {
                frame = (Frame) c;
            }
        }

        // Construct the dialog with the CURRENT props reference.
        // The dialog loads from props on open, writes back to props on OK.
        ASTME1381SettingsDialog dialog = new ASTME1381SettingsDialog(frame, props);
        dialog.setVisible(true);

        // No post-processing needed - the dialog already mutated props in place.
        // Mirth will see the changes the next time it serializes the channel.
        if (dialog.isOkPressed()) {
            // Optional: trigger a re-validation of the props
            checkProperties(props, false);
        }
    }

    /**
     * Set the parent frame explicitly (used by Mirth's UI when embedding
     * the settings panel in the channel editor).
     */
    public void setParentFrame(Frame parent) {
        this.parentFrame = parent;
    }

    private void ensureProps() {
        if (props == null) {
            props = new ASTME1381TransmissionModeProperties();
        }
    }
}
