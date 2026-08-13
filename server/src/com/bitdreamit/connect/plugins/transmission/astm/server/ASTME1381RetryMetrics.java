package com.bitdreamit.connect.plugins.transmission.astm.server;

import java.util.concurrent.atomic.AtomicInteger;

public class ASTME1381RetryMetrics {
    private final AtomicInteger frameRetries = new AtomicInteger(0);
    private final AtomicInteger nakCount = new AtomicInteger(0);
    private final AtomicInteger enqRetries = new AtomicInteger(0);

    public void incrementFrameRetry() { frameRetries.incrementAndGet(); }
    public void incrementNak() { nakCount.incrementAndGet(); }
    public void incrementEnqRetry() { enqRetries.incrementAndGet(); }

    public int getFrameRetries() { return frameRetries.get(); }
    public int getNakCount() { return nakCount.get(); }
    public int getEnqRetries() { return enqRetries.get(); }

    public void reset() {
        frameRetries.set(0);
        nakCount.set(0);
        enqRetries.set(0);
    }
}
