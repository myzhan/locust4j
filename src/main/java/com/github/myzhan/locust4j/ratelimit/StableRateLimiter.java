package com.github.myzhan.locust4j.ratelimit;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link StableRateLimiter} distributes permits at a configurable rate.
 * Each {@link #acquire()} blocks until a permit is available.
 *
 * @author myzhan
 * @since 1.0.3
 */
public class StableRateLimiter extends AbstractRateLimiter implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(StableRateLimiter.class);

    private final long maxThreshold;
    private final AtomicLong threshold;
    private final long period;
    private final TimeUnit unit;
    private ScheduledExecutorService updateTimer;
    private final AtomicBoolean stopped;

    public StableRateLimiter(long maxThreshold) {
        this(maxThreshold, 1, TimeUnit.SECONDS);
    }

    public StableRateLimiter(long maxThreshold, long period, TimeUnit unit) {
        this.maxThreshold = maxThreshold;
        this.threshold = new AtomicLong(maxThreshold);
        this.period = period;
        this.unit = unit;
        this.stopped = new AtomicBoolean(true);
    }

    @Override
    public void start() {
        updateTimer = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("StableRateLimiter-bucket-updater");
                return thread;
            }
        });
        updateTimer.scheduleAtFixedRate(this, 0, period, unit);
        stopped.set(false);
    }

    @Override
    public void run() {
        // NOTICE: this method is invoked in a thread pool, make sure it throws no exceptions.
        synchronized (this) {
            this.threshold.set(maxThreshold);
            this.notifyAll();
        }
    }

    @Override
    public boolean acquire() {
        long permit = this.threshold.decrementAndGet();
        if (permit < 0) {
            synchronized (this) {
                try {
                    this.wait();
                } catch (InterruptedException ex) {
                    logger.error("The process of acquiring a permit from rate limiter was interrupted", ex);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void stop() {
        updateTimer.shutdownNow();
        stopped.set(true);
    }

    @Override
    public boolean isStopped() {
        return stopped.get();
    }
}
