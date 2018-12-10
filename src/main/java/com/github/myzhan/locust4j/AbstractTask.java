package com.github.myzhan.locust4j;

import com.github.myzhan.locust4j.runtime.Runner;
import com.github.myzhan.locust4j.runtime.RunnerState;

/**
 * An abstraction layer of test scenario, which requires subtypes to implement test scenario in
 * method {@link #execute()}.
 *
 * Instances of task is shared across multiple threads, the execute method must be thread-safe.
 *
 * If you call locust.run(new AwesomeTask()), only one instance of AwesomeTask is used by multiple threads.
 *
 * This behavior is different from locust in python.
 *
 * @author zhanqp
 * @date 2017/11/28
 */
public abstract class AbstractTask implements Runnable {

    /**
     * When locust runs multiple tasks, their weights are used to allocate threads.
     * When locust runs one task set with all the tasks, their weights are used to call "execute" method, which means
     * RPS most of the time.
     *
     * @return the weight
     */
    public abstract int getWeight();

    /**
     * Get the name of task.
     *
     * @return the name
     */
    public abstract String getName();

    /**
     * Test scenarios should be implemented in this method, like sending http request.
     */
    public abstract void execute() throws Exception;

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
                Log.error(ex);
                Locust.getInstance().recordFailure("unknown", "error", 0, ex.getMessage());
            }
        }
    }
}
