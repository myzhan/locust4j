package com.github.myzhan.locust4j.stats;

import java.util.Map;

import com.github.myzhan.locust4j.message.LongIntMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author myzhan
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
        assertEquals(1, responseTimes.get(99L).intValue());
        assertEquals(1, responseTimes.get(150L).intValue());
        assertEquals(1, responseTimes.get(3400L).intValue());
        assertEquals(1, responseTimes.get(59000L).intValue());
    }

    @Test
    public void TestGetStrippedReport() {
        StatsEntry entry = new StatsEntry("http", "success");
        entry.reset();

        entry.log(1, 10);
        entry.log(2, 20);
        entry.logError("400 ERROR");

        Map<String, Object> serialized = entry.getStrippedReport();

        assertTrue(serialized.containsKey("name"));
        assertTrue(serialized.containsKey("method"));
        assertTrue(serialized.containsKey("last_request_timestamp"));
        assertTrue(serialized.containsKey("start_time"));
        assertTrue(serialized.containsKey("num_requests"));
        assertTrue(serialized.containsKey("num_failures"));
        assertTrue(serialized.containsKey("total_response_time"));
        assertTrue(serialized.containsKey("max_response_time"));
        assertTrue(serialized.containsKey("min_response_time"));
        assertTrue(serialized.containsKey("total_content_length"));
        assertTrue(serialized.containsKey("response_times"));
        assertTrue(serialized.containsKey("num_reqs_per_sec"));

        // getStrippedReport() will call reset()
        assertEquals(0, entry.getNumRequests());
        assertEquals(0, entry.getNumFailures());

    }
}
