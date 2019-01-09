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
        AbstractRateLimiter abstractRateLimiter = new StableRateLimiter(2);
        abstractRateLimiter.start();

        Thread.sleep(10);

        Assert.assertFalse(abstractRateLimiter.acquire());
        Assert.assertFalse(abstractRateLimiter.acquire());

        // running out of permits, acquire is blocked.
        Assert.assertTrue(abstractRateLimiter.acquire());

        // bucket is updated, acquire won't be blocked
        Assert.assertFalse(abstractRateLimiter.acquire());

        abstractRateLimiter.stop();

        Assert.assertTrue(abstractRateLimiter.isStopped());
    }
}
