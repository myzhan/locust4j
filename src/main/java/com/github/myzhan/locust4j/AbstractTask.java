package com.github.myzhan.locust4j;

public abstract class AbstractTask implements Runnable {

    public abstract int getWeight();

    public abstract String getName();

    public abstract void execute();

    @Override
    public void run() {
        Runner runner = Runner.getInstance();

        while (true) {
            if (runner.getState().equals(State.Stopped)) {
                return;
            }

            try {
                if (Locust.getInstance().isMaxRPSEnabled()) {
                    long token = Locust.getInstance().getMaxRPSThreshold().decrementAndGet();
                    if (token < 0) {
                        synchronized (Locust.getInstance().getTaskSyncLock()) {
                            Locust.getInstance().getTaskSyncLock().wait();
                        }
                    } else {
                        this.execute();
                    }
                } else {
                    this.execute();
                }
            } catch (InterruptedException ex) {
                return;
            } catch (Exception ex) {
                Locust.getInstance().recordFailure("unknown", "error", 0, ex.getMessage());
            }
        }
    }

}
