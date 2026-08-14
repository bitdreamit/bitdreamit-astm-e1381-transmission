package com.bitdreamit.mirth.astm.e1381.shared;

/**
 * ASTM E1381-95 Lower Layer Protocol Constants
 * Production-grade constants for frame-level communication
 */
public final class ASTME1381Constants {
    private ASTME1381Constants() {}

    // Control Characters (ASTM E1381 standard)
    public static final byte ENQ = 0x05;   // Enquiry
    public static final byte ACK = 0x06;   // Positive Acknowledge
    public static final byte NAK = 0x15;   // Negative Acknowledge
    public static final byte STX = 0x02;   // Start of Text
    public static final byte ETX = 0x03;   // End of Text
    public static final byte ETB = 0x17;   // End of Transmission Block (intermediate frame)
    public static final byte EOT = 0x04;   // End of Transmission
    public static final byte CR  = 0x0D;   // Carriage Return
    public static final byte LF  = 0x0A;   // Line Feed
    public static final byte SUB = 0x1A;   // Substitute (cancel)

    // Default timing values (milliseconds)
    public static final int DEFAULT_ESTABLISHMENT_TIMEOUT = 15000;
    public static final int DEFAULT_CONTENTION_TIMEOUT    = 20000;
    public static final int DEFAULT_FRAME_TIMEOUT         = 30000;
    public static final int DEFAULT_RESPONSE_TIMEOUT      = 15000;
    public static final int DEFAULT_MAX_TRANSFER_ATTEMPTS = 6;

    // Default frame parameters
    public static final int  DEFAULT_MAX_FRAME_CONTENT_LENGTH = 240;
    public static final int  DEFAULT_CHECKSUM_BYTE_LENGTH     = 2;
    public static final byte DEFAULT_FRAME_SEQUENCE_START     = 1;

    // Checksum algorithms
    public static final String CHECKSUM_ADD_MOD_256 = "Add Mod 256";
    public static final String CHECKSUM_XOR         = "XOR";
    public static final String CHECKSUM_NONE        = "None";

    // Plugin name
    public static final String PLUGIN_NAME = "ASTM E1381";
    public static final String PLUGIN_VERSION = "1.0.0";
}
