package com.github.myzhan.locust4j;

import com.github.myzhan.locust4j.ratelimit.AbstractRateLimiter;
import com.github.myzhan.locust4j.ratelimit.StableRateLimiter;
import com.github.myzhan.locust4j.stats.RequestFailure;
import com.github.myzhan.locust4j.stats.RequestSuccess;
import com.github.myzhan.locust4j.stats.Stats;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author myzhan
 * @date 2018/12/24
 */
public class TestLocust {

    class TestTask extends AbstractTask {

        private int weight;
        private String name;

        public TestTask(int weight, String name) {
            this.weight = weight;
            this.name = name;
        }

        @Override
        public int getWeight() {
            return weight;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void execute() {
        }

    }

    @Test
    public void TestDefaultRateLimiter() {
        Locust.getInstance().setMaxRPS(1000);
        AbstractRateLimiter rateLimiter = Locust.getInstance().getRateLimiter();

        Assert.assertTrue(Locust.getInstance().isRateLimitEnabled());

        // defaults to StableRateLimiter
        Assert.assertTrue(rateLimiter instanceof StableRateLimiter);
    }

    @Test
    public void TestRecordSuccess() {
        Locust.getInstance().recordSuccess("http", "success", 1 ,10);
        RequestSuccess success = Stats.getInstance().getReportSuccessQueue().poll();
        Assert.assertEquals("http", success.getRequestType());
        Assert.assertEquals("success", success.getName());
        Assert.assertEquals(1, success.getResponseTime());
        Assert.assertEquals(10, success.getContentLength());
    }

    @Test
    public void TestRecordFailure() {
        Locust.getInstance().recordFailure("http", "failure", 1 ,"error");
        RequestFailure failure = Stats.getInstance().getReportFailureQueue().poll();
        Assert.assertEquals("http", failure.getRequestType());
        Assert.assertEquals("failure", failure.getName());
        Assert.assertEquals(1, failure.getResponseTime());
        Assert.assertEquals("error", failure.getError());
    }
}
