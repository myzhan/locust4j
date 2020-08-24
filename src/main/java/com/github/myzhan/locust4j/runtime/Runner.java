package com.github.myzhan.locust4j.runtime;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.myzhan.locust4j.AbstractTask;
import com.github.myzhan.locust4j.Locust;
import com.github.myzhan.locust4j.message.Message;
import com.github.myzhan.locust4j.rpc.Client;
import com.github.myzhan.locust4j.stats.Stats;
import com.github.myzhan.locust4j.utils.Utils;
import com.sun.management.OperatingSystemMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Runner} is a state machine that tells to the master, runs all tasks, collects test results
 * and reports to the master.
 *
 * @author myzhan
 */
public class Runner {

    private static final Logger logger = LoggerFactory.getLogger(Runner.class);

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
     * Spawn rate required by the master.
     * Spawn rate means clients/s.
     */
    private int spawnRate = 0;

    /**
     * Remote params sent from the master, which is set before spawning begins.
     */
    private Map<String, String> remoteParams = new ConcurrentHashMap<>();

    /**
     * Thread pool used by runner, it will be re-created when runner starts spawning.
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

    /**
     * Disable heartbeat request.
     */
    private AtomicBoolean heartbeatStopped = new AtomicBoolean(false);

    protected void setHeartbeatStopped(boolean value) {
        heartbeatStopped.set(value);
    }

    protected boolean isHeartbeatStopped() {
        return heartbeatStopped.get();
    }

    public Runner() {
        this.nodeID = Utils.getNodeID();
    }

    public RunnerState getState() {
        return this.state;
    }

    public String getNodeID() {
        return this.nodeID;
    }

    public void setRPCClient(Client client) {
        this.rpcClient = client;
    }

    public Map<String, String> getRemoteParams() {
        return this.remoteParams;
    }

    public void setStats(Stats stats) {
        this.stats = stats;
    }

    public void setTasks(List<AbstractTask> tasks) {
        this.tasks = tasks;
    }

