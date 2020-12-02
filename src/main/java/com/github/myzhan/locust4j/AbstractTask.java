package com.github.myzhan.locust4j;

import com.github.myzhan.locust4j.runtime.Runner;
import com.github.myzhan.locust4j.runtime.RunnerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link AbstractTask} is the abstraction layer of test scenario, which requires subtypes to implement test scenario
 * in {@link #execute()} method.
 *
 * Instances of task is shared across multiple threads, the {@link #execute()} method must be thread-safe.
 *
 * If you call locust.run(new AwesomeTask()), only one instance of AwesomeTask is used by multiple threads.
 *
 * This behavior is different from locust in python.
 *
 * @author zhanqp
 * @since 1.0.0
 */
public abstract class AbstractTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTask.class);

    /**
     * When locust runs multiple tasks, their weights are used to allocate threads.
     * When locust runs one task set with all the tasks, their weights are used to invoke "execute" method, which means
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
     *
     * @throws Exception test scenarios may throw exception.
     */
    public abstract void execute() throws Exception;

    /**
     * This method will be executed once before the test loop. By default, nothing will be executed.
     *
     * @throws Exception if an execption is thronw then a failure will be recorded and test
     *                   scenarios will not be executed
     */
    public void onStart() throws Exception {

    }

    /**
     * This method will be executed once after the test loop stopped, whether it ends in failure or not.
     * By default, nothing will be executed
     */
    public void onStop() {

    }

    @Override
    public void run() {
        Runner runner = Locust.getInstance().getRunner();

        try {
            onStart();
        } catch (Exception ex) {
            logger.error("Exception when executing onStart", ex);
            Locust.getInstance().recordFailure("onStart", "error", 0, ex.getMessage());
            return;
        }
        try {
            while (true) {
                if (runner.getState() == RunnerState.Stopped || runner.getState() == RunnerState.Ready) {
                    // The runner's state is not spawning or running, so break the loop.
                    return;
                }

                if (Thread.currentThread().isInterrupted()) {
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
                } catch (InterruptedException ex) {
                    return;
                } catch (Exception ex) {
                    logger.error("Unknown exception when executing the task", ex);
                    Locust.getInstance().recordFailure("unknown", "error", 0, ex.getMessage());
                } catch (Error err) {
                    // Error happens, print out the stacktrace then rethrow it to the thread pool.
                    // This task will be discarded by the thread pool.
                    logger.error("Unknown exception when executing the task", err);
                    throw err;
                }
            }
        } finally {
            onStop();
        }
    }
}
