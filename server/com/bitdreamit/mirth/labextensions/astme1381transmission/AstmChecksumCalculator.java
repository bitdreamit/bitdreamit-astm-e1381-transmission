/*
 * BitDreamIT Mirth Lab Extensions
 * Copyright (c) 2026 Kimi AI (Moonshot AI) — MIT License
 */
package com.bitdreamit.mirth.labextensions.astme1381transmission;

import java.util.zip.CRC8;

/**
 * Multiple checksum algorithms for ASTM E1381.
 * Extra feature: supports LRC, CRC-8, and custom algorithms beyond standard sum-mod-256.
 */
public class AstmChecksumCalculator {

    public static String calculate(byte[] data, int offset, int length, String algorithm) {
        switch (algorithm) {
            case "LRC":
                return calculateLrc(data, offset, length);
            case "CRC8":
                return calculateCrc8(data, offset, length);
            case "CUSTOM":
                return calculateCustom(data, offset, length);
            case "SUM_MOD_256":
            default:
                return calculateSumMod256(data, offset, length);
        }
    }

    private static String calculateSumMod256(byte[] data, int offset, int length) {
        int sum = 0;
        for (int i = offset; i < offset + length; i++) {
            sum += data[i] & 0xFF;
        }
        return String.format("%02X", sum & 0xFF);
    }

    private static String calculateLrc(byte[] data, int offset, int length) {
        int lrc = 0;
        for (int i = offset; i < offset + length; i++) {
            lrc ^= data[i] & 0xFF;
        }
        return String.format("%02X", lrc & 0xFF);
    }

    private static String calculateCrc8(byte[] data, int offset, int length) {
        int crc = 0xFF;
        for (int i = offset; i < offset + length; i++) {
            crc ^= data[i] & 0xFF;
            for (int j = 0; j < 8; j++) {
                crc = (crc & 0x80) != 0 ? ((crc << 1) ^ 0x31) & 0xFF : (crc << 1) & 0xFF;
            }
        }
        return String.format("%02X", crc & 0xFF);
    }

    private static String calculateCustom(byte[] data, int offset, int length) {
        // Vendor-specific: XOR all bytes then add 1
        int sum = 1;
        for (int i = offset; i < offset + length; i++) {
            sum ^= data[i] & 0xFF;
        }
        return String.format("%02X", sum & 0xFF);
    }
}