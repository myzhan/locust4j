package com.github.myzhan.locust4j.runtime;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.myzhan.locust4j.AbstractTask;
import com.github.myzhan.locust4j.Log;
import com.github.myzhan.locust4j.message.Message;
import com.github.myzhan.locust4j.rpc.Client;
import com.github.myzhan.locust4j.stats.Stats;
import com.github.myzhan.locust4j.utils.Utils;

/**
 * Runner is the core role that runs all tasks, collects test results and reports to the master.
 *
 * @author myzhan
 * @date 2018/12/05
 */
public class Runner {

    /**
     * Every locust4j instance registers a unique nodeID to the master when it makes a connection.
     * NodeID is kept by Runner.
     */
    protected String nodeID;

    /**
     * Number of clients required by the master, locust4j use threads to simulate clients.
     */
    protected int numClients = 0;

    /**
     * Current state of runner.
     */
    private RunnerState state;

    /**
     * Task instances submitted by user.
     */
    private List<AbstractTask> tasks;

    /**
     * RPC Client.
     */
    private Client rpcClient;

    /**
     * Hatch rate required by the master.
     * Hatch rate means clients/s.
     */
    private int hatchRate = 0;

    /**
     * Thread pool used by runner, it will be re-created when runner starts hatching.
     */
    private ExecutorService taskExecutor;

    /**
     * Thread pool used by runner to receive and send message
     */
    private ExecutorService executor;

    /**
     * Stats collect successes and failures.
     */
    private Stats stats;

    /**
     * Use this for naming threads in the thread pool.
     */
    private AtomicInteger threadNumber = new AtomicInteger();

    private Runner() {
        this.nodeID = Utils.getNodeID();
    }

    public static Runner getInstance() {
        return RunnerInstanceHolder.RUNNER;
    }

    public RunnerState getState() {
        return this.state;
    }

    public void setRPCClient(Client client) {
        this.rpcClient = client;
    }

    public void setStats(Stats stats) {
        this.stats = stats;
    }

    public void setTasks(List<AbstractTask> tasks) {
        this.tasks = tasks;
    }

