package com.github.myzhan.locust4j;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import com.github.myzhan.locust4j.ratelimit.AbstractRateLimiter;
import com.github.myzhan.locust4j.ratelimit.StableRateLimiter;
import com.github.myzhan.locust4j.rpc.Client;
import com.github.myzhan.locust4j.rpc.ZeromqClient;
import com.github.myzhan.locust4j.runtime.Runner;
import com.github.myzhan.locust4j.stats.RequestFailure;
import com.github.myzhan.locust4j.stats.RequestSuccess;
import com.github.myzhan.locust4j.stats.Stats;

/**
 * Locust class exposes all the APIs of locust4j.
 * Use Locust.getInstance() to get a Locust singleton.
 *
 * @author myzhan
 * @date 2018/12/05
 */
public class Locust {

    private String masterHost = "127.0.0.1";
    private int masterPort = 5557;
    private boolean started = false;
    private boolean verbose = false;
    private boolean rateLimitEnabled;
    private AbstractRateLimiter rateLimiter;
    private Runner runner;

    private Locust() {

    }

    /**
     * Get a locust singleton.
     *
     * @return
     */
    public static Locust getInstance() {
        return InstanceHolder.LOCUST;
    }

    /**
     * Set master host.
     *
     * @param masterHost
     */
    public void setMasterHost(String masterHost) {
        this.masterHost = masterHost;
    }

    /**
     * Set master port.
     *
     * @param masterPort
     */
    public void setMasterPort(int masterPort) {
        this.masterPort = masterPort;
    }

    /**
     * Limit max PRS that locust4j can generator.
     *
     * @param maxRPS
     */
    public void setMaxRPS(long maxRPS) {
        rateLimiter = new StableRateLimiter(maxRPS);
        this.setRateLimiter(rateLimiter);
    }

    /**
     * Set the rate limiter
     *
     * @param rateLimiter
     * @since 1.0.3
     */
    public void setRateLimiter(AbstractRateLimiter rateLimiter) {
        this.rateLimitEnabled = true;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Get the rate limiter
     *
     * @return rateLimiter
     * @since 1.0.3
     */
    public AbstractRateLimiter getRateLimiter() {
        return this.rateLimiter;
    }

    /**
     * Return rateLimitEnabled
     *
     * @return
     * @since 1.0.3
     */
    public boolean isRateLimitEnabled() {
        return this.rateLimitEnabled;
    }

    public void setVerbose(boolean v) {
        this.verbose = v;
    }

    public boolean isVerbose() {
        return this.verbose;
    }

    protected Runner getRunner() {
        return this.runner;
    }

    private List<AbstractTask> removeInvalidTasks(List<AbstractTask> tasks) {
        ListIterator<AbstractTask> iter = tasks.listIterator();
        while (iter.hasNext()) {
            if (iter.next().getWeight() < 0) {
                iter.remove();
            }
        }
        return tasks;
    }

    /**
     * Add tasks to Runner, connect to master and wait for messages of master.
     *
     * @param tasks
     */
    public void run(AbstractTask... tasks) {
        run(Arrays.asList(tasks));
    }

    /**
     * Add tasks to Runner, connect to master and wait for messages of master.
     *
     * @param tasks
     */
    public synchronized void run(List<AbstractTask> tasks) {

        if (this.started) {
            // Don't call Locust.run() multiply times.
            return;
        }

        tasks = removeInvalidTasks(tasks);

        if (null != this.rateLimiter) {
            this.rateLimiter.start();
        }

        Client client = new ZeromqClient(masterHost, masterPort);
        runner = new Runner();
        Stats.getInstance().start();
        runner.setStats(Stats.getInstance());
        runner.setRPCClient(client);
        runner.setTasks(tasks);
        runner.getReady();
        addShutdownHook();

        this.started = true;
    }

    /**
     * Run tasks without connecting to master.
     *
     * @param tasks
     */
    public void dryRun(AbstractTask... tasks) {
        dryRun(Arrays.asList(tasks));
    }

    /**
     * Run tasks without connecting to master.
     *
     * @param tasks
     */
    public void dryRun(List<AbstractTask> tasks) {
        Log.debug("Running tasks without connecting to master.");
        for (AbstractTask task : tasks) {
            Log.debug(String.format("Running task named %s", task.getName()));
            task.execute();
        }
    }

    /**
     * when JVM is shutting down, send a quit message to master, then master will remove this slave from its list.
     */
    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // tell master that I'm quitting
                Locust.getInstance().getRateLimiter().stop();
                Locust.getInstance().runner.quit();
            }
        });
    }

    /**
     * Add a successful record, locust4j will collect it, calculate things like RPS, and report to master.
     *
     * @param requestType
     * @param name
     * @param responseTime
     * @param contentLength
     */
    public void recordSuccess(String requestType, String name, long responseTime, long contentLength) {
        RequestSuccess success = new RequestSuccess();
        success.setRequestType(requestType);
        success.setName(name);
        success.setResponseTime(responseTime);
        success.setContentLength(contentLength);
        Stats.getInstance().getReportSuccessQueue().offer(success);
        Stats.getInstance().wakeMeUp();
    }

    /**
     * Add a failed record, locust4j will collect it, and report to master.
     *
     * @param requestType
     * @param name
     * @param responseTime
     * @param error
     */
    public void recordFailure(String requestType, String name, long responseTime, String error) {
        RequestFailure failure = new RequestFailure();
        failure.setRequestType(requestType);
        failure.setName(name);
        failure.setResponseTime(responseTime);
        failure.setError(error);
        Stats.getInstance().getReportFailureQueue().offer(failure);
        Stats.getInstance().wakeMeUp();
    }

    private static class InstanceHolder {
        private static final Locust LOCUST = new Locust();
    }
}
