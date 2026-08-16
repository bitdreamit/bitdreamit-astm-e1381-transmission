package com.bitdreamit.connect.plugins.transmission.astm.client;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

/**
 * ASTM E1381-02 Transmission Mode Settings Panel.
 *
 * <p>This panel is returned by
 * {@code ASTME1381ClientProvider.getSettingsComponent()} and is displayed
 * inside Mirth's "Transmission Mode Settings" modal dialog when the user
 * clicks the "Frame Settings" link in the channel editor.</p>
 *
 * <p><b>Design notes (v1.2.4):</b></p>
 * <ul>
 *   <li>Extends {@link JPanel} directly (NOT {@code AbstractSettingsPanel}).
 *       The previous version extended {@code AbstractSettingsPanel}, which
 *       is designed for Mirth's Settings -> Extensions global view, not for
 *       the channel editor's inline modal dialog. When the channel editor
 *       tried to instantiate the panel, the {@code AbstractSettingsPanel}
 *       constructor failed silently, causing Mirth to disable the
 *       "Frame Settings" link.</li>
 *   <li>Uses standard Swing components ({@link JTextField},
 *       {@link JCheckBox}, {@link JComboBox}) instead of Mirth's custom
 *       {@code MirthTextField} / {@code MirthCheckBox} / {@code MirthComboBox}.
 *       The Mirth custom components may have different constructor
 *       signatures in different Mirth versions; using standard Swing
 *       components eliminates this risk entirely.</li>
 *   <li>Uses {@link GridBagLayout} instead of {@code MigLayout}. This
 *       removes the dependency on {@code miglayout-swing-4.2.jar} and
 *       {@code miglayout-core-4.2.jar} at runtime, which simplifies
 *       deployment. GridBagLayout is part of the JDK and always available.</li>
 * </ul>
 */
