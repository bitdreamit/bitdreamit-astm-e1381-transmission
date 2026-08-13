/*
 * BitDreamIT Mirth Lab Extensions
 * Copyright (c) 2026 Kimi AI (Moonshot AI) — MIT License
 */
package com.bitdreamit.mirth.labextensions.astme1381transmission;

import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Protocol packet capture logger.
 * Extra feature beyond commercial extension.
 */
public class AstmProtocolLogger {
    private static final Logger logger = Logger.getLogger(AstmProtocolLogger.class);
    private static final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss.SSS");

    public enum Direction { TX, RX, EVENT }

    public static class Entry {
        public final String time;
        public final Direction dir;
        public final String hex;
        public final String ascii;
        public final String note;

        public Entry(Direction dir, byte[] data, String note) {
            this.time = fmt.format(new Date());
            this.dir = dir;
            this.note = note;
            StringBuilder h = new StringBuilder();
            StringBuilder a = new StringBuilder();
            for (byte b : data) {
                h.append(String.format("%02X ", b & 0xFF));
                char c = (char) (b & 0xFF);
                a.append(c >= 32 && c < 127 ? c : '.');
            }
            this.hex = h.toString().trim();
            this.ascii = a.toString();
        }
    }

    private final List<Entry> entries = Collections.synchronizedList(new ArrayList<>());
    private final int maxSize;

    public AstmProtocolLogger(int maxSize) {
        this.maxSize = maxSize;
    }

    public void log(Direction dir, byte[] data, String note) {
        if (data == null) data = new byte[0];
        Entry e = new Entry(dir, data, note);
        entries.add(e);
        while (entries.size() > maxSize) entries.remove(0);
        logger.info(String.format("[ASTM-PROTO] %s %s: %s", e.time, dir, note));
    }

    public List<Entry> getEntries() { return new ArrayList<>(entries); }
    public void clear() { entries.clear(); }
}