    private void spawnWorkers(int spawnCount) {
        logger.debug("Spawning {} clients at the rate {} clients/s...", spawnCount, this.spawnRate);

        float weightSum = 0;
        for (AbstractTask task : this.tasks) {
            weightSum += task.getWeight();
        }

        for (AbstractTask task : this.tasks) {
            int amount;
            if (0 == weightSum) {
                amount = spawnCount / this.tasks.size();
            } else {
                float percent = task.getWeight() / weightSum;
                amount = Math.round(spawnCount * percent);
            }

            logger.debug("Allocating {} threads to task, which name is {}", amount, task.getName());

            for (int i = 1; i <= amount; i++) {
                if (i % this.spawnRate == 0) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception ex) {
                        logger.error(ex.getMessage());
                    }
                }
                this.numClients++;
                this.taskExecutor.submit(task);
            }
        }
    }

    protected void startSpawning(int spawnCount, int spawnRate) {
        stats.getClearStatsQueue().offer(true);
        Stats.getInstance().wakeMeUp();

        this.spawnRate = spawnRate;
        this.numClients = 0;
        this.threadNumber.set(0);
        this.taskExecutor = new ThreadPoolExecutor(spawnCount, spawnCount, 0L, TimeUnit.MILLISECONDS,
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

    protected void spawnComplete() {
        Map<String, Object> data = new HashMap<>(1);
        data.put("count", this.numClients);
        try {
            this.rpcClient.send((new Message("spawning_complete", data, this.nodeID)));
        } catch (IOException ex) {
            logger.error("Error while sending a message about the completed spawn", ex);
        }
    }

    public void quit() {
        try {
            this.rpcClient.send(new Message("quit", null, this.nodeID));
            this.executor.shutdownNow();
        } catch (IOException ex) {
            logger.error("Error while sending a message about quiting", ex);
        }
    }

    private void shutdownThreadPool() {
        this.taskExecutor.shutdownNow();
        try {
            this.taskExecutor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            logger.error("Error while waiting for termination", ex);
        }
        this.taskExecutor = null;
    }

    protected void stop() {
        this.shutdownThreadPool();
    }

    private boolean spawnMessageIsValid(Message message) {
        Map data = message.getData();
        if (!data.containsKey("spawn_rate")) {
            logger.debug("Invalid spawn message without spawn_rate, you may use a newer but incompatible version of locust.");
            return false;
        }
        if (!data.containsKey("num_users")) {
            logger.debug("Invalid spawn message without num_users, you may use a newer but incompatible version of locust.");
            return false;
        }
        float spawnRate = Float.parseFloat(data.get("spawn_rate").toString());
        int numUsers = 0;
        if (data.containsKey("num_users")) {
            numUsers = Integer.parseInt(data.get("num_users").toString());
        }
        if ((int)spawnRate == 0 || numUsers == 0) {
            logger.debug("Invalid message (spawn_rate: {}, num_users: {}) from master, ignored.",
                    (int)spawnRate, numUsers);
            return false;
        }
        return true;
    }

    private void onSpawnMessage(Message message) {
        Map data = message.getData();
        float spawnRate = Float.parseFloat(data.get("spawn_rate").toString());
        int numUsers = 0;
        if (data.containsKey("num_users")) {
            numUsers = Integer.parseInt(message.getData().get("num_users").toString());
        } else if (data.containsKey("num_clients")) {
            // keep compatible with previous version of locust
            numUsers = Integer.parseInt(message.getData().get("num_clients").toString());
        }
        try {
            this.rpcClient.send(new Message("spawning", null, this.nodeID));
        } catch (IOException ex) {
            logger.error("Error while sending a message about spawning", ex);
        }

        this.remoteParams.put("spawn_rate", String.valueOf(spawnRate));
        this.remoteParams.put("num_users", String.valueOf(numUsers));
        if (data.get("host") != null) {
            this.remoteParams.put("host", data.get("host").toString());
        }

        this.startSpawning(numUsers, (int)spawnRate);
        this.spawnComplete();
    }

    private void onMessage(Message message) {
        String type = message.getType();

        if ("hatch".equals(type)) {
            logger.error("The master sent a 'hatch' message, you are using an unsupported locust version, please update locust to 1.2.");
        }

        if (!"spawn".equals(type) && !"stop".equals(type) && !"quit".equals(type)) {
            logger.error("Got {} message from master, which is not supported, please report an issue to locust4j.", type);
            return;
        }

        if ("quit".equals(type)) {
            logger.debug("Got quit message from master, shutting down...");
            System.exit(0);
        }

        if (this.state == RunnerState.Ready) {
            if ("spawn".equals(type) && spawnMessageIsValid(message)) {
                this.state = RunnerState.Spawning;
                this.onSpawnMessage(message);

                if (null != Locust.getInstance().getRateLimiter()) {
                    Locust.getInstance().getRateLimiter().start();
                }

                this.state = RunnerState.Running;
            }
        } else if (this.state == RunnerState.Spawning || this.state == RunnerState.Running) {
            if ("spawn".equals(type) && spawnMessageIsValid(message)) {
                this.stop();
                this.state = RunnerState.Spawning;
                this.onSpawnMessage(message);
                this.state = RunnerState.Running;
            } else if ("stop".equals(type)) {
                this.stop();

                if (null != Locust.getInstance().getRateLimiter()) {
                    Locust.getInstance().getRateLimiter().stop();
                }

                this.state = RunnerState.Stopped;
                logger.debug("Recv stop message from master, all the workers are stopped");
                try {
                    this.rpcClient.send(new Message("client_stopped", null, this.nodeID));
                    this.rpcClient.send(new Message("client_ready", null, this.nodeID));
                    this.state = RunnerState.Ready;
                } catch (IOException ex) {
                    logger.error("Error while switching from the state stopped to ready", ex);
                }
            }
        } else if (this.state == RunnerState.Stopped) {
            if ("spawn".equals(type) && spawnMessageIsValid(message)) {
                this.state = RunnerState.Spawning;
                this.onSpawnMessage(message);

                if (null != Locust.getInstance().getRateLimiter()) {
                    Locust.getInstance().getRateLimiter().start();
                }

                this.state = RunnerState.Running;
            }
        }
    }

    public void getReady() {
        this.executor = new ThreadPoolExecutor(3, 3, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r);
            }
        });

        this.state = RunnerState.Ready;
        this.executor.submit(new Receiver(this));
        try {
            this.rpcClient.send(new Message("client_ready", null, this.nodeID));
        } catch (IOException ex) {
            logger.error("Error while sending a message that the system is ready", ex);
        }
        this.executor.submit(new Sender(this));
        this.executor.submit(new Heartbeat(this));
    }

    private class Receiver implements Runnable {
        private Runner runner;

        private Receiver(Runner runner) {
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
                    logger.error("Error while receiving a message", ex);
                }
            }
        }
    }

    private class Sender implements Runnable {
        private Runner runner;

        private Sender(Runner runner) {
            this.runner = runner;
        }

        @Override
        public void run() {
            String name = Thread.currentThread().getName();
            Thread.currentThread().setName(name + "send-to-client");
            while (true) {
                try {
                    Map<String, Object> data = runner.stats.getMessageToRunnerQueue().take();
                    if (runner.state == RunnerState.Ready || runner.state == RunnerState.Stopped) {
                        continue;
                    }
                    data.put("user_count", runner.numClients);
                    runner.rpcClient.send(new Message("stats", data, runner.nodeID));
                } catch (InterruptedException ex) {
                    return;
                } catch (Exception ex) {
                    logger.error("Error in running the sender", ex);
                }
            }
        }
    }

    private class Heartbeat implements Runnable {
        private static final int HEARTBEAT_INTERVAL = 1000;
        private Runner runner;

        private OperatingSystemMXBean osBean = getOsBean();

        private Heartbeat(Runner runner) {
            this.runner = runner;
        }

        @Override
        public void run() {
            String name = Thread.currentThread().getName();
            Thread.currentThread().setName(name + "heartbeat");
            while (true) {
                try {
                    Thread.sleep(HEARTBEAT_INTERVAL);
                    if (runner.isHeartbeatStopped()) {
                        continue;
                    }
                    Map<String, Object> data = new HashMap<>(2);
                    data.put("state", runner.state.name().toLowerCase());
                    data.put("current_cpu_usage", getCpuUsage());
                    runner.rpcClient.send(new Message("heartbeat", data, runner.nodeID));
                } catch (InterruptedException ex) {
                    return;
                } catch (Exception ex) {
                    logger.error("Error in running the heartbeat", ex);
                }
            }
        }

        private double getCpuUsage() {
            return osBean.getSystemCpuLoad() * 100;
        }

        private OperatingSystemMXBean getOsBean() {
            return (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        }
    }

}
