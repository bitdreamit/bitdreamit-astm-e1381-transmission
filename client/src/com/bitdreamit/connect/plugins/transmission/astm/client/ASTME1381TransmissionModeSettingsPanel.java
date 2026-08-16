package com.bitdreamit.connect.plugins.transmission.astm.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

/**
 * ASTM E1381-02 Transmission Mode Settings Panel.
 *
 * <p>Displayed inside Mirth's "Transmission Mode Settings" modal dialog
 * when the user clicks the settings link next to the "ASTM E1381"
 * transmission mode dropdown in the channel editor.</p>
 *
 * <p>Layout matches the MLLM (MLLP) reference style:
 * a left panel with grouped settings fields and a right panel with
 * byte abbreviation reference.</p>
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
    }

    // ------------------------------------------------------------------
    // Component initialization
    // ------------------------------------------------------------------

    private void initComponents() {
        enquiryField              = new JTextField(8);
        stxField                  = new JTextField(8);
        maxContentLengthField     = new JTextField(8);
        etbField                  = new JTextField(8);
        etxField                  = new JTextField(8);
        checksumLengthField       = new JTextField(8);
        frameTerminatorField      = new JTextField(10);
        eotField                  = new JTextField(8);

        validateFrameNumberBox    = new JCheckBox();
        ignoreServerCancelBox     = new JCheckBox();
        useChecksumBox            = new JCheckBox();
        strictValidationBox       = new JCheckBox();
        checksumAlgorithmBox      = new JComboBox<>(
            new String[]{"Add Mod 256", "XOR", "None"});
        bidirectionalBox          = new JCheckBox();
        ackField                  = new JTextField(8);
        nakField                  = new JTextField(8);

        maxTransferAttemptsField  = new JTextField(8);
        establishmentTimeoutField = new JTextField(8);
        contentionTimeoutField    = new JTextField(8);
        frameTimeoutField         = new JTextField(8);
        responseTimeoutField      = new JTextField(8);

        serverModeBox             = new JCheckBox();
    }

    // ------------------------------------------------------------------
    // Layout - matches MLLM style with two-column layout
    // ------------------------------------------------------------------

    private void initLayout() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(Color.WHITE);

        // --- Left panel: all settings ---
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(Color.WHITE);

        leftPanel.add(createFrameSettingsPanel());
        leftPanel.add(Box.createVerticalStrut(8));
        leftPanel.add(createValidationSettingsPanel());
        leftPanel.add(Box.createVerticalStrut(8));
        leftPanel.add(createConnectionSettingsPanel());
        leftPanel.add(Box.createVerticalStrut(8));
        leftPanel.add(createModePanel());

        // --- Right panel: byte abbreviations reference ---
        JPanel rightPanel = createByteReferencePanel();

        // --- Main layout ---
        add(leftPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
    }

    private JPanel createFrameSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(createTitledBorder("Frame Settings"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addHexRow(panel, gbc, row++, "Enquiry (ENQ):", enquiryField, "05", "<ENQ>");
        addHexRow(panel, gbc, row++, "Start of Frame (STX):", stxField, "02", "<STX>");
        addTextRow(panel, gbc, row++, "Max Content Length:", maxContentLengthField, "240");
        addHexRow(panel, gbc, row++, "Intermediate End (ETB):", etbField, "17", "<ETB>");
        addHexRow(panel, gbc, row++, "End of Frame (ETX):", etxField, "03", "<ETX>");
        addTextRow(panel, gbc, row++, "Checksum Byte Length:", checksumLengthField, "2");
        addHexRow(panel, gbc, row++, "Frame Terminator:", frameTerminatorField, "0D0A", "<CR><LF>");
        addHexRow(panel, gbc, row++, "End of Transmission (EOT):", eotField, "04", "<EOT>");

        return panel;
    }

    private JPanel createValidationSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(createTitledBorder("Validation Settings"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addCheckRow(panel, gbc, row++, "Validate Frame Number:", validateFrameNumberBox, true);
        addCheckRow(panel, gbc, row++, "Ignore Server-Side Cancel:", ignoreServerCancelBox, false);
        addCheckRow(panel, gbc, row++, "Use Checksum:", useChecksumBox, true);
        addCheckRow(panel, gbc, row++, "Use Strict Validation:", strictValidationBox, false);
        addComboRow(panel, gbc, row++, "Checksum Algorithm:", checksumAlgorithmBox);
        addCheckRow(panel, gbc, row++, "Bidirectional:", bidirectionalBox, true);
        addHexRow(panel, gbc, row++, "Positive ACK:", ackField, "06", "<ACK>");
        addHexRow(panel, gbc, row++, "Negative ACK:", nakField, "15", "<NAK>");

        return panel;
    }

    private JPanel createConnectionSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(createTitledBorder("Connection Settings"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addTextRow(panel, gbc, row++, "Max Transfer Attempts:", maxTransferAttemptsField, "6");
        addTextRow(panel, gbc, row++, "Establishment Timeout (ms):", establishmentTimeoutField, "15000");
        addTextRow(panel, gbc, row++, "Contention Timeout (ms):", contentionTimeoutField, "20000");
        addTextRow(panel, gbc, row++, "Frame Timeout (ms):", frameTimeoutField, "30000");
        addTextRow(panel, gbc, row++, "Response Timeout (ms):", responseTimeoutField, "15000");

        return panel;
    }

    private JPanel createModePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(createTitledBorder("Mode"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridy = 0;
        gbc.gridx = 0;
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

    private JPanel createByteReferencePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(createTitledBorder("Byte Abbreviations"));
        panel.setPreferredSize(new Dimension(140, 0));

        String[] abbrevs = {
            "<NUL> 0x00", "<SOH> 0x01", "<STX> 0x02", "<ETX> 0x03",
            "<EOT> 0x04", "<ENQ> 0x05", "<ACK> 0x06", "<BEL> 0x07",
            "<BS>  0x08", "<TAB> 0x09", "<LF>  0x0A", "<VT>  0x0B",
            "<FF>  0x0C", "<CR>  0x0D", "<ETB> 0x17", "<NAK> 0x15",
            "<SUB> 0x1A"
        };

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);
        listPanel.setBorder(new EmptyBorder(4, 8, 4, 8));

        for (String abbrev : abbrevs) {
            JLabel label = new JLabel(abbrev);
            label.setFont(new Font("Monospaced", Font.PLAIN, 11));
            label.setAlignmentX(LEFT_ALIGNMENT);
            listPanel.add(label);
        }

        panel.add(listPanel, BorderLayout.NORTH);
        return panel;
    }

    // ------------------------------------------------------------------
    // Layout helpers
    // ------------------------------------------------------------------

    private TitledBorder createTitledBorder(String title) {
        TitledBorder tb = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(153, 153, 153), 1),
            title,
            TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION,
            new Font("Tahoma", Font.BOLD, 11));
        return tb;
    }

    private void addHexRow(JPanel panel, GridBagConstraints gbc, int row,
                           String label, JTextField field, String hexValue, String abbrev) {
        gbc.gridy = row;
        // Label
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(label), gbc);
        // 0x prefix
        gbc.gridx = 1;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel("0x"), gbc);
        // Hex field
        gbc.gridx = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        field.setText(hexValue);
        panel.add(field, gbc);
        // Abbreviation label
        gbc.gridx = 3;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel abbrevLabel = new JLabel(abbrev);
        abbrevLabel.setForeground(new Color(100, 100, 100));
        abbrevLabel.setFont(new Font("Tahoma", Font.PLAIN, 11));
        panel.add(abbrevLabel, gbc);
    }

    private void addTextRow(JPanel panel, GridBagConstraints gbc, int row,
                             String label, JTextField field, String value) {
        gbc.gridy = row;
        // Label
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(label), gbc);
        // Field
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        field.setText(value);
        panel.add(field, gbc);
        gbc.gridwidth = 1;
    }

    private void addCheckRow(JPanel panel, GridBagConstraints gbc, int row,
                              String label, JCheckBox checkbox, boolean selected) {
        gbc.gridy = row;
        // Label
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(label), gbc);
        // Checkbox
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        checkbox.setSelected(selected);
        panel.add(checkbox, gbc);
        gbc.gridwidth = 1;
    }

    private void addComboRow(JPanel panel, GridBagConstraints gbc, int row,
                              String label, JComboBox<?> combo) {
        gbc.gridy = row;
        // Label
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(label), gbc);
        // Combo
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(combo, gbc);
        gbc.gridwidth = 1;
    }

    // ------------------------------------------------------------------
    // Default values
    // ------------------------------------------------------------------

    private void loadDefaults() {
        // Defaults are set during initLayout() via the addHexRow() and
        // addTextRow() helpers. This method is a placeholder for any
        // future preference-loading logic.
    }
}
