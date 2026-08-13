/*
 * BitDreamIT Mirth Lab Extensions
 * Copyright (c) 2026 Kimi AI (Moonshot AI) — MIT License
 */
package com.bitdreamit.mirth.labextensions.astme1381transmission;

import com.mirth.connect.client.ui.AbstractConnectorSettingsPanel;
import com.mirth.connect.client.ui.components.MirthCheckBox;
import com.mirth.connect.client.ui.components.MirthComboBox;
import com.mirth.connect.client.ui.components.MirthTextField;
import com.mirth.connect.plugins.transmissionmode.TransmissionModeProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class AstmE1381ModeSettingsPanel extends AbstractConnectorSettingsPanel {

    private MirthCheckBox enqAckBox;
    private JLabel enqTimeoutLabel;
    private MirthTextField enqTimeoutField;
    private JLabel ackTimeoutLabel;
    private MirthTextField ackTimeoutField;

    private MirthCheckBox checksumBox;
    private JLabel checksumAlgoLabel;
    private MirthComboBox checksumAlgoBox;
    private JLabel frameSizeLabel;
    private MirthTextField frameSizeField;
    private JLabel interFrameDelayLabel;
    private MirthTextField interFrameDelayField;

    private JLabel maxRetriesLabel;
    private MirthTextField maxRetriesField;
    private MirthCheckBox expBackoffBox;
    private JLabel baseDelayLabel;
    private MirthTextField baseDelayField;
    private JLabel maxDelayLabel;
    private MirthTextField maxDelayField;

    private JLabel frameTimeoutLabel;
    private MirthTextField frameTimeoutField;
    private JLabel eotTimeoutLabel;
    private MirthTextField eotTimeoutField;
    private JLabel sessionTimeoutLabel;
    private MirthTextField sessionTimeoutField;

    private JLabel dialectLabel;
    private MirthComboBox dialectBox;
    private MirthCheckBox intermediateBox;

    private MirthCheckBox keepaliveBox;
    private JLabel keepaliveIntervalLabel;
    private MirthTextField keepaliveIntervalField;

    private MirthCheckBox protocolLogBox;
    private JLabel maxLogLabel;
    private MirthTextField maxLogField;

    public AstmE1381ModeSettingsPanel() {
        initComponents();
    }

    private void initComponents() {
        setBackground(Color.WHITE);
        setLayout(new MigLayout("insets 8, novisualpadding, hidemode 3, fillx, gap 4", "[][grow]", ""));

        JPanel handshakePanel = new JPanel(new MigLayout("insets 8, gap 4", "[][grow]", ""));
        handshakePanel.setBackground(Color.WHITE);
        handshakePanel.setBorder(new TitledBorder("Handshake"));
        enqAckBox = new MirthCheckBox("Use ENQ/ACK Handshake");
        enqAckBox.setSelected(true);
        enqAckBox.setBackground(Color.WHITE);
        enqTimeoutLabel = new JLabel("ENQ Timeout (ms):");
        enqTimeoutField = new MirthTextField(); enqTimeoutField.setText("1000");
        ackTimeoutLabel = new JLabel("ACK Timeout (ms):");
        ackTimeoutField = new MirthTextField(); ackTimeoutField.setText("1000");
        handshakePanel.add(enqAckBox, "span 2, wrap");
        handshakePanel.add(enqTimeoutLabel, "right");
        handshakePanel.add(enqTimeoutField, "w 100!, wrap");
        handshakePanel.add(ackTimeoutLabel, "right");
        handshakePanel.add(ackTimeoutField, "w 100!, wrap");
        add(handshakePanel, "span, growx, wrap");

        JPanel framePanel = new JPanel(new MigLayout("insets 8, gap 4", "[][grow]", ""));
        framePanel.setBackground(Color.WHITE);
        framePanel.setBorder(new TitledBorder("Framing & Checksum"));
        checksumBox = new MirthCheckBox("Use Checksum");
        checksumBox.setSelected(true);
        checksumBox.setBackground(Color.WHITE);
        checksumAlgoLabel = new JLabel("Algorithm:");
        checksumAlgoBox = new MirthComboBox();
        checksumAlgoBox.setModel(new DefaultComboBoxModel<>(new String[]{"SUM_MOD_256", "LRC", "CRC8", "CUSTOM"}));
        frameSizeLabel = new JLabel("Max Frame Size:");
        frameSizeField = new MirthTextField(); frameSizeField.setText("240");
        interFrameDelayLabel = new JLabel("Inter-frame Delay (ms):");
        interFrameDelayField = new MirthTextField(); interFrameDelayField.setText("100");
        framePanel.add(checksumBox, "span 2, wrap");
        framePanel.add(checksumAlgoLabel, "right");
        framePanel.add(checksumAlgoBox, "w 150!, wrap");
        framePanel.add(frameSizeLabel, "right");
        framePanel.add(frameSizeField, "w 100!, wrap");
        framePanel.add(interFrameDelayLabel, "right");
        framePanel.add(interFrameDelayField, "w 100!, wrap");
        add(framePanel, "span, growx, wrap");

        JPanel retryPanel = new JPanel(new MigLayout("insets 8, gap 4", "[][grow]", ""));
        retryPanel.setBackground(Color.WHITE);
        retryPanel.setBorder(new TitledBorder("Retry & Backoff (Extra Feature)"));
        maxRetriesLabel = new JLabel("Max Retries:");
        maxRetriesField = new MirthTextField(); maxRetriesField.setText("3");
        expBackoffBox = new MirthCheckBox("Exponential Backoff");
        expBackoffBox.setSelected(true);
        expBackoffBox.setBackground(Color.WHITE);
        baseDelayLabel = new JLabel("Base Delay (ms):");
        baseDelayField = new MirthTextField(); baseDelayField.setText("100");
        maxDelayLabel = new JLabel("Max Delay (ms):");
        maxDelayField = new MirthTextField(); maxDelayField.setText("2000");
        retryPanel.add(maxRetriesLabel, "right");
        retryPanel.add(maxRetriesField, "w 100!, wrap");
        retryPanel.add(expBackoffBox, "span 2, wrap");
        retryPanel.add(baseDelayLabel, "right");
        retryPanel.add(baseDelayField, "w 100!, wrap");
        retryPanel.add(maxDelayLabel, "right");
        retryPanel.add(maxDelayField, "w 100!, wrap");
        add(retryPanel, "span, growx, wrap");

        JPanel timeoutPanel = new JPanel(new MigLayout("insets 8, gap 4", "[][grow]", ""));
        timeoutPanel.setBackground(Color.WHITE);
        timeoutPanel.setBorder(new TitledBorder("Timeouts (Extra Feature)"));
        frameTimeoutLabel = new JLabel("Frame Timeout (ms):");
        frameTimeoutField = new MirthTextField(); frameTimeoutField.setText("5000");
        eotTimeoutLabel = new JLabel("EOT Timeout (ms):");
        eotTimeoutField = new MirthTextField(); eotTimeoutField.setText("2000");
        sessionTimeoutLabel = new JLabel("Session Timeout (ms):");
        sessionTimeoutField = new MirthTextField(); sessionTimeoutField.setText("30000");
        timeoutPanel.add(frameTimeoutLabel, "right");
        timeoutPanel.add(frameTimeoutField, "w 100!, wrap");
        timeoutPanel.add(eotTimeoutLabel, "right");
        timeoutPanel.add(eotTimeoutField, "w 100!, wrap");
        timeoutPanel.add(sessionTimeoutLabel, "right");
        timeoutPanel.add(sessionTimeoutField, "w 100!, wrap");
        add(timeoutPanel, "span, growx, wrap");

        JPanel dialectPanel = new JPanel(new MigLayout("insets 8, gap 4", "[][grow]", ""));
        dialectPanel.setBackground(Color.WHITE);
        dialectPanel.setBorder(new TitledBorder("Dialect (Extra Feature)"));
        dialectLabel = new JLabel("Dialect:");
        dialectBox = new MirthComboBox();
        dialectBox.setModel(new DefaultComboBoxModel<>(new String[]{"LIS02-A", "LIS01-A", "VENDOR_CUSTOM"}));
        intermediateBox = new MirthCheckBox("Use Intermediate Records (C-records)");
        intermediateBox.setSelected(true);
        intermediateBox.setBackground(Color.WHITE);
        dialectPanel.add(dialectLabel, "right");
        dialectPanel.add(dialectBox, "w 150!, wrap");
        dialectPanel.add(intermediateBox, "span 2, wrap");
        add(dialectPanel, "span, growx, wrap");

        JPanel keepalivePanel = new JPanel(new MigLayout("insets 8, gap 4", "[][grow]", ""));
        keepalivePanel.setBackground(Color.WHITE);
        keepalivePanel.setBorder(new TitledBorder("Keepalive (Extra Feature)"));
        keepaliveBox = new MirthCheckBox("Enable Keepalive");
        keepaliveBox.setBackground(Color.WHITE);
        keepaliveIntervalLabel = new JLabel("Interval (ms):");
        keepaliveIntervalField = new MirthTextField(); keepaliveIntervalField.setText("60000");
        keepalivePanel.add(keepaliveBox, "wrap");
        keepalivePanel.add(keepaliveIntervalLabel, "right");
        keepalivePanel.add(keepaliveIntervalField, "w 100!, wrap");
        add(keepalivePanel, "span, growx, wrap");

        JPanel logPanel = new JPanel(new MigLayout("insets 8, gap 4", "[][grow]", ""));
        logPanel.setBackground(Color.WHITE);
        logPanel.setBorder(new TitledBorder("Protocol Analyzer (Extra Feature)"));
        protocolLogBox = new MirthCheckBox("Enable Protocol Logging");
        protocolLogBox.setBackground(Color.WHITE);
        maxLogLabel = new JLabel("Max Entries:");
        maxLogField = new MirthTextField(); maxLogField.setText("1000");
        logPanel.add(protocolLogBox, "wrap");
        logPanel.add(maxLogLabel, "right");
        logPanel.add(maxLogField, "w 100!, wrap");
        add(logPanel, "span, growx, wrap");
    }

    @Override
    public TransmissionModeProperties getProperties() {
        AstmE1381ModeProperties p = new AstmE1381ModeProperties();
        p.setUseEnqAck(enqAckBox.isSelected());
        try { p.setEnqTimeout(Integer.parseInt(enqTimeoutField.getText())); } catch (Exception ignored) {}
        try { p.setAckTimeout(Integer.parseInt(ackTimeoutField.getText())); } catch (Exception ignored) {}
        p.setUseChecksum(checksumBox.isSelected());
        p.setChecksumAlgorithm((String) checksumAlgoBox.getSelectedItem());
        try { p.setMaxFrameSize(Integer.parseInt(frameSizeField.getText())); } catch (Exception ignored) {}
        try { p.setInterFrameDelay(Integer.parseInt(interFrameDelayField.getText())); } catch (Exception ignored) {}
        try { p.setMaxRetries(Integer.parseInt(maxRetriesField.getText())); } catch (Exception ignored) {}
        p.setUseExponentialBackoff(expBackoffBox.isSelected());
        try { p.setBaseRetryDelay(Integer.parseInt(baseDelayField.getText())); } catch (Exception ignored) {}
        try { p.setMaxRetryDelay(Integer.parseInt(maxDelayField.getText())); } catch (Exception ignored) {}
        try { p.setFrameTimeout(Integer.parseInt(frameTimeoutField.getText())); } catch (Exception ignored) {}
        try { p.setEotTimeout(Integer.parseInt(eotTimeoutField.getText())); } catch (Exception ignored) {}
        try { p.setSessionTimeout(Integer.parseInt(sessionTimeoutField.getText())); } catch (Exception ignored) {}
        p.setDialect((String) dialectBox.getSelectedItem());
        p.setUseIntermediateRecords(intermediateBox.isSelected());
        p.setEnableKeepalive(keepaliveBox.isSelected());
        try { p.setKeepaliveInterval(Integer.parseInt(keepaliveIntervalField.getText())); } catch (Exception ignored) {}
        p.setEnableProtocolLogging(protocolLogBox.isSelected());
        try { p.setMaxProtocolLogSize(Integer.parseInt(maxLogField.getText())); } catch (Exception ignored) {}
        return p;
    }

    @Override
    public void setProperties(TransmissionModeProperties properties) {
        if (properties instanceof AstmE1381ModeProperties) {
            AstmE1381ModeProperties p = (AstmE1381ModeProperties) properties;
            enqAckBox.setSelected(p.isUseEnqAck());
            enqTimeoutField.setText(String.valueOf(p.getEnqTimeout()));
            ackTimeoutField.setText(String.valueOf(p.getAckTimeout()));
            checksumBox.setSelected(p.isUseChecksum());
            checksumAlgoBox.setSelectedItem(p.getChecksumAlgorithm());
            frameSizeField.setText(String.valueOf(p.getMaxFrameSize()));
            interFrameDelayField.setText(String.valueOf(p.getInterFrameDelay()));
            maxRetriesField.setText(String.valueOf(p.getMaxRetries()));
            expBackoffBox.setSelected(p.isUseExponentialBackoff());
            baseDelayField.setText(String.valueOf(p.getBaseRetryDelay()));
            maxDelayField.setText(String.valueOf(p.getMaxRetryDelay()));
            frameTimeoutField.setText(String.valueOf(p.getFrameTimeout()));
            eotTimeoutField.setText(String.valueOf(p.getEotTimeout()));
            sessionTimeoutField.setText(String.valueOf(p.getSessionTimeout()));
            dialectBox.setSelectedItem(p.getDialect());
            intermediateBox.setSelected(p.isUseIntermediateRecords());
            keepaliveBox.setSelected(p.isEnableKeepalive());
            keepaliveIntervalField.setText(String.valueOf(p.getKeepaliveInterval()));
            protocolLogBox.setSelected(p.isEnableProtocolLogging());
            maxLogField.setText(String.valueOf(p.getMaxProtocolLogSize()));
        }
    }

    @Override
    public TransmissionModeProperties getDefaults() {
        return new AstmE1381ModeProperties();
    }

    @Override
    public boolean checkProperties(TransmissionModeProperties properties, boolean highlight) {
        return true;
    }

    @Override
    public void resetInvalidProperties() {}
}