public class ASTME1381TransmissionModeSettingsPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    // --- Frame Settings fields ---
    private JTextField enquiryField;
    private JTextField stxField;
    private JTextField maxContentLengthField;
    private JTextField etbField;
    private JTextField etxField;
    private JTextField checksumLengthField;
    private JTextField frameTerminatorField;
    private JTextField eotField;

    // --- Validation Settings fields ---
    private JCheckBox validateFrameNumberBox;
    private JCheckBox ignoreServerCancelBox;
    private JCheckBox useChecksumBox;
    private JCheckBox strictValidationBox;
    private JComboBox<String> checksumAlgorithmBox;
    private JCheckBox bidirectionalBox;
    private JTextField ackField;
    private JTextField nakField;

    // --- Connection Settings fields ---
    private JTextField maxTransferAttemptsField;
    private JTextField establishmentTimeoutField;
    private JTextField contentionTimeoutField;
    private JTextField frameTimeoutField;
    private JTextField responseTimeoutField;

    // --- Mode field ---
    private JCheckBox serverModeBox;

    public ASTME1381TransmissionModeSettingsPanel() {
        initComponents();
        initLayout();
        loadDefaults();
    }

    public ASTME1381TransmissionModeSettingsPanel(String tabName) {
        this();
        // tabName is accepted for backwards compatibility with the old
        // AbstractSettingsPanel-based constructor, but is not used since
        // we now extend JPanel directly.
    }

    // ------------------------------------------------------------------
    // Component initialization
    // ------------------------------------------------------------------

    private void initComponents() {
        enquiryField              = new JTextField(10);
        stxField                  = new JTextField(10);
        maxContentLengthField     = new JTextField(10);
        etbField                  = new JTextField(10);
        etxField                  = new JTextField(10);
        checksumLengthField       = new JTextField(10);
        frameTerminatorField      = new JTextField(12);
        eotField                  = new JTextField(10);

        validateFrameNumberBox    = new JCheckBox();
        ignoreServerCancelBox     = new JCheckBox();
        useChecksumBox            = new JCheckBox();
        strictValidationBox       = new JCheckBox();
        checksumAlgorithmBox      = new JComboBox<>(
            new String[]{"Add Mod 256", "XOR", "None"});
        bidirectionalBox          = new JCheckBox();
        ackField                  = new JTextField(10);
        nakField                  = new JTextField(10);

        maxTransferAttemptsField  = new JTextField(10);
        establishmentTimeoutField = new JTextField(10);
        contentionTimeoutField    = new JTextField(10);
        frameTimeoutField         = new JTextField(10);
        responseTimeoutField      = new JTextField(10);

        serverModeBox             = new JCheckBox();
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private void initLayout() {
        setBackground(Color.WHITE);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        // --- Frame Settings Panel ---
        add(createFrameSettingsPanel(), gbc);
        gbc.gridy++;

        // --- Validation Settings Panel ---
        add(createValidationSettingsPanel(), gbc);
        gbc.gridy++;

        // --- Connection Settings Panel ---
        add(createConnectionSettingsPanel(), gbc);
        gbc.gridy++;

        // --- Mode Panel ---
        add(createModePanel(), gbc);
    }

    private JPanel createFrameSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(createTitledBorder("Frame Settings"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 4, 3, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.0;
        gbc.gridx = 0;

        String[][] fields = {
            {"Enquiry (ENQ):", "0x05"},
            {"Start of Frame (STX):", "0x02"},
            {"Max Content Length:", "240"},
            {"Intermediate End (ETB):", "0x17"},
            {"End of Frame (ETX):", "0x03"},
            {"Checksum Byte Length:", "2"},
            {"Frame Terminator:", "0x0D0A"},
            {"End of Transmission (EOT):", "0x04"},
        };

        JTextField[] fieldRefs = {
            enquiryField, stxField, maxContentLengthField, etbField,
            etxField, checksumLengthField, frameTerminatorField, eotField
        };

        for (int i = 0; i < fields.length; i++) {
            gbc.gridy = i;
            gbc.gridx = 0;
            gbc.weightx = 0.0;
            gbc.anchor = GridBagConstraints.EAST;
            panel.add(new JLabel(fields[i][0]), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.anchor = GridBagConstraints.WEST;
            fieldRefs[i].setText(fields[i][1]);
            panel.add(fieldRefs[i], gbc);
        }

        return panel;
    }

    private JPanel createValidationSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(createTitledBorder("Validation Settings"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 4, 3, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Validate Frame Number
        addRow(panel, gbc, 0, "Validate Frame Number:", validateFrameNumberBox, true);
        // Row 1: Ignore Server-Side Cancel
        addRow(panel, gbc, 1, "Ignore Server-Side Cancel:", ignoreServerCancelBox, false);
        // Row 2: Use Checksum
        addRow(panel, gbc, 2, "Use Checksum:", useChecksumBox, true);
        // Row 3: Use Strict Validation
        addRow(panel, gbc, 3, "Use Strict Validation:", strictValidationBox, false);
        // Row 4: Checksum Algorithm
        addRow(panel, gbc, 4, "Checksum Algorithm:", checksumAlgorithmBox, null);
        // Row 5: Bidirectional
        addRow(panel, gbc, 5, "Bidirectional:", bidirectionalBox, true);
        // Row 6: Positive ACK
        addTextRow(panel, gbc, 6, "Positive ACK:", ackField, "0x06");
        // Row 7: Negative ACK
        addTextRow(panel, gbc, 7, "Negative ACK:", nakField, "0x15");

        return panel;
    }

    private JPanel createConnectionSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(createTitledBorder("Connection Settings"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 4, 3, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addTextRow(panel, gbc, 0, "Max Transfer Attempts:", maxTransferAttemptsField, "6");
        addTextRow(panel, gbc, 1, "Establishment Timeout (ms):", establishmentTimeoutField, "15000");
        addTextRow(panel, gbc, 2, "Contention Timeout (ms):", contentionTimeoutField, "20000");
        addTextRow(panel, gbc, 3, "Frame Timeout (ms):", frameTimeoutField, "30000");
        addTextRow(panel, gbc, 4, "Response Timeout (ms):", responseTimeoutField, "15000");

        return panel;
    }

    private JPanel createModePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(createTitledBorder("Mode"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 4, 3, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel("Server Mode:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        serverModeBox.setSelected(true);
        panel.add(serverModeBox, gbc);

        return panel;
    }

    // ------------------------------------------------------------------
    // Layout helpers
    // ------------------------------------------------------------------

    private TitledBorder createTitledBorder(String title) {
        return BorderFactory.createTitledBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(204, 204, 204)),
            title,
            TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION,
            new java.awt.Font("Tahoma", java.awt.Font.BOLD, 11));
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row,
                        String label, JCheckBox checkbox, Boolean selected) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        if (selected != null) {
            checkbox.setSelected(selected);
        }
        panel.add(checkbox, gbc);
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row,
                        String label, JComboBox<?> combo, Object ignored) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(combo, gbc);
    }

    private void addTextRow(JPanel panel, GridBagConstraints gbc, int row,
                             String label, JTextField field, String defaultValue) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        field.setText(defaultValue);
        panel.add(field, gbc);
    }

    // ------------------------------------------------------------------
    // Default values
    // ------------------------------------------------------------------

    private void loadDefaults() {
        // All defaults are set during initLayout() via the addTextRow()
        // and addRow() helpers. This method is a placeholder for any
        // future preference-loading logic.
    }
}
