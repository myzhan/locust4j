package com.github.myzhan.locust4j.ratelimit;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author myzhan
 */
public class TestStableRateLimiter {

    @Test
    public void TestAcquire() throws Exception {
        AbstractRateLimiter abstractRateLimiter = new StableRateLimiter(2);
        abstractRateLimiter.start();

        Thread.sleep(10);

        assertFalse(abstractRateLimiter.acquire());
        assertFalse(abstractRateLimiter.acquire());

        // running out of permits, acquire is blocked.
        assertTrue(abstractRateLimiter.acquire());

        // bucket is updated, acquire won't be blocked
        assertFalse(abstractRateLimiter.acquire());

        abstractRateLimiter.stop();

        assertTrue(abstractRateLimiter.isStopped());
    }
}
