package com.github.myzhan.locust4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.github.myzhan.locust4j.rpc.Client;
import com.github.myzhan.locust4j.rpc.ZeromqClient;

/**
 * Locust class exposes all the APIs of locust4j.
 * Use Locust.getInstance() to get a Locust singleton.
 */
public class Locust {

    private final Object taskSyncLock = new Object();
    private String masterHost = "127.0.0.1";
    private int masterPort = 5557;
    private boolean started = false;
    private boolean verbose = false;
    private AtomicInteger threadNumber = new AtomicInteger();
    private ExecutorService coreThreadPool;
    private long maxRPS;
    private AtomicLong maxRPSThreshold = new AtomicLong();
    private boolean maxRPSEnabled;

    private Locust() {
        this.coreThreadPool = new ThreadPoolExecutor(7, Integer.MAX_VALUE, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName(String.format("locust4j-core#%d#", threadNumber.getAndIncrement()));
                return thread;
            }
        });
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

    public long getMaxRPS() {
        return this.maxRPS;
    }

    /**
     * Limit max PRS that locust4j can generator.
     *
     * @param maxRPS
     */
    public void setMaxRPS(long maxRPS) {
        this.maxRPS = maxRPS;
        this.maxRPSEnabled = true;
    }

    /**
     * Submit runnable to core threadpool of locust4j.
     *
     * @param r
     */
    protected void submitToCoreThreadPool(Runnable r) {
        this.coreThreadPool.submit(r);
    }

    public boolean isMaxRPSEnabled() {
        return this.maxRPSEnabled;
    }

    public void setVerbose(boolean v) {
        this.verbose = v;
    }

    public boolean isVerbose() {
        return this.verbose;
    }

    protected Object getTaskSyncLock() {
        return this.taskSyncLock;
    }

    protected AtomicLong getMaxRPSThreshold() {
        return this.maxRPSThreshold;
    }

    /**
     * Add tasks to Runner, connect to master and wait for messages of master.
     *
     * @param tasks
     */
    public void run(AbstractTask... tasks) {
        List<AbstractTask> taskList = new ArrayList<AbstractTask>();
        for (AbstractTask task : tasks) {
            taskList.add(task);
        }
        run(taskList);
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

        if (this.maxRPSEnabled) {
            Locust.getInstance().submitToCoreThreadPool(new TokenUpdater());
            Log.debug(String.format("Max RPS is limited to %d", this.maxRPS));
        }

        Client client = new ZeromqClient(masterHost, masterPort);
        Runner runner = Runner.getInstance();
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
        List<AbstractTask> taskList = new ArrayList<AbstractTask>();
        for (AbstractTask task : tasks) {
            taskList.add(task);
        }
        dryRun(taskList);
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
                Runner.getInstance().quit();
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
        success.requestType = requestType;
        success.name = name;
        success.responseTime = responseTime;
        success.contentLength = contentLength;
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
        failure.requestType = requestType;
        failure.name = name;
        failure.responseTime = responseTime;
        failure.error = error;
        Stats.getInstance().getReportFailureQueue().offer(failure);
        Stats.getInstance().wakeMeUp();
    }

    private static class InstanceHolder {
        private static final Locust LOCUST = new Locust();
    }

    /**
     * If maxPRS is enabled, TokenUpdater will update maxRPSThreshold every seconds.
     */
    private class TokenUpdater implements Runnable {

        @Override
        public void run() {
            String name = Thread.currentThread().getName();
            Thread.currentThread().setName(name + "token-updater");
            long maxRPS = Locust.getInstance().getMaxRPS();
            AtomicLong maxRPSThreshold = Locust.getInstance().getMaxRPSThreshold();
            while (true) {
                try {
                    synchronized (Locust.getInstance().taskSyncLock) {
                        maxRPSThreshold.set(maxRPS);
                        Locust.getInstance().taskSyncLock.notifyAll();
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Log.error(ex);
                }
            }
        }
    }

}
