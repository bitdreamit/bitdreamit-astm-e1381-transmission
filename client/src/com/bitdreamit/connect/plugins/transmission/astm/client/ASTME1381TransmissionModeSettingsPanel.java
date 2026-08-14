package com.bitdreamit.connect.plugins.transmission.astm.client;

import java.awt.Color;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import com.mirth.connect.client.ui.AbstractSettingsPanel;
import com.mirth.connect.client.ui.components.MirthCheckBox;
import com.mirth.connect.client.ui.components.MirthComboBox;
import com.mirth.connect.client.ui.components.MirthTextField;

import net.miginfocom.swing.MigLayout;

/**
 * ASTM E1381-95 Transmission Mode Settings Panel
 * Production-grade UI matching NextGen's screenshot with premium additions.
 */
public class ASTME1381TransmissionModeSettingsPanel extends AbstractSettingsPanel {

    private static final String PREFIX = "com.bitdreamit.astm.e1381.";

    // Frame Settings
    private MirthTextField enquiryField;
    private MirthTextField stxField;
    private MirthTextField maxContentLengthField;
    private MirthTextField etbField;
    private MirthTextField etxField;
    private MirthTextField checksumLengthField;
    private MirthTextField frameTerminatorField;
    private MirthTextField eotField;

    // Validation Settings
    private MirthCheckBox validateFrameNumberBox;
    private MirthCheckBox ignoreServerCancelBox;
    private MirthCheckBox useChecksumBox;
    private MirthCheckBox strictValidationBox;
    private MirthComboBox checksumAlgorithmBox;
    private MirthCheckBox bidirectionalBox;
    private MirthTextField ackField;
    private MirthTextField nakField;

    // Connection Settings
    private MirthTextField maxTransferAttemptsField;
    private MirthTextField establishmentTimeoutField;
    private MirthTextField contentionTimeoutField;
    private MirthTextField frameTimeoutField;
    private MirthTextField responseTimeoutField;

    // Mode
    private MirthCheckBox serverModeBox;

    public ASTME1381TransmissionModeSettingsPanel(String tabName) {
        super(tabName);
        initComponents();
        initLayout();
        doRefresh();
    }

    private void initComponents() {
        enquiryField              = new MirthTextField();
        stxField                  = new MirthTextField();
        maxContentLengthField     = new MirthTextField();
        etbField                  = new MirthTextField();
        etxField                  = new MirthTextField();
        checksumLengthField       = new MirthTextField();
        frameTerminatorField      = new MirthTextField();
        eotField                  = new MirthTextField();

        validateFrameNumberBox    = new MirthCheckBox();
        ignoreServerCancelBox     = new MirthCheckBox();
        useChecksumBox            = new MirthCheckBox();
        strictValidationBox       = new MirthCheckBox();
        checksumAlgorithmBox      = new MirthComboBox();
        bidirectionalBox          = new MirthCheckBox();
        ackField                  = new MirthTextField();
        nakField                  = new MirthTextField();

        maxTransferAttemptsField  = new MirthTextField();
        establishmentTimeoutField = new MirthTextField();
        contentionTimeoutField    = new MirthTextField();
        frameTimeoutField         = new MirthTextField();
        responseTimeoutField      = new MirthTextField();

        serverModeBox             = new MirthCheckBox();

        // Defaults
        enquiryField.setText("0x05");
        stxField.setText("0x02");
        maxContentLengthField.setText("240");
        etbField.setText("0x17");
        etxField.setText("0x03");
        checksumLengthField.setText("2");
        frameTerminatorField.setText("0x000A");
        eotField.setText("0x04");

        validateFrameNumberBox.setSelected(true);
        ignoreServerCancelBox.setSelected(false);
        useChecksumBox.setSelected(true);
        strictValidationBox.setSelected(false);
        checksumAlgorithmBox.setModel(new javax.swing.DefaultComboBoxModel<>(
            new String[]{"Add Mod 256", "XOR", "None"}));
        bidirectionalBox.setSelected(true);
        ackField.setText("0x06");
        nakField.setText("0x15");

        maxTransferAttemptsField.setText("6");
        establishmentTimeoutField.setText("15000");
        contentionTimeoutField.setText("20000");
        frameTimeoutField.setText("30000");
        responseTimeoutField.setText("15000");

        serverModeBox.setSelected(true);
    }

