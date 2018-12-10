package com.github.myzhan.locust4j.ratelimit;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author myzhan
 * @date 2018/12/10
 */
public class TestStableRateLimiter {

    @Test
    public void TestAcquire() throws Exception {
        RateLimiter rateLimiter = new StableRateLimiter(2);
        rateLimiter.start();

        Assert.assertEquals(false, rateLimiter.acquire());
        Assert.assertEquals(false, rateLimiter.acquire());

        // running out of permits, acquire is blocked.
        Assert.assertEquals(true, rateLimiter.acquire());

        Thread.sleep(1010);

        Assert.assertEquals(false, rateLimiter.acquire());

        rateLimiter.stop();

        Assert.assertEquals(true, rateLimiter.isStopped());
    }
}
