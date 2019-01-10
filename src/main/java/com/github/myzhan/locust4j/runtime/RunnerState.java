package com.github.myzhan.locust4j.runtime;

/**
 * State of runner
 *
 * @author myzhan
 */
public enum RunnerState {
    /**
     * Runner is ready to receive message from master.
     */
    Ready,

    /**
     * Runner is submitting tasks to its thread pool.
     */
    Hatching,

    /**
     * Runner is done with submitting tasks.
     */
    Running,

    /**
     * Runner is stopped, its thread pool is destroyed, the test is stopped.
     */
    Stopped,
}