    private void initLayout() {
        setBackground(Color.WHITE);
        setLayout(new MigLayout("insets 12, fillx, wrap 2", "[right][left,grow]", ""));

        // --- Frame Settings Panel ---
        JPanel framePanel = new JPanel(new MigLayout("insets 8, fillx, wrap 2", "[right][left,grow]"));
        framePanel.setBackground(Color.WHITE);
        framePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(204, 204, 204)),
            "Frame Settings", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            new java.awt.Font("Tahoma", java.awt.Font.BOLD, 11)));

        framePanel.add(new JLabel("Enquiry (ENQ):"));      framePanel.add(enquiryField, "w 80!");
        framePanel.add(new JLabel("Start of Frame (STX):")); framePanel.add(stxField, "w 80!");
        framePanel.add(new JLabel("Max Content Length:"));   framePanel.add(maxContentLengthField, "w 80!");
        framePanel.add(new JLabel("Intermediate End (ETB):")); framePanel.add(etbField, "w 80!");
        framePanel.add(new JLabel("End of Frame (ETX):"));   framePanel.add(etxField, "w 80!");
        framePanel.add(new JLabel("Checksum Byte Length:")); framePanel.add(checksumLengthField, "w 80!");
        framePanel.add(new JLabel("Frame Terminator:"));     framePanel.add(frameTerminatorField, "w 120!");
        framePanel.add(new JLabel("End of Transmission (EOT):")); framePanel.add(eotField, "w 80!");

        // --- Validation Settings Panel ---
        JPanel validationPanel = new JPanel(new MigLayout("insets 8, fillx, wrap 2", "[right][left,grow]"));
        validationPanel.setBackground(Color.WHITE);
        validationPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(204, 204, 204)),
            "Validation Settings", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            new java.awt.Font("Tahoma", java.awt.Font.BOLD, 11)));

        validationPanel.add(new JLabel("Validate Frame Number:")); validationPanel.add(validateFrameNumberBox);
        validationPanel.add(new JLabel("Ignore Server-Side Cancel:")); validationPanel.add(ignoreServerCancelBox);
        validationPanel.add(new JLabel("Use Checksum:"));          validationPanel.add(useChecksumBox);
        validationPanel.add(new JLabel("Use Strict Validation:")); validationPanel.add(strictValidationBox);
        validationPanel.add(new JLabel("Checksum Algorithm:"));    validationPanel.add(checksumAlgorithmBox, "w 150!");
        validationPanel.add(new JLabel("Bidirectional:"));          validationPanel.add(bidirectionalBox);
        validationPanel.add(new JLabel("Positive ACK:"));          validationPanel.add(ackField, "w 80!");
        validationPanel.add(new JLabel("Negative ACK:"));            validationPanel.add(nakField, "w 80!");

        // --- Connection Settings Panel ---
        JPanel connPanel = new JPanel(new MigLayout("insets 8, fillx, wrap 2", "[right][left,grow]"));
        connPanel.setBackground(Color.WHITE);
        connPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(204, 204, 204)),
            "Connection Settings", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            new java.awt.Font("Tahoma", java.awt.Font.BOLD, 11)));

        connPanel.add(new JLabel("Max Transfer Attempts:"));      connPanel.add(maxTransferAttemptsField, "w 80!");
        connPanel.add(new JLabel("Establishment Timeout (ms):")); connPanel.add(establishmentTimeoutField, "w 100!");
        connPanel.add(new JLabel("Contention Timeout (ms):"));    connPanel.add(contentionTimeoutField, "w 100!");
        connPanel.add(new JLabel("Frame Timeout (ms):"));          connPanel.add(frameTimeoutField, "w 100!");
        connPanel.add(new JLabel("Response Timeout (ms):"));      connPanel.add(responseTimeoutField, "w 100!");

        // --- Mode Panel ---
        JPanel modePanel = new JPanel(new MigLayout("insets 8, fillx, wrap 2", "[right][left,grow]"));
        modePanel.setBackground(Color.WHITE);
        modePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(204, 204, 204)),
            "Mode", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            new java.awt.Font("Tahoma", java.awt.Font.BOLD, 11)));

        modePanel.add(new JLabel("Server Mode:")); modePanel.add(serverModeBox);

        add(framePanel, "growx, span 2");
        add(validationPanel, "growx, span 2");
        add(connPanel, "growx, span 2");
        add(modePanel, "growx, span 2");
    }

    @Override
    public void doRefresh() {
        Preferences p = Preferences.userNodeForPackage(this.getClass());
        enquiryField.setText(p.get(PREFIX + "enquiry", "0x05"));
        stxField.setText(p.get(PREFIX + "stx", "0x02"));
        maxContentLengthField.setText(p.get(PREFIX + "maxContentLength", "240"));
        etbField.setText(p.get(PREFIX + "etb", "0x17"));
        etxField.setText(p.get(PREFIX + "etx", "0x03"));
        checksumLengthField.setText(p.get(PREFIX + "checksumLength", "2"));
        frameTerminatorField.setText(p.get(PREFIX + "frameTerminator", "0x000A"));
        eotField.setText(p.get(PREFIX + "eot", "0x04"));

        validateFrameNumberBox.setSelected(p.getBoolean(PREFIX + "validateFrameNumber", true));
        ignoreServerCancelBox.setSelected(p.getBoolean(PREFIX + "ignoreServerCancel", false));
        useChecksumBox.setSelected(p.getBoolean(PREFIX + "useChecksum", true));
        strictValidationBox.setSelected(p.getBoolean(PREFIX + "strictValidation", false));
        checksumAlgorithmBox.setSelectedItem(p.get(PREFIX + "checksumAlgorithm", "Add Mod 256"));
        bidirectionalBox.setSelected(p.getBoolean(PREFIX + "bidirectional", true));
        ackField.setText(p.get(PREFIX + "ack", "0x06"));
        nakField.setText(p.get(PREFIX + "nak", "0x15"));

        maxTransferAttemptsField.setText(p.get(PREFIX + "maxTransferAttempts", "6"));
        establishmentTimeoutField.setText(p.get(PREFIX + "establishmentTimeout", "15000"));
        contentionTimeoutField.setText(p.get(PREFIX + "contentionTimeout", "20000"));
        frameTimeoutField.setText(p.get(PREFIX + "frameTimeout", "30000"));
        responseTimeoutField.setText(p.get(PREFIX + "responseTimeout", "15000"));

        serverModeBox.setSelected(p.getBoolean(PREFIX + "serverMode", true));
    }

    @Override
    public boolean doSave() {
        // Validation
        if (!isValidHex(enquiryField.getText())) { showError("Enquiry must be hex (e.g. 0x05)"); return false; }
        if (!isValidHex(stxField.getText()))     { showError("STX must be hex"); return false; }
        if (!isPositiveInt(maxContentLengthField.getText())) { showError("Max content length must be positive integer"); return false; }
        if (!isValidHex(etbField.getText()))     { showError("ETB must be hex"); return false; }
        if (!isValidHex(etxField.getText()))     { showError("ETX must be hex"); return false; }
        if (!isPositiveInt(checksumLengthField.getText())) { showError("Checksum length must be 1 or 2"); return false; }
        if (!isValidHex(frameTerminatorField.getText())) { showError("Frame terminator must be hex"); return false; }
        if (!isValidHex(eotField.getText()))     { showError("EOT must be hex"); return false; }
        if (!isValidHex(ackField.getText()))      { showError("ACK must be hex"); return false; }
        if (!isValidHex(nakField.getText()))      { showError("NAK must be hex"); return false; }
        if (!isPositiveInt(maxTransferAttemptsField.getText())) { showError("Max transfer attempts must be positive integer"); return false; }
        if (!isPositiveInt(establishmentTimeoutField.getText())) { showError("Timeouts must be positive integers"); return false; }

        Preferences p = Preferences.userNodeForPackage(this.getClass());
        p.put(PREFIX + "enquiry", enquiryField.getText().trim());
        p.put(PREFIX + "stx", stxField.getText().trim());
        p.put(PREFIX + "maxContentLength", maxContentLengthField.getText().trim());
        p.put(PREFIX + "etb", etbField.getText().trim());
        p.put(PREFIX + "etx", etxField.getText().trim());
        p.put(PREFIX + "checksumLength", checksumLengthField.getText().trim());
        p.put(PREFIX + "frameTerminator", frameTerminatorField.getText().trim());
        p.put(PREFIX + "eot", eotField.getText().trim());

        p.putBoolean(PREFIX + "validateFrameNumber", validateFrameNumberBox.isSelected());
        p.putBoolean(PREFIX + "ignoreServerCancel", ignoreServerCancelBox.isSelected());
        p.putBoolean(PREFIX + "useChecksum", useChecksumBox.isSelected());
        p.putBoolean(PREFIX + "strictValidation", strictValidationBox.isSelected());
        p.put(PREFIX + "checksumAlgorithm", (String) checksumAlgorithmBox.getSelectedItem());
        p.putBoolean(PREFIX + "bidirectional", bidirectionalBox.isSelected());
        p.put(PREFIX + "ack", ackField.getText().trim());
        p.put(PREFIX + "nak", nakField.getText().trim());

        p.put(PREFIX + "maxTransferAttempts", maxTransferAttemptsField.getText().trim());
        p.put(PREFIX + "establishmentTimeout", establishmentTimeoutField.getText().trim());
        p.put(PREFIX + "contentionTimeout", contentionTimeoutField.getText().trim());
        p.put(PREFIX + "frameTimeout", frameTimeoutField.getText().trim());
        p.put(PREFIX + "responseTimeout", responseTimeoutField.getText().trim());

        p.putBoolean(PREFIX + "serverMode", serverModeBox.isSelected());

        return true;
    }

    private boolean isValidHex(String s) {
        if (s == null || s.trim().isEmpty()) return false;
        s = s.trim().toLowerCase();
        return s.startsWith("0x") && s.length() > 2 && s.substring(2).matches("[0-9a-f]+") ||
               s.matches("[0-9a-f]+");
    }

    private boolean isPositiveInt(String s) {
        try { return Integer.parseInt(s.trim()) > 0; } catch (Exception e) { return false; }
    }

    private void showError(String msg) {
        javax.swing.JOptionPane.showMessageDialog(this, msg, "Validation Error", javax.swing.JOptionPane.ERROR_MESSAGE);
    }
}
