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
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381Constants;
import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties;
import com.mirth.connect.client.ui.components.MirthCheckBox;
import com.mirth.connect.client.ui.components.MirthComboBox;
import com.mirth.connect.client.ui.components.MirthTextField;

/**
 * ASTM E1381 Settings Dialog (modal) - Mirth MLLP-style.
 *
 * <p>This dialog mirrors the design of Mirth Connect's built-in
 * {@code MLLPModeSettingsDialog}: a modal dialog with a two-column layout -
 * settings on the left, a byte-abbreviations reference on the right, and
 * OK / Cancel buttons at the bottom.</p>
 *
 * <p><b>Component choice (v1.3.3):</b> All input fields now use Mirth's
 * private UI components - {@link MirthTextField}, {@link MirthCheckBox},
 * {@link MirthComboBox} - instead of plain Swing. This makes the dialog
 * pick up Mirth's theming, tab-order, and validation styling, exactly
 * like Mirth's MLLP settings dialog does.</p>
 *
 * <p><b>Load / Save (v1.3.3):</b> The dialog takes a non-null
 * {@link ASTME1381TransmissionModeProperties} in its constructor and
 * <b>loads every field from it on open</b>. When the user clicks OK, the
 * dialog writes the field values back into the same Properties object
 * <b>in place</b> (no defensive copy) and returns {@code true} - the
 * caller already holds the reference, so Mirth's channel-serialization
 * picks up the changes automatically. This was the root cause of the
 * "save doesn't work" bug in v1.3.2: the dialog never loaded from props
 * and the provider wrote to a fresh props on OK, so Mirth's reference
 * was never updated.</p>
 *
 * <p><b>Validation (v1.3.3):</b> Each field is validated on OK. Invalid
 * fields get a red border and a tooltip explaining the problem. The
 * dialog stays open until all fields are valid or the user clicks
 * Cancel.</p>
 */
public class ASTME1381SettingsDialog extends JDialog {

    private boolean okPressed = false;
    private final ASTME1381TransmissionModeProperties props;

    // --- Frame Settings fields (Mirth components) ---
    private MirthTextField enquiryField;
    private MirthTextField stxField;
    private MirthTextField maxContentLengthField;
    private MirthTextField etbField;
    private MirthTextField etxField;
    private MirthTextField checksumLengthField;
    private MirthTextField frameTerminatorField;
    private MirthTextField eotField;

    // --- Validation Settings fields (Mirth components) ---
    private MirthCheckBox validateFrameNumberBox;
    private MirthCheckBox ignoreServerCancelBox;
    private MirthCheckBox useChecksumBox;
    private MirthCheckBox strictValidationBox;
    private MirthComboBox checksumAlgorithmBox;
    private MirthCheckBox bidirectionalBox;
    private MirthTextField ackField;
    private MirthTextField nakField;

    // --- Connection Settings fields (Mirth components) ---
    private MirthTextField maxTransferAttemptsField;
    private MirthTextField establishmentTimeoutField;
    private MirthTextField contentionTimeoutField;
    private MirthTextField frameTimeoutField;
    private MirthTextField responseTimeoutField;

    // --- Mode field ---
    private MirthCheckBox serverModeBox;

    // --- Buttons ---
    private JButton okButton;
    private JButton cancelButton;

    /**
     * Build the dialog.
     *
     * @param parent the parent frame (for modal positioning); may be null
     * @param props  the properties to load on open and write back to on OK;
     *               MUST NOT be null (caller is responsible for ensuring
     *               the provider has a non-null props before opening)
     */
    public ASTME1381SettingsDialog(Frame parent, ASTME1381TransmissionModeProperties props) {
        super(parent, "ASTM E1381 Transmission Mode Settings", true);
        if (props == null) {
            throw new IllegalArgumentException(
                "ASTME1381SettingsDialog requires a non-null Properties " +
                "(the caller must ensure the provider has properties set " +
                "before opening the dialog)");
        }
        this.props = props;
        initComponents();
        initLayout();
        loadFromProperties();
        pack();
        setLocationRelativeTo(parent);
    }

    // ------------------------------------------------------------------
    // Component creation
    // ------------------------------------------------------------------

