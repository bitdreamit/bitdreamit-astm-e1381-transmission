package com.bitdreamit.connect.plugins.transmission.astm.client;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.GroupLayout;

/**
 * Small settings panel with a settings button.
 *
 * <p>Mirrors Mirth's MLLPModeSettingsPanel: a small panel containing
 * just a settings button. When the user clicks the button, the
 * {@link ASTME1381SettingsDialog} modal dialog opens.</p>
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
        setBackground(new Color(0xF0, 0xF0, 0xF0));

        settingsButton = new JButton();
        settingsButton.setText("Frame Settings");
        settingsButton.setMargin(new Insets(2, 6, 2, 6));
        settingsButton.setFocusable(false);
        settingsButton.setPreferredSize(new Dimension(100, 22));
        settingsButton.setMaximumSize(new Dimension(120, 22));
        settingsButton.setMinimumSize(new Dimension(80, 22));
        settingsButton.setContentAreaFilled(false);
        settingsButton.setBorderPainted(false);
        settingsButton.setOpaque(false);

        // Use GroupLayout to match Mirth's MLLP layout style
        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(settingsButton)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(settingsButton)
                .addContainerGap())
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
