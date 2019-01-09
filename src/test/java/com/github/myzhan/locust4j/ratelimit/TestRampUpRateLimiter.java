package com.github.myzhan.locust4j.ratelimit;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class TestRampUpRateLimiter {

    @Test
    public void TestAcquire() throws Exception {
        AbstractRateLimiter abstractRateLimiter = new RampUpRateLimiter(3, 1, 300, TimeUnit.MILLISECONDS,
                300, TimeUnit.MILLISECONDS);
        abstractRateLimiter.start();

        Thread.sleep(20);

        Assert.assertFalse(abstractRateLimiter.acquire());
        // running out of permits, acquire is blocked.
        Assert.assertTrue(abstractRateLimiter.acquire());

        Assert.assertFalse(abstractRateLimiter.acquire());
        Assert.assertFalse(abstractRateLimiter.acquire());
        // running out of permits, acquire is blocked.
        Assert.assertTrue(abstractRateLimiter.acquire());

        Assert.assertFalse(abstractRateLimiter.acquire());
        Assert.assertFalse(abstractRateLimiter.acquire());
        Assert.assertFalse(abstractRateLimiter.acquire());
        // running out of permits, acquire is blocked.
        Assert.assertTrue(abstractRateLimiter.acquire());

        Assert.assertFalse(abstractRateLimiter.acquire());
        Assert.assertFalse(abstractRateLimiter.acquire());
        Assert.assertFalse(abstractRateLimiter.acquire());
        // running out of permits, acquire is blocked.
        Assert.assertTrue(abstractRateLimiter.acquire());

        abstractRateLimiter.stop();
        Assert.assertTrue(abstractRateLimiter.isStopped());
    }
}
