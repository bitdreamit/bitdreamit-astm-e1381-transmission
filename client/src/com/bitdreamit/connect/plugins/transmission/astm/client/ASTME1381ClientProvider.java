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
 * <p>Mirrors Mirth's MLLPModeClientProvider: returns a small
 * {@link ASTME1381SettingsPanel} (with a settings button) from
 * {@link #getSettingsComponent()}. When the user clicks the button,
 * the {@link ASTME1381SettingsDialog} modal dialog opens.</p>
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
    }

    /**
     * Returns a small panel with a "Frame Settings" button.
     * Clicking the button opens the ASTME1381SettingsDialog modal.
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

    private void openSettingsDialog() {
        // Find the parent frame by walking up the component hierarchy
        Frame frame = parentFrame;
        if (frame == null) {
            Component c = settingsPanel;
            while (c != null && !(c instanceof Frame)) {
                c = c.getParent();
            }
            if (c instanceof Frame) {
                frame = (Frame) c;
            }
        }

        ASTME1381SettingsDialog dialog = new ASTME1381SettingsDialog(frame);
        dialog.setVisible(true);

        if (dialog.isOkPressed()) {
            // User clicked OK - update properties from dialog
            ensureProps();
            try { props.setEnquiryByte(parseHex(dialog.getEnquiryByte())); } catch (Exception ignored) {}
            try { props.setStartOfFrameByte(parseHex(dialog.getStartOfFrameByte())); } catch (Exception ignored) {}
            try { props.setMaxFrameContentLength(Integer.parseInt(dialog.getMaxFrameContentLength())); } catch (Exception ignored) {}
            try { props.setIntermediateEndOfFrame(parseHex(dialog.getIntermediateEndOfFrame())); } catch (Exception ignored) {}
            try { props.setEndOfFrameByte(parseHex(dialog.getEndOfFrameByte())); } catch (Exception ignored) {}
            try { props.setChecksumByteLength(Integer.parseInt(dialog.getChecksumByteLength())); } catch (Exception ignored) {}
            try { props.setEndOfTransmissionByte(parseHex(dialog.getEndOfTransmissionByte())); } catch (Exception ignored) {}
            try { props.setPositiveAckByte(parseHex(dialog.getPositiveAckByte())); } catch (Exception ignored) {}
            try { props.setNegativeAckByte(parseHex(dialog.getNegativeAckByte())); } catch (Exception ignored) {}
            try { props.setMaxTransferAttempts(Integer.parseInt(dialog.getMaxTransferAttempts())); } catch (Exception ignored) {}
            try { props.setEstablishmentTimeout(Integer.parseInt(dialog.getEstablishmentTimeout())); } catch (Exception ignored) {}
            try { props.setContentionTimeout(Integer.parseInt(dialog.getContentionTimeout())); } catch (Exception ignored) {}
            try { props.setFrameTimeout(Integer.parseInt(dialog.getFrameTimeout())); } catch (Exception ignored) {}
            try { props.setResponseTimeout(Integer.parseInt(dialog.getResponseTimeout())); } catch (Exception ignored) {}
            props.setValidateFrameNumber(dialog.isValidateFrameNumber());
            props.setIgnoreServerSideCancel(dialog.isIgnoreServerSideCancel());
            props.setUseChecksum(dialog.isUseChecksum());
            props.setUseStrictValidation(dialog.isUseStrictValidation());
            props.setChecksumAlgorithm(dialog.getChecksumAlgorithm());
            props.setBidirectional(dialog.isBidirectional());
            props.setServerMode(dialog.isServerMode());
        }
    }

    private int parseHex(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        s = s.trim().replace("0x", "").replace("0X", "");
        return Integer.parseInt(s, 16);
    }

    private void ensureProps() {
        if (props == null) {
            props = new ASTME1381TransmissionModeProperties();
        }
    }
}
