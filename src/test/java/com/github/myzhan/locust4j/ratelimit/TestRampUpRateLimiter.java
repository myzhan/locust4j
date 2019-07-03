package com.github.myzhan.locust4j.ratelimit;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestRampUpRateLimiter {

    @Test
    public void TestAcquire() throws Exception {
        AbstractRateLimiter abstractRateLimiter = new RampUpRateLimiter(3, 1, 290, TimeUnit.MILLISECONDS,
                300, TimeUnit.MILLISECONDS);
        abstractRateLimiter.start();

        Thread.sleep(20);

        assertFalse(abstractRateLimiter.acquire());
        // running out of permits, acquire is blocked.
        assertTrue(abstractRateLimiter.acquire());

        assertFalse(abstractRateLimiter.acquire());
        assertFalse(abstractRateLimiter.acquire());
        // running out of permits, acquire is blocked.
        assertTrue(abstractRateLimiter.acquire());

        assertFalse(abstractRateLimiter.acquire());
        assertFalse(abstractRateLimiter.acquire());
        assertFalse(abstractRateLimiter.acquire());
        // running out of permits, acquire is blocked.
        assertTrue(abstractRateLimiter.acquire());

        assertFalse(abstractRateLimiter.acquire());
        assertFalse(abstractRateLimiter.acquire());
        assertFalse(abstractRateLimiter.acquire());
        // running out of permits, acquire is blocked.
        assertTrue(abstractRateLimiter.acquire());

        abstractRateLimiter.stop();
        assertTrue(abstractRateLimiter.isStopped());
    }
}
