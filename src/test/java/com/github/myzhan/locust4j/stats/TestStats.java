package com.github.myzhan.locust4j.stats;

import java.util.List;
import java.util.Map;

import com.github.myzhan.locust4j.stats.RequestFailure;
import com.github.myzhan.locust4j.stats.RequestSuccess;
import com.github.myzhan.locust4j.stats.Stats;
import com.github.myzhan.locust4j.stats.StatsEntry;
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

        Map<String, Object> dataToRunner = stats.getMessageToRunnerQueue().take();

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
        stats.logRequest("http", "test", 1000L, 0);
        stats.logError("http", "test", "Test Error");
        stats.logError("udp", "test", "Unknown Error");

        StatsEntry entry = stats.get("test", "http");
        assertEquals(1, entry.getNumFailures());

        StatsEntry total = stats.getTotal();
        assertEquals(2, total.getNumFailures());
        assertEquals(1, total.getNumRequests());

        Map<String, Map<String, Object>> errors = stats.serializeErrors();

        String httpKey = Utils.md5("http" + "test" + "Test Error");
        Map<String, Object> httpError = errors.get(httpKey);
        assertEquals("test", httpError.get("name"));
        assertEquals(1L, httpError.get("occurrences"));
        assertEquals("http", httpError.get("method"));
        assertEquals("Test Error", httpError.get("error"));

        String udpKey = Utils.md5("udp" + "test" + "Unknown Error");
        Map<String, Object> udpError = errors.get(udpKey);
        assertEquals("test", udpError.get("name"));
        assertEquals(1L, udpError.get("occurrences"));
        assertEquals("udp", udpError.get("method"));
        assertEquals("Unknown Error", udpError.get("error"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void TestCollectReportDataResetsStats() {
        stats.logRequest("http", "test", 1000L, 2000);
        stats.logError("http", "test", "Test Error");
        stats.logError("udp", "test", "Unknown Error");

        // First Pass - Check Correctness
        Map<String, Object> report = stats.collectReportData();

        assertTrue(report.containsKey("stats"));
        assertTrue(report.containsKey("stats_total"));
        assertTrue(report.containsKey("errors"));

        List<Map<String, Object>> statsReport = (List<Map<String, Object>>) report.get("stats");
        Map<String, Object> statsTotalReport = (Map<String, Object>) report.get("stats_total");
        Map<String, Object> errorReport = (Map<String, Object>) report.get("errors");

        assertEquals(2, statsReport.size()); // "http" and "udp"
        assertEquals("Total", statsTotalReport.get("name"));
        assertEquals(1L, statsTotalReport.get("num_requests"));
        assertEquals(2L, statsTotalReport.get("num_failures"));
        assertEquals(2, errorReport.size());

        String httpErrorKey = Utils.md5("http" + "test" + "Test Error");
        String udpErrorKey = Utils.md5("udp" + "test" + "Unknown Error");

        assertTrue(errorReport.containsKey(httpErrorKey));
        assertTrue(errorReport.containsKey(udpErrorKey));

        Map<String, Object> httpError = (Map<String, Object>) errorReport.get(httpErrorKey);

        assertEquals("http", httpError.get("method"));
        assertEquals("test", httpError.get("name"));
        assertEquals(1L, httpError.get("occurrences"));
        assertEquals("Test Error", httpError.get("error"));

        Map<String, Object> udpError = (Map<String, Object>) errorReport.get(udpErrorKey);

        assertEquals("udp", udpError.get("method"));
        assertEquals("test", udpError.get("name"));
        assertEquals(1L, udpError.get("occurrences"));
        assertEquals("Unknown Error", udpError.get("error"));

        // Second Pass - Check that Stats Reset
        report = stats.collectReportData();

        assertTrue(report.containsKey("stats"));
        assertTrue(report.containsKey("stats_total"));
        assertTrue(report.containsKey("errors"));

        statsReport = (List<Map<String, Object>>) report.get("stats");
        statsTotalReport = (Map<String, Object>) report.get("stats_total");
        errorReport = (Map<String, Object>) report.get("errors");

        assertEquals(0, statsReport.size());
        assertEquals("Total", statsTotalReport.get("name"));
        assertEquals(0L, statsTotalReport.get("num_requests"));
        assertEquals(0L, statsTotalReport.get("num_failures"));
        assertEquals(0, errorReport.size());
    }
}
