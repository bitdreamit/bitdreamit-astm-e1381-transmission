package com.bitdreamit.connect.plugins.transmission.astm.shared;

/**
 * Raised when an ASTM E1381 frame cannot be decoded or its checksum fails.
 *
 * <p>This is a checked exception because frame failures are recoverable at the
 * protocol layer: the receiver typically replies with NAK and asks the sender
 * to retry the frame.</p>
 */
public class ASTME1381FrameException extends Exception {

    private static final long serialVersionUID = 1L;

    public ASTME1381FrameException(String message) {
        super(message);
    }

    public ASTME1381FrameException(String message, Throwable cause) {
        super(message, cause);
    }
}
