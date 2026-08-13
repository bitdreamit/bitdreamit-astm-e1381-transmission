package com.bitdreamit.connect.plugins.transmission.astm.shared;

public final class ASTME1381Constants {
    public static final byte ENQ = 0x05;
    public static final byte ACK = 0x06;
    public static final byte NAK = 0x15;
    public static final byte STX = 0x02;
    public static final byte ETX = 0x03;
    public static final byte ETB = 0x17;
    public static final byte EOT = 0x04;
    public static final byte CR  = 0x0D;
    public static final byte LF  = 0x0A;
    public static final int MAX_FRAME_TEXT_LENGTH = 240;
    public static final String PLUGIN_POINT_NAME = "ASTM E1381 Transmission";
    public static final String PLUGIN_NAME = "bitdreamit-astm-e1381-transmission";
    private ASTME1381Constants() {}
}
