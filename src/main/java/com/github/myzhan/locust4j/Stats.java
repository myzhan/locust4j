package com.github.myzhan.locust4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Stats implements Runnable {

    private Map<String, StatsEntry> entries;
    private Map<String, StatsError> errors;
    private StatsEntry total;
    private long startTime;

    private Object lock = new Object();

    private Stats() {
        this.entries = new HashMap<String, StatsEntry>(8);
        this.errors = new HashMap<String, StatsError>(8);
        this.total = new StatsEntry("Total");
        this.total.reset();

        Locust.getInstance().submitToCoreThreadPool(new StatsTimer(this));
        Locust.getInstance().submitToCoreThreadPool((this));
    }

    protected static Stats getInstance() {
        return StatsInstanceHolder.INSTANCE;
    }

    protected void wakeMeUp() {
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    private void sleep() {
        synchronized (lock) {
            try {
                lock.wait();
            } catch (Exception ex) {
                Log.error(ex.getMessage());
            }
        }
    }

    @Override
    public void run() {

        String name = Thread.currentThread().getName();
        Thread.currentThread().setName(name + "stats");

        while (true) {

            boolean allEmpty = true;

            RequestSuccess successMessage = Queues.REPORT_SUCCESS_TO_STATS.poll();
            if (successMessage != null) {
                this.logRequest(successMessage.requestType, successMessage.name, successMessage.responseTime
                    , successMessage.contentLength);
                allEmpty = false;
            }

            RequestFailure failureMessage = Queues.REPORT_FAILURE_TO_STATS.poll();
            if (null != failureMessage) {
                this.logError(failureMessage.requestType, failureMessage.name, failureMessage.error);
                allEmpty = false;
            }

            Boolean needToClearStats = Queues.CLEAR_STATS.poll();
            if (null != needToClearStats && needToClearStats) {
                this.clearAll();
                allEmpty = false;
            }

            Boolean timeToReport = Queues.TIME_TO_REPORT.poll();
            if (null != timeToReport) {
                Map data = this.collectReportData();
                Queues.REPORT_TO_RUNNER.add(data);
                allEmpty = false;
            }

            if (allEmpty) {
                // if all the queues are empty, stats will sleep to avoid endless loop.
                this.sleep();
            }
        }
    }

    private StatsEntry get(String name, String method) {
        StatsEntry entry = this.entries.get(name + method);
        if (null == entry) {
            entry = new StatsEntry(name, method);
            entry.reset();
            this.entries.put(name + method, entry);
        }
        return entry;
    }

    protected void logRequest(String method, String name, long responseTime, long contentLength) {
        this.total.log(responseTime, contentLength);
        this.get(name, method).log(responseTime, contentLength);
    }

    protected void logError(String method, String name, String error) {
        this.total.logError(error);
        this.get(name, method).logError(error);

        String key = method + name + error;
        StatsError entry = this.errors.get(key);
        if (null == entry) {
            entry = new StatsError(name, method, error);
            this.errors.put(key, entry);
        }
        entry.occured();
    }

    protected void clearAll() {
        this.total = new StatsEntry("Total");
        this.total.reset();
        this.entries = new HashMap<String, StatsEntry>(8);
        this.errors = new HashMap<String, StatsError>(8);
        this.startTime = Utils.currentTimeInSeconds();
    }

    protected List serializeStats() {
        List entries = new ArrayList(this.entries.size());
        for (Map.Entry<String, StatsEntry> item : this.entries.entrySet()) {
            StatsEntry entry = item.getValue();
            if (!(entry.numRequests == 0 && entry.numFailures == 0)) {
                entries.add(entry.getStrippedReport());
            }
        }
        return entries;
    }

    protected Map<String, Map<String, Object>> serializeErrors() {
        Map<String, Map<String, Object>> errors = new HashMap(8);
        for (Map.Entry<String, StatsError> item : this.errors.entrySet()) {
            String key = item.getKey();
            StatsError error = item.getValue();
            errors.put(key, error.toMap());
        }
        return errors;
    }

    protected Map<String, Object> collectReportData() {
        Map<String, Object> data = new HashMap<String, Object>(3);

        data.put("stats", this.serializeStats());
        data.put("stats_total", this.total.getStrippedReport());
        data.put("errors", this.serializeErrors());

        return data;
    }

    private static class StatsInstanceHolder {
        private static final Stats INSTANCE = new Stats();
    }

    private class StatsTimer implements Runnable {

        protected static final int SLAVE_REPORT_INTERVAL = 3000;
        protected Stats stats;

        protected StatsTimer(Stats stats) {
            this.stats = stats;
        }

        @Override
        public void run() {

            String name = Thread.currentThread().getName();
            Thread.currentThread().setName(name + "stats-timer");

            while (true) {
                try {
                    Thread.sleep(SLAVE_REPORT_INTERVAL);
                } catch (Exception ex) {
                    Log.error(ex);
                }
                Queues.TIME_TO_REPORT.offer(true);
                Stats.getInstance().wakeMeUp();
            }
        }
    }

}

class StatsEntry {

    protected String name = "";
    protected String method = "";
    protected long numRequests;
    protected long numFailures;
    protected long totalResponseTime;
    protected long minResponseTime;
    protected long maxResponseTime;
    protected LongIntMap numReqsPerSec;
    protected LongIntMap responseTimes;
    protected long totalContentLength;
    protected long startTime;
    protected long lastRequestTimestamp;

    protected StatsEntry(String name) {
        this.name = name;
    }

    protected StatsEntry(String name, String method) {
        this.name = name;
        this.method = method;
    }

    protected void reset() {
        this.startTime = Utils.currentTimeInSeconds();
        this.numRequests = 0;
        this.numFailures = 0;
        this.totalResponseTime = 0;
        this.responseTimes = new LongIntMap();
        this.minResponseTime = 0;
        this.maxResponseTime = 0;
        this.lastRequestTimestamp = Utils.currentTimeInSeconds();
        this.numReqsPerSec = new LongIntMap();
        this.totalContentLength = 0;
    }

    protected void log(long responseTime, long contentLength) {
        this.numRequests++;
        this.logTimeOfRequest();
        this.logResponseTime(responseTime);
        this.totalContentLength += contentLength;
    }

    protected void logTimeOfRequest() {
        long now = Utils.currentTimeInSeconds();
        this.numReqsPerSec.add(now);
        this.lastRequestTimestamp = now;
    }

    protected void logResponseTime(long responseTime) {
        this.totalResponseTime += responseTime;

        if (this.minResponseTime == 0) {
            this.minResponseTime = responseTime;
        }

        if (responseTime < this.minResponseTime) {
            this.minResponseTime = responseTime;
        }

        if (responseTime > this.maxResponseTime) {
            this.maxResponseTime = responseTime;
        }

        long roundedResponseTime = 0L;

        if (responseTime < 100) {
            roundedResponseTime = responseTime;
        } else if (responseTime < 1000) {
            roundedResponseTime = Utils.round(responseTime, -1);
        } else if (responseTime < 10000) {
            roundedResponseTime = Utils.round(responseTime, -2);
        } else {
            roundedResponseTime = Utils.round(responseTime, -3);
        }

        this.responseTimes.add(roundedResponseTime);
    }

    protected void logError(String error) {
        this.numFailures++;
    }

    protected Map<String, Object> serialize() {
        Map<String, Object> result = new HashMap<String, Object>(12);
        result.put("name", this.name);
        result.put("method", this.method);
        result.put("last_request_timestamp", this.lastRequestTimestamp);
        result.put("start_time", this.startTime);
        result.put("num_requests", this.numRequests);
        result.put("num_failures", this.numFailures);
        result.put("total_response_time", this.totalResponseTime);
        result.put("max_response_time", this.maxResponseTime);
        result.put("min_response_time", this.minResponseTime);
        result.put("total_content_length", this.totalContentLength);
        result.put("response_times", this.responseTimes);
        result.put("num_reqs_per_sec", this.numReqsPerSec);
        return result;
    }

    protected Map<String, Object> getStrippedReport() {
        Map<String, Object> report = this.serialize();
        this.reset();
        return report;
    }

}

class StatsError {

    protected String name;
    protected String method;
    protected String error;
    protected long occurences;

    protected StatsError(String name, String method, String error) {
        this.name = name;
        this.method = method;
        this.error = error;
    }

    protected void occured() {
        this.occurences++;
    }

    protected Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<String, Object>(4);
        m.put("name", this.name);
        m.put("method", this.method);
        m.put("error", this.error);
        m.put("occurences", this.occurences);
        return m;
    }

}

class RequestSuccess {

    protected String requestType;
    protected String name;
    protected long responseTime;
    protected long contentLength;

    protected RequestSuccess() {

    }
}

class RequestFailure {

    protected String requestType;
    protected String name;
    protected long responseTime;
    protected String error;

    protected RequestFailure() {

    }
}

class LongIntMap {

    protected Map<Long, Integer> longIntegerMap;

    LongIntMap() {
        longIntegerMap = new HashMap<Long, Integer>(16);
    }

    void add(Long k) {
        if (longIntegerMap.containsKey(k)) {
            longIntegerMap.put(k, longIntegerMap.get(k) + 1);
        } else {
            longIntegerMap.put(k, 1);
        }
    }

    @Override
    public String toString() {
        return this.longIntegerMap.toString();
    }
}