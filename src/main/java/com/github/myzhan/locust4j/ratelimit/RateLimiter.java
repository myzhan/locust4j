package com.github.myzhan.locust4j.ratelimit;

/**
 * @author myzhan
 * @date 2018/12/07
 */
public abstract class RateLimiter {

    /**
     * rate limiter only works after started.
     */
    public abstract void start();

    /**
     * Acquire a permit from rate limiter.
     * @return blocked
     */
    public abstract boolean acquire();

    /**
     * Update threshold.
     */
    public abstract void update();

    /**
     * Stop the rate limiter.
     */
    public abstract void stop();

    /**
     * Is rate limiter stopped.
     * @return stopped
     */
    public abstract boolean isStopped();

}
