package com.bitdreamit.connect.plugins.transmission.astm.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

/**
 * ASTM E1381 Settings Dialog (modal).
 *
 * <p>Mirrors Mirth's MLLPModeSettingsDialog: a modal dialog with
 * settings on the left, byte abbreviations reference on the right,
 * and OK / Cancel buttons at the bottom.</p>
 *
 * <p>Opened when the user clicks the "Frame Settings" button in
 * {@link ASTME1381SettingsPanel}.</p>
 */
public class ASTME1381SettingsDialog extends JDialog {

    private boolean okPressed = false;

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

    // --- Buttons ---
    private JButton okButton;
    private JButton cancelButton;

    public ASTME1381SettingsDialog(Frame parent) {
        super(parent, "Transmission Mode Settings", true);
        initComponents();
        initLayout();
        pack();
        setLocationRelativeTo(parent);
    }

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

        okButton     = new JButton("OK");
        cancelButton = new JButton("Cancel");

        // Set default values
        enquiryField.setText("05");
        stxField.setText("02");
        maxContentLengthField.setText("240");
        etbField.setText("17");
        etxField.setText("03");
        checksumLengthField.setText("2");
        frameTerminatorField.setText("0D0A");
        eotField.setText("04");

        validateFrameNumberBox.setSelected(true);
        ignoreServerCancelBox.setSelected(false);
        useChecksumBox.setSelected(true);
        strictValidationBox.setSelected(false);
        bidirectionalBox.setSelected(true);
        ackField.setText("06");
        nakField.setText("15");

        maxTransferAttemptsField.setText("6");
        establishmentTimeoutField.setText("15000");
        contentionTimeoutField.setText("20000");
        frameTimeoutField.setText("30000");
        responseTimeoutField.setText("15000");

        serverModeBox.setSelected(true);

        // Button handlers
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                okPressed = true;
                setVisible(false);
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                okPressed = false;
                setVisible(false);
                dispose();
            }
        });
    }

    private void initLayout() {
        setLayout(new BorderLayout(8, 8));
        setBackground(Color.WHITE);

        // --- Left panel: all settings ---
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        leftPanel.add(createFrameSettingsPanel());
        leftPanel.add(Box.createVerticalStrut(6));
        leftPanel.add(createValidationSettingsPanel());
        leftPanel.add(Box.createVerticalStrut(6));
        leftPanel.add(createConnectionSettingsPanel());
        leftPanel.add(Box.createVerticalStrut(6));
        leftPanel.add(createModePanel());

        // --- Right panel: byte abbreviations ---
        JPanel rightPanel = createByteReferencePanel();

        // --- Center: left + right ---
        JPanel centerPanel = new JPanel(new BorderLayout(8, 0));
        centerPanel.setBackground(Color.WHITE);
        centerPanel.add(leftPanel, BorderLayout.CENTER);
        centerPanel.add(rightPanel, BorderLayout.EAST);

        // --- Bottom: OK / Cancel buttons ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        add(centerPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
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

        JList<String> list = new JList<>(abbrevs);
        list.setFont(new Font("Monospaced", Font.PLAIN, 11));
        list.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(120, 200));

        panel.add(scrollPane, BorderLayout.NORTH);
        return panel;
    }

    // --- Layout helpers ---

    private TitledBorder createTitledBorder(String title) {
        return BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(153, 153, 153), 1),
            title,
            TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION,
            new Font("Tahoma", Font.BOLD, 11));
    }

    private void addHexRow(JPanel panel, GridBagConstraints gbc, int row,
                           String label, JTextField field, String hexValue, String abbrev) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel("0x"), gbc);
        gbc.gridx = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(field, gbc);
        gbc.gridx = 3;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel abbrevLabel = new JLabel(abbrev);
        abbrevLabel.setForeground(new Color(100, 100, 100));
        panel.add(abbrevLabel, gbc);
    }

    private void addTextRow(JPanel panel, GridBagConstraints gbc, int row,
                             String label, JTextField field, String value) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(field, gbc);
        gbc.gridwidth = 1;
    }

    private void addCheckRow(JPanel panel, GridBagConstraints gbc, int row,
                              String label, JCheckBox checkbox, boolean selected) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(label), gbc);
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
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(combo, gbc);
        gbc.gridwidth = 1;
    }

    // --- Public API ---

    public boolean isOkPressed() {
        return okPressed;
    }

    // --- Field getters (for the provider to read after OK) ---

    public String getEnquiryByte() { return enquiryField.getText(); }
    public String getStartOfFrameByte() { return stxField.getText(); }
    public String getMaxFrameContentLength() { return maxContentLengthField.getText(); }
    public String getIntermediateEndOfFrame() { return etbField.getText(); }
    public String getEndOfFrameByte() { return etxField.getText(); }
    public String getChecksumByteLength() { return checksumLengthField.getText(); }
    public String getFrameTerminator() { return frameTerminatorField.getText(); }
    public String getEndOfTransmissionByte() { return eotField.getText(); }
    public boolean isValidateFrameNumber() { return validateFrameNumberBox.isSelected(); }
    public boolean isIgnoreServerSideCancel() { return ignoreServerCancelBox.isSelected(); }
    public boolean isUseChecksum() { return useChecksumBox.isSelected(); }
    public boolean isUseStrictValidation() { return strictValidationBox.isSelected(); }
    public String getChecksumAlgorithm() { return (String) checksumAlgorithmBox.getSelectedItem(); }
    public boolean isBidirectional() { return bidirectionalBox.isSelected(); }
    public String getPositiveAckByte() { return ackField.getText(); }
    public String getNegativeAckByte() { return nakField.getText(); }
    public String getMaxTransferAttempts() { return maxTransferAttemptsField.getText(); }
    public String getEstablishmentTimeout() { return establishmentTimeoutField.getText(); }
    public String getContentionTimeout() { return contentionTimeoutField.getText(); }
    public String getFrameTimeout() { return frameTimeoutField.getText(); }
    public String getResponseTimeout() { return responseTimeoutField.getText(); }
    public boolean isServerMode() { return serverModeBox.isSelected(); }
}
