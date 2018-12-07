package com.github.myzhan.locust4j;

import com.github.myzhan.locust4j.runtime.Runner;
import com.github.myzhan.locust4j.runtime.RunnerState;

public abstract class AbstractTask implements Runnable {

    public abstract int getWeight();

    public abstract String getName();

    public abstract void execute();

    @Override
    public void run() {
        Runner runner = Locust.getInstance().getRunner();

        while (true) {
            if (runner.getState().equals(RunnerState.Stopped)) {
                return;
            }

            try {
                if (Locust.getInstance().isRateLimitEnabled()) {
                    // block and wait for next permit
                    boolean blocked = Locust.getInstance().getRateLimiter().acquire();
                    if (!blocked) {
                        this.execute();
                    }
                } else {
                    this.execute();
                }
            } catch (Exception ex) {
                Locust.getInstance().recordFailure("unknown", "error", 0, ex.getMessage());
            }
        }
    }

}
