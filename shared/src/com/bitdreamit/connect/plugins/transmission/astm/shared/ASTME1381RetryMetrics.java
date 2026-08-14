package com.bitdreamit.connect.plugins.transmission.astm.shared;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe operational counters for an ASTM E1381 session.
 *
 * <p>These counters are intended for monitoring dashboards (Prometheus, JMX,
 * Mirth channel statistics). They cover the three retry dimensions that the
 * protocol exposes:</p>
 * <ul>
 *   <li>{@code frameRetries} - retransmissions of data frames due to NAK / timeout</li>
 *   <li>{@code nakCount}     - total NAKs sent or received</li>
 *   <li>{@code enqRetries}   - retransmissions of the ENQ establishment probe</li>
 * </ul>
 *
 * <p>The class is intentionally lock-free and safe to publish across threads.</p>
 */
public class ASTME1381RetryMetrics {

    private final AtomicInteger frameRetries = new AtomicInteger(0);
    private final AtomicInteger nakCount     = new AtomicInteger(0);
    private final AtomicInteger enqRetries   = new AtomicInteger(0);
    private final AtomicInteger framesSent   = new AtomicInteger(0);
    private final AtomicInteger framesReceived = new AtomicInteger(0);
    private final AtomicLong    sessionStartedAt = new AtomicLong(0L);

    public void incrementFrameRetry()   { frameRetries.incrementAndGet(); }
    public void incrementNak()           { nakCount.incrementAndGet(); }
    public void incrementEnqRetry()      { enqRetries.incrementAndGet(); }
    public void incrementFramesSent()    { framesSent.incrementAndGet(); }
    public void incrementFramesReceived(){ framesReceived.incrementAndGet(); }

    public int getFrameRetries()   { return frameRetries.get(); }
    public int getNakCount()       { return nakCount.get(); }
    public int getEnqRetries()     { return enqRetries.get(); }
    public int getFramesSent()     { return framesSent.get(); }
    public int getFramesReceived() { return framesReceived.get(); }

    public long getSessionStartedAt()  { return sessionStartedAt.get(); }

    /** Mark the session as (re)started at the current wall-clock time. */
    public void markSessionStart() {
        sessionStartedAt.set(System.currentTimeMillis());
    }

    /**
     * Atomically reset all counters. Called when a new session starts so the
     * dashboard reflects per-session state.
     */
    public void reset() {
        frameRetries.set(0);
        nakCount.set(0);
        enqRetries.set(0);
        framesSent.set(0);
        framesReceived.set(0);
        sessionStartedAt.set(System.currentTimeMillis());
    }

    @Override
    public String toString() {
        return "ASTME1381RetryMetrics{sent=" + framesSent.get()
                + ", recv=" + framesReceived.get()
                + ", retries=" + frameRetries.get()
                + ", naks=" + nakCount.get()
                + ", enqRetries=" + enqRetries.get()
                + '}';
    }
}
