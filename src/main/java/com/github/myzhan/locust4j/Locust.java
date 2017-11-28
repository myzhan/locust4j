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

public class Locust {

    private final Object taskSyncLock = new Object();
    private String masterHost = "127.0.0.1";
    private int masterPort = 5557;
    private Client client;
    private boolean started = false;
    private AtomicInteger threadNumber = new AtomicInteger();
    private ExecutorService coreThreadPool;
    private long maxRPS;
    private AtomicLong maxRPSThreshold = new AtomicLong();
    private boolean maxRPSEnabled;

    private Locust() {
        this.coreThreadPool = new ThreadPoolExecutor(7, Integer.MAX_VALUE ,0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName(String.format("locust4j-core#%d#", threadNumber.getAndIncrement()));
                return thread;
            }
        });
    }

    public static Locust getInstance() {
        return InstanceHolder.LOCUST;
    }

    public void setMasterHost(String masterHost) {
        this.masterHost = masterHost;
    }

    public void setMasterPort(int masterPort) {
        this.masterPort = masterPort;
    }

    public long getMaxRPS() {
        return this.maxRPS;
    }

    public void setMaxRPS(long maxRPS) {
        this.maxRPS = maxRPS;
        this.maxRPSEnabled = true;
    }

    protected void submitToCoreThreadPool(Runnable r) {
        this.coreThreadPool.submit(r);
    }

    public boolean isMaxRPSEnabled() {
        return this.maxRPSEnabled;
    }

    protected Object getTaskSyncLock() {
        return this.taskSyncLock;
    }

    protected AtomicLong getMaxRPSThreshold() {
        return this.maxRPSThreshold;
    }

    public void run(AbstractTask... tasks) {
        List<AbstractTask> taskList = new ArrayList<AbstractTask>();
        for (AbstractTask task : tasks) {
            taskList.add(task);
        }
        run(taskList);
    }

    public synchronized void run(List<AbstractTask> tasks) {

        if (this.started) {
            // Don't call Locust.run() multiply times.
            return;
        }

        if (this.maxRPSEnabled) {
            Locust.getInstance().submitToCoreThreadPool(new TokenUpdater());
            Log.debug(String.format("Max RPS is limited to %d", this.maxRPS));
        }

        this.client = new ZeromqClient(masterHost, masterPort);
        Runner runner = Runner.getInstance();
        runner.setTasks(tasks);
        runner.getReady();
        addShutdownHook();

        this.started = true;
    }

    public void dryRun(AbstractTask... tasks) {
        List<AbstractTask> taskList = new ArrayList<AbstractTask>();
        for (AbstractTask task : tasks) {
            taskList.add(task);
        }
        dryRun(taskList);
    }

    public void dryRun(List<AbstractTask> tasks) {
        Log.debug("Running tasks without connecting to master.");
        for (AbstractTask task : tasks) {
            Log.debug(String.format("Running task named %s", task.getName()));
            task.execute();
        }
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // tell master that I'm quitting
                Runner.getInstance().quit();
                try {
                    Queues.DISCONNECTED_FROM_MASTER.take();
                } catch (Exception ex) {
                    Log.error(ex);
                }
            }
        });
    }

    public void recordSuccess(String requestType, String name, long responseTime, long contentLength) {
        RequestSuccess success = new RequestSuccess();
        success.requestType = requestType;
        success.name = name;
        success.responseTime = responseTime;
        success.contentLength = contentLength;
        Queues.REPORT_SUCCESS_TO_STATS.offer(success);
        Stats.getInstance().wakeMeUp();
    }

    public void recordFailure(String requestType, String name, long responseTime, String error) {
        RequestFailure failure = new RequestFailure();
        failure.requestType = requestType;
        failure.name = name;
        failure.responseTime = responseTime;
        failure.error = error;
        Queues.REPORT_FAILURE_TO_STATS.offer(failure);
        Stats.getInstance().wakeMeUp();
    }

    private static class InstanceHolder {
        private static final Locust LOCUST = new Locust();
    }

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
