package com.github.myzhan.locust4j.stats;

import java.util.Map;

import com.github.myzhan.locust4j.message.LongIntMap;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author myzhan
 * @date 2018/12/05
 */
public class TestStatsEntry {

    @Test
    public void TestRoundedResponseTime() {
        StatsEntry entry = new StatsEntry("http");
        entry.reset();
        entry.logResponseTime(99);
        entry.logResponseTime(147);
        entry.logResponseTime(3432);
        entry.logResponseTime(58760);

        LongIntMap responseTimes = entry.getResponseTimes();
        Assert.assertEquals(1, responseTimes.get(Long.valueOf(99)).intValue());
        Assert.assertEquals(1, responseTimes.get(Long.valueOf(150)).intValue());
        Assert.assertEquals(1, responseTimes.get(Long.valueOf(3400)).intValue());
        Assert.assertEquals(1, responseTimes.get(Long.valueOf(59000)).intValue());
    }

    @Test
    public void TestGetStrippedReport() {
        StatsEntry entry = new StatsEntry("http", "success");
        entry.reset();

        entry.log(1, 10);
        entry.log(2, 20);
        entry.logError("400 ERROR");

        Map<String, Object> serialized = entry.getStrippedReport();

        Assert.assertTrue(serialized.containsKey("name"));
        Assert.assertTrue(serialized.containsKey("method"));
        Assert.assertTrue(serialized.containsKey("last_request_timestamp"));
        Assert.assertTrue(serialized.containsKey("start_time"));
        Assert.assertTrue(serialized.containsKey("num_requests"));
        Assert.assertTrue(serialized.containsKey("num_failures"));
        Assert.assertTrue(serialized.containsKey("total_response_time"));
        Assert.assertTrue(serialized.containsKey("max_response_time"));
        Assert.assertTrue(serialized.containsKey("min_response_time"));
        Assert.assertTrue(serialized.containsKey("total_content_length"));
        Assert.assertTrue(serialized.containsKey("response_times"));
        Assert.assertTrue(serialized.containsKey("num_reqs_per_sec"));

        // getStrippedReport() will call reset()
        Assert.assertEquals(0, entry.getNumRequests());
        Assert.assertEquals(0, entry.getNumFailures());

    }
}
