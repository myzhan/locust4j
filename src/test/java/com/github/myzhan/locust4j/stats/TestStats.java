package com.github.myzhan.locust4j.stats;

import java.util.Map;

import com.github.myzhan.locust4j.utils.Utils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author myzhan
 */
public class TestStats {

    private Stats stats;

    @Before
    public void before() {
        stats = Stats.getInstance();
    }

    @Test
    public void TestAll() throws Exception {
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

        assertTrue(dataToRunner.containsKey("stats"));
        assertTrue(dataToRunner.containsKey("stats_total"));
        assertTrue(dataToRunner.containsKey("errors"));

        stats.stop();
    }

    @Test
    public void TestClearAll() throws Exception {
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
        assertEquals(0, stats.serializeStats().size());

        stats.stop();
    }

    @Test
    public void TestLogRequest() {
        stats.logRequest("http", "test", 1000L, 2000L);
        stats.logRequest("http", "test", 2000L, 4000L);
        stats.logRequest("udp", "test", 300L, 300L);

        StatsEntry entry = stats.get("test", "http");

        assertEquals("test", entry.getName());
        assertEquals("http", entry.getMethod());
        assertEquals(2, entry.getNumRequests());
        assertEquals(0, entry.getNumFailures());
        assertEquals(1000L, entry.getMinResponseTime());
        assertEquals(2000L, entry.getMaxResponseTime());
        assertEquals(3000L, entry.getTotalResponseTime());
        assertEquals(6000L, entry.getTotalContentLength());
        assertEquals(1, (long)entry.getResponseTimes().get(1000L));
        assertEquals(1, (long)entry.getResponseTimes().get(2000L));

        StatsEntry total = stats.getTotal();

        assertEquals("Total", total.getName());
        assertEquals("", total.getMethod());
        assertEquals(3, total.getNumRequests());
        assertEquals(0, total.getNumFailures());
        assertEquals(300L, total.getMinResponseTime());
        assertEquals(2000L, total.getMaxResponseTime());
        assertEquals(3300L, total.getTotalResponseTime());
        assertEquals(6300L, total.getTotalContentLength());
        assertEquals(1, (long)total.getResponseTimes().get(300L));
        assertEquals(1, (long)total.getResponseTimes().get(1000L));
        assertEquals(1, (long)total.getResponseTimes().get(2000L));
    }

    @Test
    public void TestLogError() {
        stats.logError("http", "test", "Test Error");
        stats.logError("udp", "test", "Unknown Error");

        StatsEntry entry = stats.get("test", "http");
        assertEquals(1, entry.getNumFailures());

        StatsEntry total = stats.getTotal();
        assertEquals(2, total.getNumFailures());

        Map<String, Map<String, Object>> errors = stats.serializeErrors();

        String httpKey = Utils.md5("http" + "test" + "Test Error");
        Map<String, Object> httpError = errors.get(httpKey);
        assertEquals("test", httpError.get("name"));
        assertEquals(1L, httpError.get("occurences"));
        assertEquals("http", httpError.get("method"));
        assertEquals("Test Error", httpError.get("error"));

        String udpKey = Utils.md5("udp" + "test" + "Unknown Error");
        Map<String, Object> udpError = errors.get(udpKey);
        assertEquals("test", udpError.get("name"));
        assertEquals(1L, udpError.get("occurences"));
        assertEquals("udp", udpError.get("method"));
        assertEquals("Unknown Error", udpError.get("error"));
    }
}
