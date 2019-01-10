package com.github.myzhan.locust4j.ratelimit;

import com.github.myzhan.locust4j.Log;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A {@link RampUpRateLimiter} distributes permits at a ramp-up rate, in steps.
 * Each {@link #acquire()} blocks until a permit is available.
 *
 * @author myzhan
 * @since 1.0.4
 */
public class RampUpRateLimiter extends AbstractRateLimiter {

    private final long maxThreshold;
    private AtomicLong nextThreshold;
    private final AtomicLong threshold;

    private final long rampUpStep;
    private final long rampUpPeriod;
    private final TimeUnit rampUpTimeUnit;

    private final long refillPeriod;
    private final TimeUnit refillUnit;

    private ScheduledExecutorService bucketUpdater;
    private ScheduledExecutorService thresholdUpdater;
    private final Object lock = new Object();
    private AtomicBoolean stopped;

    /**
     * Creates a {@code RampUpRateLimiter}
     *
     * @param maxThreshold   the max threshold that should not be overstepped.
     * @param rampUpStep     the ramp-up step.
     * @param rampUpPeriod   the duration of the period where the {@code RampUpRateLimiter} ramps up the threshold.
     * @param rampUpTimeUnit the time unit.
     * @param refillPeriod   the duration of the period where the {@code RampUpRateLimiter} updates the bucket.
     * @param refillUnit     the time unit.
     */
    public RampUpRateLimiter(long maxThreshold, long rampUpStep, long rampUpPeriod, TimeUnit rampUpTimeUnit,
                             long refillPeriod, TimeUnit refillUnit) {
        this.maxThreshold = maxThreshold;
        this.threshold = new AtomicLong(0);
        this.nextThreshold = new AtomicLong(0);
        this.rampUpStep = rampUpStep;
        this.rampUpPeriod = rampUpPeriod;
        this.rampUpTimeUnit = rampUpTimeUnit;
        this.refillPeriod = refillPeriod;
        this.refillUnit = refillUnit;
        this.stopped = new AtomicBoolean(true);
    }

    @Override
    public void start() {
        thresholdUpdater = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("StableRateLimiter-threshold-updater");
                return thread;
            }
        });
        thresholdUpdater.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                long nextValue = nextThreshold.get() + rampUpStep;
                if (nextValue < 0) {
                    // long value overflow
                    nextValue = Long.MAX_VALUE;
                }
                if (nextValue > maxThreshold) {
                    nextValue = maxThreshold;
                }
                nextThreshold.set(nextValue);
            }
        }, 0, rampUpPeriod, rampUpTimeUnit);

        bucketUpdater = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("StableRateLimiter-bucket-updater");
                return thread;
            }
        });
        bucketUpdater.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    threshold.set(nextThreshold.get());
                    lock.notifyAll();
                }
            }
        }, 0, refillPeriod, refillUnit);

        stopped.set(false);
    }

    @Override
    public boolean acquire() {
        long permit = this.threshold.decrementAndGet();
        if (permit < 0) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException ex) {
                    Log.error(ex);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void stop() {
        bucketUpdater.shutdownNow();
        thresholdUpdater.shutdownNow();
        stopped.set(true);
    }

    @Override
    public boolean isStopped() {
        return stopped.get();
    }
}
