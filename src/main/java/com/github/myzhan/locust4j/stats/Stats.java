package com.github.myzhan.locust4j.stats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.myzhan.locust4j.Log;
import com.github.myzhan.locust4j.utils.Utils;

/**
 * Stats collects test results from Queues.REPORT_SUCCESS_TO_STATS and Queues.REPORT_FAILURE_TO_STATS and reports to
 * Runner with an interval of 3 seconds.
 *
 * To keep simplicity, it's designed to be single-threaded, but it won't be a bottleneck.
 *
 * @author myzhan
 * @date 2018/12/05
 */
public class Stats implements Runnable {

    private Map<String, StatsEntry> entries;
    private Map<String, StatsError> errors;
    private StatsEntry total;
    private long startTime;

    private ConcurrentLinkedQueue<RequestSuccess> reportSuccessQueue;
    private ConcurrentLinkedQueue<RequestFailure> reportFailureQueue;
    private ConcurrentLinkedQueue<Boolean> clearStatsQueue;
    private ConcurrentLinkedQueue<Boolean> timeToReportQueue;
    private BlockingQueue<Map> messageToRunnerQueue;

    private ExecutorService threadPool;
    private AtomicInteger threadNumber;
    private Object lock = new Object();

    public Stats() {
        reportSuccessQueue = new ConcurrentLinkedQueue<RequestSuccess>();
        reportFailureQueue = new ConcurrentLinkedQueue<RequestFailure>();
        clearStatsQueue = new ConcurrentLinkedQueue<Boolean>();
        timeToReportQueue = new ConcurrentLinkedQueue<Boolean>();
        messageToRunnerQueue = new LinkedBlockingDeque<Map>();
        threadNumber = new AtomicInteger();

        this.entries = new HashMap<String, StatsEntry>(8);
        this.errors = new HashMap<String, StatsError>(8);
        this.total = new StatsEntry("Total");
        this.total.reset();
    }

    public static Stats getInstance() {
        return StatsInstanceHolder.INSTANCE;
    }

    public void start() {
        threadPool = new ThreadPoolExecutor(2, Integer.MAX_VALUE, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName(String.format("locust4j-stats#%d#", threadNumber.getAndIncrement()));
                return thread;
            }
        });

        threadPool.submit(new StatsTimer(this));
        threadPool.submit(this);
    }

    public void stop() {
        threadPool.shutdownNow();
    }

    public Queue<RequestSuccess> getReportSuccessQueue() {
        return this.reportSuccessQueue;
    }

    public Queue<RequestFailure> getReportFailureQueue() {
        return this.reportFailureQueue;
    }

    public Queue<Boolean> getClearStatsQueue() {
        return this.clearStatsQueue;
    }

    public BlockingQueue<Map> getMessageToRunnerQueue() {
        return this.messageToRunnerQueue;
    }

    public void wakeMeUp() {
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

    /**
     * User code reports successful and failed records to Stats.
     * If the sending speed is too fast, single-threaded stats may be a bottleneck and reports wrong RPS.
     */
    @Override
    public void run() {

        String name = Thread.currentThread().getName();
        Thread.currentThread().setName(name + "stats");

        while (true) {

            boolean allEmpty = true;

            RequestSuccess successMessage = reportSuccessQueue.poll();
            if (successMessage != null) {
                this.logRequest(successMessage.getRequestType(), successMessage.getName(),
                    successMessage.getResponseTime()
                    , successMessage.getContentLength());
                allEmpty = false;
            }

            RequestFailure failureMessage = reportFailureQueue.poll();
            if (null != failureMessage) {
                this.logError(failureMessage.getRequestType(), failureMessage.getName(), failureMessage.getError());
                allEmpty = false;
            }

            Boolean needToClearStats = clearStatsQueue.poll();
            if (null != needToClearStats && needToClearStats) {
                this.clearAll();
                allEmpty = false;
            }

            Boolean timeToReport = timeToReportQueue.poll();
            if (null != timeToReport) {
                Map data = this.collectReportData();
                messageToRunnerQueue.add(data);
                allEmpty = false;
            }

            if (allEmpty) {
                // if all the queues are empty, stats will sleep to avoid endless loop.
                this.sleep();
            }
        }
    }

    protected StatsEntry getTotal() {
        return this.total;
    }

    protected StatsEntry get(String name, String method) {
        StatsEntry entry = this.entries.get(name + method);
        if (null == entry) {
            entry = new StatsEntry(name, method);
            entry.reset();
            this.entries.put(name + method, entry);
        }
        return entry;
    }

    public void logRequest(String method, String name, long responseTime, long contentLength) {
        this.total.log(responseTime, contentLength);
        this.get(name, method).log(responseTime, contentLength);
    }

    public void logError(String method, String name, String error) {
        this.total.logError(error);
        this.get(name, method).logError(error);

        String key = Utils.md5(method + name + error);
        if (null == key) {
            key = method + name + error;
        }
        StatsError entry = this.errors.get(key);
        if (null == entry) {
            entry = new StatsError(name, method, error);
            this.errors.put(key, entry);
        }
        entry.occured();
    }

    public void clearAll() {
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
            if (!(entry.getNumRequests() == 0 && entry.getNumFailures() == 0)) {
                entries.add(entry.getStrippedReport());
            }
        }
        return entries;
    }

    public Map<String, Map<String, Object>> serializeErrors() {
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
                stats.timeToReportQueue.offer(true);
                stats.wakeMeUp();
            }
        }
    }

}