    private void spawnWorkers(int spawnCount) {

        Log.debug(
            String.format("Hatching and swarming %d clients at the rate %d clients/s...", spawnCount, this.hatchRate));

        float weightSum = 0;
        for (AbstractTask task : this.tasks) {
            weightSum += task.getWeight();
        }

        for (AbstractTask task : this.tasks) {

            float percent;
            if (0 == weightSum) {
                percent = 1 / (float)this.tasks.size();
            } else {
                percent = task.getWeight() / weightSum;
            }

            int amount = Math.round(spawnCount * percent);
            if (weightSum == 0) {
                amount = spawnCount / this.tasks.size();
            }

            Log.debug(String.format("Allocating %d threads to task, which name is %s", amount, task.getName()));

            for (int i = 1; i <= amount; i++) {
                if (i % this.hatchRate == 0) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception ex) {
                        Log.error(ex.getMessage());
                    }
                }
                this.numClients++;
                this.taskExecutor.submit(task);
            }
        }

        this.hatchComplete();
    }

    protected void startHatching(int spawnCount, int hatchRate) {
        stats.getClearStatsQueue().offer(true);
        Stats.getInstance().wakeMeUp();

        this.hatchRate = hatchRate;
        this.numClients = 0;
        this.threadNumber.set(0);
        this.taskExecutor = new ThreadPoolExecutor(this.numClients, spawnCount, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(),
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("locust4j-worker#" + threadNumber.getAndIncrement());
                    return thread;
                }
            });
        this.spawnWorkers(spawnCount);
    }

    protected void hatchComplete() {
        Map data = new HashMap(1);
        data.put("count", this.numClients);
        try {
            this.rpcClient.send((new Message("hatch_complete", data, this.nodeID)));
        } catch (IOException ex) {
            Log.error(ex);
        }
    }

    public void quit() {
        try {
            this.rpcClient.send(new Message("quit", null, this.nodeID));
        } catch (IOException ex) {
            Log.error(ex);
        }
    }

    private void shutdownThreadPool() {
        this.taskExecutor.shutdownNow();
        try {
            this.taskExecutor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Log.error(ex.getMessage());
        }
        this.taskExecutor = null;
    }

    protected void stop() {
        this.shutdownThreadPool();
    }

    private void onHatchMessage(Message message) {
        try {
            this.rpcClient.send(new Message("hatching", null, this.nodeID));
        } catch (IOException ex) {
            Log.error(ex);
        }
        Float hatchRate = Float.valueOf(message.getData().get("hatch_rate").toString());
        int numClients = Integer.valueOf(message.getData().get("num_clients").toString());
        if (hatchRate.intValue() == 0 || numClients == 0) {
            Log.debug(String
                .format("Invalid message (hatch_rate: %d, num_clients: %d) from master, ignored.",
                    hatchRate.intValue(), numClients));
            return;
        }
        this.startHatching(numClients, hatchRate.intValue());
    }

    private void onMessage(Message message) {
        String type = message.getType();

        if ("quit".equals(type)) {
            Log.debug("Got quit message from master, shutting down...");
            System.exit(0);
        }

        if (this.state == RunnerState.Ready) {
            if ("hatch".equals(type)) {
                this.state = RunnerState.Hatching;
                this.onHatchMessage(message);
                this.state = RunnerState.Running;
            }
        } else if (this.state == RunnerState.Hatching || this.state == RunnerState.Running) {
            if ("hatch".equals(type)) {
                this.stop();
                this.state = RunnerState.Hatching;
                this.onHatchMessage(message);
                this.state = RunnerState.Running;
            } else if ("stop".equals(type)) {
                this.stop();
                this.state = RunnerState.Stopped;
                Log.debug("Recv stop message from master, all the workers are stopped");
                try {
                    this.rpcClient.send(new Message("client_stopped", null, this.nodeID));
                    this.rpcClient.send(new Message("client_ready", null, this.nodeID));
                } catch (IOException ex) {
                    Log.error(ex);
                }
            }
        } else if (this.state == RunnerState.Stopped) {
            if ("hatch".equals(type)) {
                this.state = RunnerState.Hatching;
                this.onHatchMessage(message);
                this.state = RunnerState.Running;
            }
        }
    }

    public void getReady() {
        this.executor = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());

        this.state = RunnerState.Ready;
        this.executor.submit(new Receiver(this));
        try {
            this.rpcClient.send(new Message("client_ready", null, this.nodeID));
        } catch (IOException ex) {
            Log.error(ex);
        }
        this.executor.submit(new Sender(this));
    }

    private static class RunnerInstanceHolder {
        private static final Runner RUNNER = new Runner();
    }

    private class Receiver implements Runnable {

        private Runner runner;

        protected Receiver(Runner runner) {
            this.runner = runner;
        }

        @Override
        public void run() {
            String name = Thread.currentThread().getName();
            Thread.currentThread().setName(name + "receive-from-client");
            while (true) {
                try {
                    Message message = rpcClient.recv();
                    runner.onMessage(message);
                } catch (Exception ex) {
                    Log.error(ex);
                }
            }
        }
    }

    private class Sender implements Runnable {

        private Runner runner;

        protected Sender(Runner runner) {
            this.runner = runner;
        }

        @Override
        public void run() {
            String name = Thread.currentThread().getName();
            Thread.currentThread().setName(name + "send-to-client");
            while (true) {
                try {
                    Map data = runner.stats.getMessageToRunnerQueue().take();
                    data.put("user_count", runner.numClients);
                    runner.rpcClient.send(new Message("stats", data, runner.nodeID));
                } catch (Exception ex) {
                    Log.error(ex);
                }
            }
        }
    }

}
