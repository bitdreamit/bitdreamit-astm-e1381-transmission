package com.bitdreamit.connect.plugins.transmission.astm.client;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.GroupLayout;

/**
 * Small settings panel with a wrench icon button.
 *
 * <p>Mirrors Mirth's MLLPModeSettingsPanel: a small panel containing
 * just a wrench icon button (no text). When the user clicks the button,
 * the {@link ASTME1381SettingsDialog} modal dialog opens.</p>
 *
 * <p>The icon used is the same wrench.png that Mirth's MLLP plugin uses:
 * {@code com/mirth/connect/client/ui/images/wrench.png}.</p>
 *
 * <p>Returned by {@code getSettingsComponent()} on
 * {@link ASTME1381ClientProvider}. Mirth displays this panel inline
 * in the channel editor next to the "Transmission Mode" dropdown.</p>
 */
public class ASTME1381SettingsPanel extends JPanel {

    private JButton settingsButton;
    private ActionListener actionListener;

    public ASTME1381SettingsPanel() {
        initComponents();
    }

    private void initComponents() {
        // Load the wrench icon from Mirth's built-in images
        // (same icon used by MLLPModeSettingsPanel)
        ImageIcon wrenchIcon = null;
        try {
            java.net.URL iconUrl = getClass().getClassLoader()
                .getResource("com/mirth/connect/client/ui/images/wrench.png");
            if (iconUrl != null) {
                wrenchIcon = new ImageIcon(iconUrl);
            }
        } catch (Exception e) {
            // Icon not found - will use text fallback
        }

        settingsButton = new JButton();
        if (wrenchIcon != null) {
            settingsButton.setIcon(wrenchIcon);
        } else {
            settingsButton.setText("Frame Settings");
        }

        // Match Mirth's MLLP button style: small, no border, no fill,
        // just the icon
        settingsButton.setMargin(new Insets(0, 0, 0, 0));
        settingsButton.setFocusable(false);
        settingsButton.setBorderPainted(false);
        settingsButton.setContentAreaFilled(false);
        settingsButton.setOpaque(false);
        settingsButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        // Use GroupLayout to center the button in the panel
        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.CENTER)
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(settingsButton))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.CENTER)
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(settingsButton))
        );

        settingsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (actionListener != null) {
                    actionListener.actionPerformed(e);
                }
            }
        });
    }

    public void setActionListener(ActionListener listener) {
        this.actionListener = listener;
    }

    public JButton getSettingsButton() {
        return settingsButton;
    }
}
