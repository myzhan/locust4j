package com.github.myzhan.locust4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

enum State {
    Ready,
    Hatching,
    Running,
    Stopped,
}

public class Runner {

    protected String nodeID;
    protected int numClients = 0;
    private State state;
    private List<AbstractTask> tasks;
    private int hatchRate = 0;
    private ExecutorService executor;
    private AtomicInteger threadNumber = new AtomicInteger();

    private Runner() {
        this.nodeID = Utils.getNodeID();
    }

    public static Runner getInstance() {
        return RunnerInstanceHolder.RUNNER;
    }

    protected void setTasks(List<AbstractTask> tasks) {
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
                this.executor.submit(task);
            }
        }

        this.hatchComplete();

    }

    protected void startHatching(int spawnCount, int hatchRate) {
        if (this.state != State.Running && this.state != State.Hatching) {
            Queues.CLEAR_STATS.offer(true);
            this.numClients = spawnCount;
        }
        if (this.state == State.Running) {
            this.executor.shutdown();
        }
        this.state = State.Hatching;
        this.hatchRate = hatchRate;
        this.numClients = spawnCount;
        this.threadNumber.set(0);
        this.executor = new ThreadPoolExecutor(this.numClients, this.numClients,0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(),
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("locust4j-worker#" + threadNumber.getAndIncrement());
                    return thread;
                }
            });
        this.spawnWorkers(numClients);
    }

    protected void hatchComplete() {
        Map data = new HashMap(1);
        data.put("count", this.numClients);
        Queues.MESSAGE_TO_MASTER.add(new Message("hatch_complete", data, this.nodeID));
        this.state = State.Running;
    }

    protected void quit() {
        Queues.MESSAGE_TO_MASTER.add(new Message("quit", null, this.nodeID));
    }

    protected void stop() {
        if (this.state == State.Running) {
            this.executor.shutdown();
            try {
                this.executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Log.error(ex.getMessage());
            }
            this.state = State.Stopped;
            this.executor = null;
            Log.debug("Recv stop message from master, all the workers are stopped");
        }
    }

    public void getReady() {
        this.state = State.Ready;

        Locust.getInstance().submitToCoreThreadPool(new Receiver(this));

        Queues.MESSAGE_TO_MASTER.add(new Message("client_ready", null, this.nodeID));

        Locust.getInstance().submitToCoreThreadPool(new Sender(this));

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
                    Message message = Queues.MESSAGE_FROM_MASTER.take();
                    String type = message.getType();
                    if ("hatch".equals(type)) {
                        Queues.MESSAGE_TO_MASTER.add(new Message("hatching", null, runner.nodeID));
                        Float hatchRate = Float.valueOf(message.getData().get("hatch_rate").toString());
                        int numClients = Integer.valueOf(message.getData().get("num_clients").toString());
                        runner.startHatching(numClients, hatchRate.intValue());
                    } else if ("stop".equals(type)) {
                        runner.stop();
                        Queues.MESSAGE_TO_MASTER.add(new Message("client_stopped", null, runner.nodeID));
                        Queues.MESSAGE_TO_MASTER.add(new Message("client_ready", null, runner.nodeID));
                    } else if ("quit".equals(type)) {
                        Log.debug("Got quit message from master, shutting down...");
                        System.exit(0);
                    }
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
                    Map data = Queues.REPORT_TO_RUNNER.take();
                    data.put("user_count", runner.numClients);
                    Queues.MESSAGE_TO_MASTER.add(new Message("stats", data, runner.nodeID));
                } catch (Exception ex) {
                    Log.error(ex);
                }
            }
        }
    }

}
