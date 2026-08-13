package com.bitdreamit.connect.plugins.transmission.astm.client;

import com.mirth.connect.client.ui.AbstractSettingsPanel;
import com.mirth.connect.client.ui.UIConstants;
import com.mirth.connect.client.ui.components.MirthTextField;
import com.mirth.connect.client.ui.components.MirthCheckBox;
import com.mirth.connect.client.ui.components.MirthComboBox;
import com.mirth.connect.model.transmission.TransmissionModeProperties;
import com.bitdreamit.connect.plugins.transmission.astm.server.ASTME1381TransmissionModeProperties;
import javax.swing.*;
import java.awt.*;

public class ASTME1381TransmissionModeSettingsPanel extends AbstractSettingsPanel {

    private MirthTextField enqTimeoutField;
    private MirthTextField frameAckTimeoutField;
    private MirthTextField maxEnqRetriesField;
    private MirthTextField maxFrameRetriesField;
    private MirthCheckBox strictSequencingCheckBox;
    private MirthComboBox frameNumberStartComboBox;

    public ASTME1381TransmissionModeSettingsPanel(String tabName) {
        super(tabName);
        initComponents();
        initLayout();
    }

    private void initComponents() {
        enqTimeoutField = new MirthTextField();
        enqTimeoutField.setToolTipText("Timeout in milliseconds waiting for ACK after ENQ");
        frameAckTimeoutField = new MirthTextField();
        frameAckTimeoutField.setToolTipText("Timeout in milliseconds waiting for ACK after each frame");
        maxEnqRetriesField = new MirthTextField();
        maxEnqRetriesField.setToolTipText("Maximum ENQ retry attempts");
        maxFrameRetriesField = new MirthTextField();
        maxFrameRetriesField.setToolTipText("Maximum frame retransmit attempts on NAK/timeout");
        strictSequencingCheckBox = new MirthCheckBox("Strict frame sequencing (NAK on out-of-order frames)");
        strictSequencingCheckBox.setToolTipText("Uncheck for lenient mode (log warning but accept frame)");
        frameNumberStartComboBox = new MirthComboBox();
        frameNumberStartComboBox.setModel(new DefaultComboBoxModel(new String[]{"0", "1"}));
        frameNumberStartComboBox.setToolTipText("Some analyzers start frame numbering at 0 instead of 1");
    }

    private void initLayout() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("ENQ Timeout (ms):"), gbc);
        gbc.gridx = 1;
        add(enqTimeoutField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("Frame ACK Timeout (ms):"), gbc);
        gbc.gridx = 1;
        add(frameAckTimeoutField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("Max ENQ Retries:"), gbc);
        gbc.gridx = 1;
        add(maxEnqRetriesField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        add(new JLabel("Max Frame Retries:"), gbc);
        gbc.gridx = 1;
        add(maxFrameRetriesField, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        add(strictSequencingCheckBox, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 1;
        add(new JLabel("Frame Number Start:"), gbc);
        gbc.gridx = 1;
        add(frameNumberStartComboBox, gbc);

        gbc.gridx = 0; gbc.gridy = 6; gbc.weighty = 1.0;
        add(Box.createVerticalGlue(), gbc);
    }

    @Override
    public void setProperties(TransmissionModeProperties properties) {
        ASTME1381TransmissionModeProperties props = (ASTME1381TransmissionModeProperties) properties;
        enqTimeoutField.setText(String.valueOf(props.getEnqTimeoutMs()));
        frameAckTimeoutField.setText(String.valueOf(props.getFrameAckTimeoutMs()));
        maxEnqRetriesField.setText(String.valueOf(props.getMaxEnqRetries()));
        maxFrameRetriesField.setText(String.valueOf(props.getMaxFrameRetries()));
        strictSequencingCheckBox.setSelected(props.isStrictFrameSequencing());
        frameNumberStartComboBox.setSelectedItem(String.valueOf(props.getFrameNumberStart()));
    }

    @Override
    public TransmissionModeProperties getProperties() {
        ASTME1381TransmissionModeProperties props = new ASTME1381TransmissionModeProperties();
        try {
            props.setEnqTimeoutMs(Integer.parseInt(enqTimeoutField.getText()));
        } catch (NumberFormatException e) { props.setEnqTimeoutMs(15000); }
        try {
            props.setFrameAckTimeoutMs(Integer.parseInt(frameAckTimeoutField.getText()));
        } catch (NumberFormatException e) { props.setFrameAckTimeoutMs(15000); }
        try {
            props.setMaxEnqRetries(Integer.parseInt(maxEnqRetriesField.getText()));
        } catch (NumberFormatException e) { props.setMaxEnqRetries(6); }
        try {
            props.setMaxFrameRetries(Integer.parseInt(maxFrameRetriesField.getText()));
        } catch (NumberFormatException e) { props.setMaxFrameRetries(6); }
        props.setStrictFrameSequencing(strictSequencingCheckBox.isSelected());
        try {
            props.setFrameNumberStart(Integer.parseInt((String) frameNumberStartComboBox.getSelectedItem()));
        } catch (NumberFormatException e) { props.setFrameNumberStart(1); }
        return props;
    }
}