    private void initComponents() {
        // Frame Settings
        enquiryField              = new MirthTextField();
        stxField                  = new MirthTextField();
        maxContentLengthField     = new MirthTextField();
        etbField                  = new MirthTextField();
        etxField                  = new MirthTextField();
        checksumLengthField       = new MirthTextField();
        frameTerminatorField      = new MirthTextField();
        eotField                  = new MirthTextField();

        // Validation Settings
        validateFrameNumberBox    = new MirthCheckBox();
        ignoreServerCancelBox     = new MirthCheckBox();
        useChecksumBox            = new MirthCheckBox();
        strictValidationBox       = new MirthCheckBox();
        checksumAlgorithmBox      = new MirthComboBox();
        bidirectionalBox          = new MirthCheckBox();
        ackField                  = new MirthTextField();
        nakField                  = new MirthTextField();

        // Connection Settings
        maxTransferAttemptsField  = new MirthTextField();
        establishmentTimeoutField = new MirthTextField();
        contentionTimeoutField    = new MirthTextField();
        frameTimeoutField         = new MirthTextField();
        responseTimeoutField      = new MirthTextField();

        // Mode
        serverModeBox             = new MirthCheckBox();

        // Checksum algorithm combo box options
        checksumAlgorithmBox.setModel(new javax.swing.DefaultComboBoxModel<>(
            new String[]{
                ASTME1381Constants.CHECKSUM_ADD_MOD_256,
                ASTME1381Constants.CHECKSUM_XOR,
                ASTME1381Constants.CHECKSUM_NONE
            }));

        // Preferred column widths
        for (MirthTextField f : new MirthTextField[]{
                enquiryField, stxField, maxContentLengthField, etbField,
                etxField, checksumLengthField, eotField,
                ackField, nakField,
                maxTransferAttemptsField, establishmentTimeoutField,
                contentionTimeoutField, frameTimeoutField, responseTimeoutField
        }) {
            f.setColumns(10);
            f.setMinimumSize(new Dimension(80, 24));
            f.setPreferredSize(new Dimension(120, 24));
        }
        frameTerminatorField.setColumns(12);
        frameTerminatorField.setMinimumSize(new Dimension(100, 24));
        frameTerminatorField.setPreferredSize(new Dimension(140, 24));

        // Buttons
        okButton     = new JButton("OK");
        cancelButton = new JButton("Cancel");

        okButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                if (validateAndSave()) {
                    okPressed = true;
                    setVisible(false);
                    dispose();
                }
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                okPressed = false;
                setVisible(false);
                dispose();
            }
        });

        // Default button
        getRootPane().setDefaultButton(okButton);
    }

    // ------------------------------------------------------------------
    // Layout - two columns: settings (left) + byte reference (right)
    // ------------------------------------------------------------------

    private void initLayout() {
        setLayout(new BorderLayout(8, 8));
        setBackground(Color.WHITE);

        // --- Left: all settings ---
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

        // --- Right: byte abbreviations reference ---
        JPanel rightPanel = createByteReferencePanel();

        // --- Center: left + right ---
        JPanel centerPanel = new JPanel(new BorderLayout(8, 0));
        centerPanel.setBackground(Color.WHITE);
        centerPanel.add(leftPanel, BorderLayout.CENTER);
        centerPanel.add(rightPanel, BorderLayout.EAST);

        // --- Bottom: OK / Cancel ---
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
        addHexRow(panel, gbc, row++, "Enquiry (ENQ):",        enquiryField,         "<ENQ>");
        addHexRow(panel, gbc, row++, "Start of Frame (STX):", stxField,             "<STX>");
        addTextRow(panel, gbc, row++, "Max Content Length:",  maxContentLengthField);
        addHexRow(panel, gbc, row++, "Intermediate End (ETB):", etbField,          "<ETB>");
        addHexRow(panel, gbc, row++, "End of Frame (ETX):",   etxField,             "<ETX>");
        addTextRow(panel, gbc, row++, "Checksum Byte Length:", checksumLengthField);
        addHexRow(panel, gbc, row++, "Frame Terminator:",      frameTerminatorField, "<CR><LF>");
        addHexRow(panel, gbc, row++, "End of Transmission (EOT):", eotField,        "<EOT>");
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
        addCheckRow(panel, gbc, row++, "Validate Frame Number:",  validateFrameNumberBox);
        addCheckRow(panel, gbc, row++, "Ignore Server-Side Cancel:", ignoreServerCancelBox);
        addCheckRow(panel, gbc, row++, "Use Checksum:",            useChecksumBox);
        addCheckRow(panel, gbc, row++, "Use Strict Validation:",   strictValidationBox);
        addComboRow(panel, gbc, row++, "Checksum Algorithm:",      checksumAlgorithmBox);
        addCheckRow(panel, gbc, row++, "Bidirectional:",           bidirectionalBox);
        addHexRow(panel, gbc, row++, "Positive ACK:",             ackField,             "<ACK>");
        addHexRow(panel, gbc, row++, "Negative ACK:",             nakField,             "<NAK>");
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
        addTextRow(panel, gbc, row++, "Max Transfer Attempts:",   maxTransferAttemptsField);
        addTextRow(panel, gbc, row++, "Establishment Timeout (ms):", establishmentTimeoutField);
        addTextRow(panel, gbc, row++, "Contention Timeout (ms):",    contentionTimeoutField);
        addTextRow(panel, gbc, row++, "Frame Timeout (ms):",       frameTimeoutField);
        addTextRow(panel, gbc, row++, "Response Timeout (ms):",    responseTimeoutField);
        return panel;
    }

    private JPanel createModePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(createTitledBorder("Mode"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0; gbc.gridx = 0;
        gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel("Server Mode:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.anchor = GridBagConstraints.WEST;
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

    // ------------------------------------------------------------------
    // Layout helpers
    // ------------------------------------------------------------------

    private TitledBorder createTitledBorder(String title) {
        return BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(153, 153, 153), 1),
            title,
            TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION,
            new Font("Tahoma", Font.BOLD, 11));
    }

    private void addHexRow(JPanel panel, GridBagConstraints gbc, int row,
                           String label, MirthTextField field, String abbrev) {
        gbc.gridy = row;
        gbc.gridx = 0; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel("0x"), gbc);
        gbc.gridx = 2; gbc.weightx = 1.0; gbc.anchor = GridBagConstraints.WEST;
        panel.add(field, gbc);
        gbc.gridx = 3; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.WEST;
        JLabel abbrevLabel = new JLabel(abbrev);
        abbrevLabel.setForeground(new Color(100, 100, 100));
        panel.add(abbrevLabel, gbc);
    }

    private void addTextRow(JPanel panel, GridBagConstraints gbc, int row,
                             String label, MirthTextField field) {
        gbc.gridy = row;
        gbc.gridx = 0; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        gbc.weightx = 1.0; gbc.anchor = GridBagConstraints.WEST;
        panel.add(field, gbc);
        gbc.gridwidth = 1;
    }

    private void addCheckRow(JPanel panel, GridBagConstraints gbc, int row,
                              String label, MirthCheckBox checkbox) {
        gbc.gridy = row;
        gbc.gridx = 0; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        gbc.weightx = 1.0; gbc.anchor = GridBagConstraints.WEST;
        panel.add(checkbox, gbc);
        gbc.gridwidth = 1;
    }

    private void addComboRow(JPanel panel, GridBagConstraints gbc, int row,
                              String label, MirthComboBox combo) {
        gbc.gridy = row;
        gbc.gridx = 0; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        gbc.weightx = 1.0; gbc.anchor = GridBagConstraints.WEST;
        panel.add(combo, gbc);
        gbc.gridwidth = 1;
    }

    // ------------------------------------------------------------------
    // LOAD: read every field from the Properties object
    // ------------------------------------------------------------------

    private void loadFromProperties() {
        // Hex byte fields - rendered as 2-digit uppercase hex (no "0x" prefix;
        // the "0x" label sits to the left of the field in the UI)
        enquiryField.setText(toHex2(props.getEnquiryByte()));
        stxField.setText(toHex2(props.getStartOfFrameByte()));
        etbField.setText(toHex2(props.getIntermediateEndOfFrame()));
        etxField.setText(toHex2(props.getEndOfFrameByte()));
        eotField.setText(toHex2(props.getEndOfTransmissionByte()));
        ackField.setText(toHex2(props.getPositiveAckByte()));
        nakField.setText(toHex2(props.getNegativeAckByte()));

        // Frame terminator is 1-4 hex digits (e.g. "0D0A" for CR+LF)
        frameTerminatorField.setText(normalizeHex(props.getFrameTerminator()));

        // Plain integer fields
        maxContentLengthField.setText(Integer.toString(props.getMaxFrameContentLength()));
        checksumLengthField.setText(Integer.toString(props.getChecksumByteLength()));
        maxTransferAttemptsField.setText(Integer.toString(props.getMaxTransferAttempts()));
        establishmentTimeoutField.setText(Integer.toString(props.getEstablishmentTimeout()));
        contentionTimeoutField.setText(Integer.toString(props.getContentionTimeout()));
        frameTimeoutField.setText(Integer.toString(props.getFrameTimeout()));
        responseTimeoutField.setText(Integer.toString(props.getResponseTimeout()));

        // Checkboxes
        validateFrameNumberBox.setSelected(props.isValidateFrameNumber());
        ignoreServerCancelBox.setSelected(props.isIgnoreServerSideCancel());
        useChecksumBox.setSelected(props.isUseChecksum());
        strictValidationBox.setSelected(props.isUseStrictValidation());
        bidirectionalBox.setSelected(props.isBidirectional());
        serverModeBox.setSelected(props.isServerMode());

        // Combo box
        checksumAlgorithmBox.setSelectedItem(props.getChecksumAlgorithm());
    }

    // ------------------------------------------------------------------
    // VALIDATE + SAVE: validate every field, write back to props in place
    // ------------------------------------------------------------------

    private boolean validateAndSave() {
        // Clear any previous error states
        clearError(enquiryField); clearError(stxField); clearError(etbField);
        clearError(etxField); clearError(eotField); clearError(ackField);
        clearError(nakField); clearError(frameTerminatorField);
        clearError(maxContentLengthField); clearError(checksumLengthField);
        clearError(maxTransferAttemptsField); clearError(establishmentTimeoutField);
        clearError(contentionTimeoutField); clearError(frameTimeoutField);
        clearError(responseTimeoutField);

        boolean ok = true;

        // --- Hex byte fields (must be valid hex, value 0x00-0xFF) ---
        int enquiry = parseByteHex(enquiryField.getText(), "ENQ");      if (enquiry < 0) { markError(enquiryField, "ENQ must be hex 00-FF"); ok = false; }
        int stx     = parseByteHex(stxField.getText(),     "STX");      if (stx     < 0) { markError(stxField,     "STX must be hex 00-FF"); ok = false; }
        int etb     = parseByteHex(etbField.getText(),     "ETB");      if (etb     < 0) { markError(etbField,     "ETB must be hex 00-FF"); ok = false; }
        int etx     = parseByteHex(etxField.getText(),     "ETX");      if (etx     < 0) { markError(etxField,     "ETX must be hex 00-FF"); ok = false; }
        int eot     = parseByteHex(eotField.getText(),     "EOT");      if (eot     < 0) { markError(eotField,     "EOT must be hex 00-FF"); ok = false; }
        int ack     = parseByteHex(ackField.getText(),      "ACK");      if (ack     < 0) { markError(ackField,     "ACK must be hex 00-FF"); ok = false; }
        int nak     = parseByteHex(nakField.getText(),      "NAK");      if (nak     < 0) { markError(nakField,     "NAK must be hex 00-FF"); ok = false; }

        // --- Frame terminator (1-4 hex digits, e.g. "0D0A") ---
        String termStr = normalizeHex(frameTerminatorField.getText());
        if (termStr.isEmpty() || termStr.length() > 4 || !termStr.matches("[0-9A-Fa-f]+")) {
            markError(frameTerminatorField, "Frame terminator must be 1-4 hex digits (e.g. 0D0A)");
            ok = false;
        }

        // --- Positive integer fields ---
        int maxContent = parseIntSafe(maxContentLengthField.getText());
        if (maxContent <= 0) { markError(maxContentLengthField, "Must be > 0"); ok = false; }
        int checksumLen = parseIntSafe(checksumLengthField.getText());
        if (checksumLen < 1 || checksumLen > 2) { markError(checksumLengthField, "Must be 1 or 2"); ok = false; }
        int maxAttempts = parseIntSafe(maxTransferAttemptsField.getText());
        if (maxAttempts <= 0) { markError(maxTransferAttemptsField, "Must be > 0"); ok = false; }
        int estTimeout = parseIntSafe(establishmentTimeoutField.getText());
        if (estTimeout <= 0) { markError(establishmentTimeoutField, "Must be > 0"); ok = false; }
        int contTimeout = parseIntSafe(contentionTimeoutField.getText());
        if (contTimeout <= 0) { markError(contentionTimeoutField, "Must be > 0"); ok = false; }
        int frameTimeout = parseIntSafe(frameTimeoutField.getText());
        if (frameTimeout <= 0) { markError(frameTimeoutField, "Must be > 0"); ok = false; }
        int respTimeout = parseIntSafe(responseTimeoutField.getText());
        if (respTimeout <= 0) { markError(responseTimeoutField, "Must be > 0"); ok = false; }

        if (!ok) {
            // Surface a single message-box error to draw attention
            javax.swing.JOptionPane.showMessageDialog(this,
                "Some fields have invalid values (highlighted in red).\n" +
                "Please fix them before saving.",
                "Validation Error",
                javax.swing.JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // --- All fields valid - write back to props IN PLACE ---
        // (Mirth holds the same Properties reference, so changes propagate
        // automatically to the channel XML on the next save.)
        props.setEnquiryByte(enquiry);
        props.setStartOfFrameByte(stx);
        props.setIntermediateEndOfFrame(etb);
        props.setEndOfFrameByte(etx);
        props.setEndOfTransmissionByte(eot);
        props.setPositiveAckByte(ack);
        props.setNegativeAckByte(nak);
        props.setFrameTerminator("0x" + termStr.toUpperCase());

        props.setMaxFrameContentLength(maxContent);
        props.setChecksumByteLength(checksumLen);
        props.setMaxTransferAttempts(maxAttempts);
        props.setEstablishmentTimeout(estTimeout);
        props.setContentionTimeout(contTimeout);
        props.setFrameTimeout(frameTimeout);
        props.setResponseTimeout(respTimeout);

        props.setValidateFrameNumber(validateFrameNumberBox.isSelected());
        props.setIgnoreServerSideCancel(ignoreServerCancelBox.isSelected());
        props.setUseChecksum(useChecksumBox.isSelected());
        props.setUseStrictValidation(strictValidationBox.isSelected());
        props.setBidirectional(bidirectionalBox.isSelected());
        props.setServerMode(serverModeBox.isSelected());

        props.setChecksumAlgorithm((String) checksumAlgorithmBox.getSelectedItem());

        return true;
    }

    // ------------------------------------------------------------------
    // Validation helpers
    // ------------------------------------------------------------------

    private static String toHex2(int b) {
        return String.format("%02X", b & 0xFF);
    }

    private static String normalizeHex(String s) {
        if (s == null) return "";
        return s.trim().replace("0x", "").replace("0X", "").toUpperCase();
    }

    private static int parseByteHex(String s, String fieldName) {
        String h = normalizeHex(s);
        if (h.isEmpty() || h.length() > 2 || !h.matches("[0-9A-Fa-f]+")) return -1;
        return Integer.parseInt(h, 16);
    }

    private static int parseIntSafe(String s) {
        if (s == null || s.trim().isEmpty()) return -1;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return -1; }
    }

    private static void markError(javax.swing.JComponent field, String message) {
        field.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
        field.setToolTipText(message);
    }

    private static void clearError(javax.swing.JComponent field) {
        field.setBorder(javax.swing.UIManager.getBorder("TextField.border"));
        field.setToolTipText(null);
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    public boolean isOkPressed() {
        return okPressed;
    }
}
