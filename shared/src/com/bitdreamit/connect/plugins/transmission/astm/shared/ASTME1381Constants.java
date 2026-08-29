package com.bitdreamit.connect.plugins.transmission.astm.shared;

/**
 * ASTM E1381-02 Lower-Layer Protocol Constants.
 *
 * <p>These constants capture the control characters, default timing values,
 * frame parameters and checksum algorithm identifiers defined by the
 * ASTM E1381-02 standard. They are used by the shared, server and client
 * modules of the bitdreamit-astm-e1381-transmission Mirth Connect extension.</p>
 *
 * <p>This class is not instantiable.</p>
 */
public final class ASTME1381Constants {

    private ASTME1381Constants() {
        // utility class - no instances
    }

    // ------------------------------------------------------------------
    // Control Characters (ASTM E1381-02)
    // ------------------------------------------------------------------
    public static final byte ENQ = 0x05;   // Enquiry
    public static final byte ACK = 0x06;   // Positive Acknowledge
    public static final byte NAK = 0x15;   // Negative Acknowledge
    public static final byte STX = 0x02;   // Start of Text
    public static final byte ETX = 0x03;   // End of Text (final frame terminator)
    public static final byte ETB = 0x17;   // End of Transmission Block (intermediate frame)
    public static final byte EOT = 0x04;   // End of Transmission
    public static final byte CR  = 0x0D;   // Carriage Return
    public static final byte LF  = 0x0A;   // Line Feed
    public static final byte SUB = 0x1A;   // Substitute (abort/cancel)

    // ------------------------------------------------------------------
    // Default timing values (milliseconds) - per ASTM E1381-02
    // ------------------------------------------------------------------
    /** Maximum time the receiver waits for an ENQ before declaring line idle. */
    public static final int DEFAULT_ESTABLISHMENT_TIMEOUT = 15000;
    /** Time the sender/receiver waits to resolve line contention. */
    public static final int DEFAULT_CONTENTION_TIMEOUT    = 20000;
    /** Maximum time to wait for a complete frame to arrive once started. */
    public static final int DEFAULT_FRAME_TIMEOUT         = 30000;
    /** Maximum time to wait for an ACK/NAK after sending a frame. */
    public static final int DEFAULT_RESPONSE_TIMEOUT      = 15000;
    /** Default number of full transfer retries before giving up. */
    public static final int DEFAULT_MAX_TRANSFER_ATTEMPTS = 6;

    // ------------------------------------------------------------------
    // Frame parameters
    // ------------------------------------------------------------------
    /** Maximum number of payload bytes per frame (excludes FN, terminator, checksum). */
    public static final int  DEFAULT_MAX_FRAME_CONTENT_LENGTH = 240;
    /** Maximum payload length kept for backwards compatibility. */
    public static final int  MAX_FRAME_TEXT_LENGTH            = DEFAULT_MAX_FRAME_CONTENT_LENGTH;
    /** Number of bytes used to encode the checksum (two hex chars). */
    public static final int  DEFAULT_CHECKSUM_BYTE_LENGTH     = 2;
    /** Default first frame number when sequencing is enabled (1-7,0). */
    public static final byte DEFAULT_FRAME_SEQUENCE_START     = 1;

    // ------------------------------------------------------------------
    // Retry / establishment defaults used by the new-style providers
    // ------------------------------------------------------------------
    /** Default number of ENQ retries before declaring establishment failure. */
    public static final int DEFAULT_MAX_ENQ_RETRIES    = 6;
    /** Default per-frame ACK retry count. */
    public static final int DEFAULT_MAX_FRAME_RETRIES  = 6;
    /** Default ACK timeout for an ENQ handshake (ms). */
    public static final int DEFAULT_ENQ_TIMEOUT_MS     = DEFAULT_RESPONSE_TIMEOUT;
    /** Default ACK timeout for a single data frame (ms). */
    public static final int DEFAULT_FRAME_ACK_TIMEOUT_MS = DEFAULT_RESPONSE_TIMEOUT;
    /** Backoff base used for exponential ENQ retry (ms). */
    public static final long DEFAULT_ENQ_BACKOFF_BASE_MS = 500L;
    /** Maximum ENQ backoff cap (ms). */
    public static final long DEFAULT_ENQ_BACKOFF_CAP_MS  = 8000L;

    // ------------------------------------------------------------------
    // Checksum algorithms
    // ------------------------------------------------------------------
    public static final String CHECKSUM_ADD_MOD_256 = "Add Mod 256";
    public static final String CHECKSUM_XOR         = "XOR";
    public static final String CHECKSUM_NONE        = "None";

    // ------------------------------------------------------------------
    // Plugin identity
    // ------------------------------------------------------------------
    public static final String PLUGIN_NAME    = "ASTM E1381";
    /** Legacy alias for {@link #PLUGIN_NAME} - retained for source-level compatibility. */
    public static final String PLUGIN_POINT_NAME = PLUGIN_NAME;
    public static final String PLUGIN_VERSION = "1.3.5";
}
