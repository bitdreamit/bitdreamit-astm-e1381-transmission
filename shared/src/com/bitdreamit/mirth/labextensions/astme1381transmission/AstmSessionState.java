/*
 * BitDreamIT Mirth Lab Extensions
 * Copyright (c) 2026 Kimi AI (Moonshot AI) — MIT License
 */
package com.bitdreamit.mirth.labextensions.astme1381transmission;

/**
 * ASTM E1381 session state machine states.
 */
public enum AstmSessionState {
    IDLE,
    ENQ_SENT,
    ACK_WAIT,
    TRANSFER,
    EOT_WAIT,
    COMPLETE,
    ERROR
}