package com.github.myzhan.locust4j;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author myzhan
 * @date 2017/12/06
 */
public class TestStats {

    @Test
    public void logRequest() {

        Stats stats = Stats.getInstance();

        stats.logRequest("http", "test", 1000l, 2000l);
        stats.logRequest("http", "test", 2000l, 4000l);
        stats.logRequest("udp", "test", 300l, 300l);

        StatsEntry entry = stats.get("test", "http");

        Assert.assertEquals("test", entry.name);
        Assert.assertEquals("http", entry.method);
        Assert.assertEquals(2, entry.numRequests);
        Assert.assertEquals(0, entry.numFailures);
        Assert.assertEquals(1000l, entry.minResponseTime);
        Assert.assertEquals(2000l, entry.maxResponseTime);
        Assert.assertEquals(3000l, entry.totalResponseTime);
        Assert.assertEquals(6000l, entry.totalContentLength);
        Assert.assertEquals(1, (long)entry.responseTimes.get(1000l));
        Assert.assertEquals(1, (long)entry.responseTimes.get(2000l));

        StatsEntry total = stats.getTotal();

        Assert.assertEquals("Total", total.name);
        Assert.assertEquals("", total.method);
        Assert.assertEquals(3, total.numRequests);
        Assert.assertEquals(0, total.numFailures);
        Assert.assertEquals(300l, total.minResponseTime);
        Assert.assertEquals(2000l, total.maxResponseTime);
        Assert.assertEquals(3300l, total.totalResponseTime);
        Assert.assertEquals(6300l, total.totalContentLength);
        Assert.assertEquals(1, (long)total.responseTimes.get(300l));
        Assert.assertEquals(1, (long)total.responseTimes.get(1000l));
        Assert.assertEquals(1, (long)total.responseTimes.get(2000l));

    }

    @Test
    public void logError() {

        Stats stats = Stats.getInstance();

        stats.logError("http", "test", "Test Error");
        stats.logError("udp", "test", "Unknown Error");

        StatsEntry entry = stats.get("test", "http");
        Assert.assertEquals(1, entry.numFailures);

        StatsEntry total = stats.getTotal();
        Assert.assertEquals(2, total.numFailures);

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
