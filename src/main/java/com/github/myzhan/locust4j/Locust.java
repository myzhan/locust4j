package com.github.myzhan.locust4j;

import com.github.myzhan.locust4j.ratelimit.AbstractRateLimiter;
import com.github.myzhan.locust4j.ratelimit.StableRateLimiter;
import com.github.myzhan.locust4j.rpc.Client;
import com.github.myzhan.locust4j.rpc.ZeromqClient;
import com.github.myzhan.locust4j.runtime.Runner;
import com.github.myzhan.locust4j.stats.RequestFailure;
import com.github.myzhan.locust4j.stats.RequestSuccess;
import com.github.myzhan.locust4j.stats.Stats;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Locust class exposes all the APIs of locust4j.
 * Use Locust.getInstance() to get a Locust singleton.
 *
 * @author myzhan
 */
public class Locust {

    private static final Logger logger = LoggerFactory.getLogger(Locust.class);

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
     * Get the locust singleton.
     *
     * @return a Locust singleton
     * @since 1.0.0
     */
    public static Locust getInstance() {
        return InstanceHolder.LOCUST;
    }

    /**
     * Set the master host.
     *
     * @param masterHost the master host
     * @since 1.0.0
     */
    public void setMasterHost(String masterHost) {
        this.masterHost = masterHost;
    }

    /**
     * Set the master port.
     *
     * @param masterPort the master port
     * @since 1.0.0
     */
    public void setMasterPort(int masterPort) {
        this.masterPort = masterPort;
    }

    /**
     * Limit max PRS that locust4j can generator.
     *
     * @param maxRPS max rps
     * @since 1.0.0
     */
    public void setMaxRPS(long maxRPS) {
        rateLimiter = new StableRateLimiter(maxRPS);
        this.setRateLimiter(rateLimiter);
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
     * Set the rate limiter
     *
     * @param rateLimiter builtin or custom rate limiter
     * @since 1.0.3
     */
    public void setRateLimiter(AbstractRateLimiter rateLimiter) {
        this.rateLimitEnabled = true;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Return rateLimitEnabled
     *
     * @return is rate limiter enabled?
     * @since 1.0.3
     */
    public boolean isRateLimitEnabled() {
        return this.rateLimitEnabled;
    }

    /**
     * @return is it verbose?
     * @since 1.0.2
     * @deprecated Since 1.0.8, we use slf4j as a logging facade without a particular logging framework.
     */
    public boolean isVerbose() {
        return this.verbose;
    }

    /**
     * Print out the internal log of locust4j, or not.
     *
     * @param v set true to print out
     * @since 1.0.2
     * @deprecated Since 1.0.8, we use slf4j as a logging facade without a particular logging framework.
     */
    public void setVerbose(boolean v) {
        this.verbose = v;
    }

    protected Runner getRunner() {
        return this.runner;
    }

    protected void setRunner(Runner runner) {
        this.runner = runner;
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
     * @param tasks test tasks
     * @since 1.0.0
     */
    public void run(AbstractTask... tasks) {
        run(Arrays.asList(tasks));
    }

    /**
     * Add tasks to Runner, connect to master and wait for messages of master.
     *
     * @param tasks test tasks
     * @since 1.0.0
     */
    public synchronized void run(List<AbstractTask> tasks) {
        if (this.started) {
            // Don't call Locust.run() multiply times.
            return;
        }

        removeInvalidTasks(tasks);

        Stats.getInstance().start();

        runner = new Runner();
        runner.setStats(Stats.getInstance());

        Client client = new ZeromqClient(masterHost, masterPort, runner.getNodeID());
        runner.setRPCClient(client);
        runner.setTasks(tasks);
        runner.getReady();
        addShutdownHook();

        this.started = true;
    }

    /**
     * Run tasks without connecting to master.
     *
     * @param tasks test tasks
     * @since 1.0.0
     */
    public void dryRun(AbstractTask... tasks) {
        dryRun(Arrays.asList(tasks));
    }

    /**
     * Run tasks without connecting to master.
     *
     * @param tasks test tasks
     * @since 1.0.0
     */
    public void dryRun(List<AbstractTask> tasks) {
        logger.debug("Running tasks without connecting to master.");
        for (AbstractTask task : tasks) {
            logger.debug("Running task named {} onStart");
            try{
                task.onStart();
            } catch (Exception ex) {
                logger.error("Unknown exception when running onStart");
                continue;
            }

            try {
                logger.debug("Running task named {}", task.getName());
                try {
                    task.execute();
                } catch (Exception ex) {
                    logger.error("Unknown exception when executing the task", ex);
                }
            
            } finally {
                task.onStop();
            }
        }
    }

    /**
     * Stop locust
     *
     * @since 1.0.7
     */
    public synchronized void stop() {
        if (this.started) {
            AbstractRateLimiter rateLimiter = this.getRateLimiter();
            if (rateLimiter != null && !rateLimiter.isStopped()) {
                rateLimiter.stop();
            }
            // tell master that I'm quitting
            if (this.runner != null) {
                this.runner.quit();
            }
            this.started = false;
        }
    }

    /**
     * when JVM is shutting down, send a quit message to master, then master will remove this slave from its list.
     */
    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                Locust.getInstance().stop();
            }
        });
    }

    /**
     * Add a successful record, locust4j will collect it, calculate things like RPS, and report to master.
     *
     * @param requestType   locust use request type to classify test results
     * @param name          like request type, used by locust to classify test results
     * @param responseTime  how long does it take for a single test scenario, in millis
     * @param contentLength content length in bytes
     * @since 1.0.0
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
     * @param requestType  locust use request type to classify test results
     * @param name         like request type, used by locust to classify test results
     * @param responseTime how long does it take for a single test scenario, in millis
     * @param error        error message
     * @since 1.0.0
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

    /**
     * Get remote params sent by the master, which will be set before spawning begins.
     * But Locust has not documentations about the data protocol. It may change and this method will return null with
     * the same key.
     * @param key remote param key
     * @return remote param value
     * @since 1.0.11
     */
    public String getRemoteParam(String key) {
        if (this.runner == null) {
            return null;
        }
        return this.runner.getRemoteParams().get(key);
    }

    private static class InstanceHolder {
        private static final Locust LOCUST = new Locust();
    }
}
