package com.github.myzhan.locust4j.stats;

import java.util.Map;

import com.github.myzhan.locust4j.utils.Utils;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author myzhan
 * @date 2017/12/06
 */
public class TestStats {

    @Test
    public void TestAll() throws Exception {
        Stats stats = Stats.getInstance();
        stats.start();

        RequestSuccess success = new RequestSuccess();
        success.setRequestType("http");
        success.setName("success");
        success.setResponseTime(1);
        success.setContentLength(10);
        stats.getReportSuccessQueue().add(success);

        RequestFailure failure = new RequestFailure();
        failure.setRequestType("http");
        failure.setName("failure");
        failure.setResponseTime(1000);
        failure.setError("timeout");
        stats.getReportFailureQueue().add(failure);

        stats.wakeMeUp();
        Thread.sleep(3100);
        stats.wakeMeUp();

        Map dataToRunner = stats.getMessageToRunnerQueue().take();

        Assert.assertTrue(dataToRunner.containsKey("stats"));
        Assert.assertTrue(dataToRunner.containsKey("stats_total"));
        Assert.assertTrue(dataToRunner.containsKey("errors"));

        stats.stop();
    }

    @Test
    public void TestClearAll() throws Exception {
        Stats stats = Stats.getInstance();
        stats.start();

        RequestSuccess success = new RequestSuccess();
        success.setRequestType("http");
        success.setName("success");
        success.setResponseTime(1);
        success.setContentLength(10);
        stats.getReportSuccessQueue().add(success);

        RequestFailure failure = new RequestFailure();
        failure.setRequestType("http");
        failure.setName("failure");
        failure.setResponseTime(1000);
        failure.setError("timeout");
        stats.getReportFailureQueue().add(failure);

        stats.getClearStatsQueue().offer(true);
        stats.wakeMeUp();
        Thread.sleep(500);

        // stats is cleared in another thread
        Assert.assertEquals(0, stats.serializeStats().size());

        stats.stop();
    }

    @Test
    public void TestLogRequest() {
        Stats stats = Stats.getInstance();

        stats.logRequest("http", "test", 1000l, 2000l);
        stats.logRequest("http", "test", 2000l, 4000l);
        stats.logRequest("udp", "test", 300l, 300l);

        StatsEntry entry = stats.get("test", "http");

        Assert.assertEquals("test", entry.getName());
        Assert.assertEquals("http", entry.getMethod());
        Assert.assertEquals(2, entry.getNumRequests());
        Assert.assertEquals(0, entry.getNumFailures());
        Assert.assertEquals(1000l, entry.getMinResponseTime());
        Assert.assertEquals(2000l, entry.getMaxResponseTime());
        Assert.assertEquals(3000l, entry.getTotalResponseTime());
        Assert.assertEquals(6000l, entry.getTotalContentLength());
        Assert.assertEquals(1, (long)entry.getResponseTimes().get(1000l));
        Assert.assertEquals(1, (long)entry.getResponseTimes().get(2000l));

        StatsEntry total = stats.getTotal();

        Assert.assertEquals("Total", total.getName());
        Assert.assertEquals("", total.getMethod());
        Assert.assertEquals(3, total.getNumRequests());
        Assert.assertEquals(0, total.getNumFailures());
        Assert.assertEquals(300l, total.getMinResponseTime());
        Assert.assertEquals(2000l, total.getMaxResponseTime());
        Assert.assertEquals(3300l, total.getTotalResponseTime());
        Assert.assertEquals(6300l, total.getTotalContentLength());
        Assert.assertEquals(1, (long)total.getResponseTimes().get(300l));
        Assert.assertEquals(1, (long)total.getResponseTimes().get(1000l));
        Assert.assertEquals(1, (long)total.getResponseTimes().get(2000l));
    }

    @Test
    public void TestLogError() {
        Stats stats = Stats.getInstance();

        stats.logError("http", "test", "Test Error");
        stats.logError("udp", "test", "Unknown Error");

        StatsEntry entry = stats.get("test", "http");
        Assert.assertEquals(1, entry.getNumFailures());

        StatsEntry total = stats.getTotal();
        Assert.assertEquals(2, total.getNumFailures());

        Map<String, Map<String, Object>> errors = stats.serializeErrors();

        String httpKey = Utils.md5("http" + "test" + "Test Error");
        Map<String, Object> httpError = errors.get(httpKey);
        Assert.assertEquals("test", httpError.get("name"));
        Assert.assertEquals(1l, httpError.get("occurences"));
        Assert.assertEquals("http", httpError.get("method"));
        Assert.assertEquals("Test Error", httpError.get("error"));

        String udpKey = Utils.md5("udp" + "test" + "Unknown Error");
        Map<String, Object> udpError = errors.get(udpKey);
        Assert.assertEquals("test", udpError.get("name"));
        Assert.assertEquals(1l, udpError.get("occurences"));
        Assert.assertEquals("udp", udpError.get("method"));
        Assert.assertEquals("Unknown Error", udpError.get("error"));
    }
}
