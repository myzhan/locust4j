package com.github.myzhan.locust4j.ratelimit;

/**
 * @author myzhan
 * @since 1.0.3
 */
public abstract class AbstractRateLimiter {

    /**
     * rate limiter only works after started.
     */
    public abstract void start();

    /**
     * Acquire a permit from rate limiter.
     *
     * @return blocked
     */
    public abstract boolean acquire();

    /**
     * Stop the rate limiter.
     */
    public abstract void stop();

    /**
     * Is rate limiter stopped.
     *
     * @return stopped
     */
    public abstract boolean isStopped();

}
