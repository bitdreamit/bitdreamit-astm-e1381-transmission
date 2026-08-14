package com.bitdreamit.connect.plugins.transmission.astm.test;

import com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381RetryMetrics;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Verifies the thread-safe counters exposed by {@link ASTME1381RetryMetrics}.
 */
public class ASTME1381RetryMetricsTest {

    @Test
    public void testFreshMetricsAreZero() {
        ASTME1381RetryMetrics m = new ASTME1381RetryMetrics();
        assertEquals(0, m.getFrameRetries());
        assertEquals(0, m.getNakCount());
        assertEquals(0, m.getEnqRetries());
        assertEquals(0, m.getFramesSent());
        assertEquals(0, m.getFramesReceived());
    }

    @Test
    public void testIncrementCounters() {
        ASTME1381RetryMetrics m = new ASTME1381RetryMetrics();
        m.incrementFrameRetry();
        m.incrementFrameRetry();
        m.incrementNak();
        m.incrementEnqRetry();
        m.incrementFramesSent();
        m.incrementFramesSent();
        m.incrementFramesSent();
        m.incrementFramesReceived();

        assertEquals(2, m.getFrameRetries());
        assertEquals(1, m.getNakCount());
        assertEquals(1, m.getEnqRetries());
        assertEquals(3, m.getFramesSent());
        assertEquals(1, m.getFramesReceived());
    }

    @Test
    public void testReset() {
        ASTME1381RetryMetrics m = new ASTME1381RetryMetrics();
        m.incrementFrameRetry();
        m.incrementNak();
        m.incrementEnqRetry();
        m.incrementFramesSent();
        m.incrementFramesReceived();

        m.reset();

        assertEquals(0, m.getFrameRetries());
        assertEquals(0, m.getNakCount());
        assertEquals(0, m.getEnqRetries());
        assertEquals(0, m.getFramesSent());
        assertEquals(0, m.getFramesReceived());
    }

    @Test
    public void testMarkSessionStartSetsTimestamp() throws InterruptedException {
        ASTME1381RetryMetrics m = new ASTME1381RetryMetrics();
        long before = System.currentTimeMillis();
        m.markSessionStart();
        long after = System.currentTimeMillis();
        assertTrue(m.getSessionStartedAt() >= before);
        assertTrue(m.getSessionStartedAt() <= after);
    }

    @Test
    public void testToStringContainsCounters() {
        ASTME1381RetryMetrics m = new ASTME1381RetryMetrics();
        m.incrementFramesSent();
        m.incrementNak();
        String s = m.toString();
        assertTrue(s.contains("sent=1"));
        assertTrue(s.contains("naks=1"));
        assertTrue(s.contains("ASTME1381RetryMetrics"));
    }

    @Test
    public void testConcurrentIncrementsAreSafe() throws InterruptedException {
        final ASTME1381RetryMetrics m = new ASTME1381RetryMetrics();
        final int N_THREADS = 8;
        final int N_OPS = 1000;
        Thread[] threads = new Thread[N_THREADS];
        for (int i = 0; i < N_THREADS; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < N_OPS; j++) {
                    m.incrementFrameRetry();
                    m.incrementNak();
                    m.incrementEnqRetry();
                }
            });
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        assertEquals(N_THREADS * N_OPS, m.getFrameRetries());
        assertEquals(N_THREADS * N_OPS, m.getNakCount());
        assertEquals(N_THREADS * N_OPS, m.getEnqRetries());
    }
}
