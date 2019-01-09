package com.github.myzhan.locust4j.ratelimit;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.github.myzhan.locust4j.Log;

/**
 * This limiter distributes permits at a configurable rate. Each {@link #acquire()} blocks until a permit is available.
 *
 * @author myzhan
 * @date 2018/12/07
 */
public class StableRateLimiter extends AbstractRateLimiter implements Runnable {

    private final long maxThreshold;
    private final AtomicLong threshold;
    private final long period;
    private final TimeUnit unit;
    private ScheduledExecutorService updateTimer;
    private AtomicBoolean stopped;

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
                thread.setName("update-timer");
                return thread;
            }
        });
        updateTimer.scheduleAtFixedRate(this, 0, 1, TimeUnit.SECONDS);
        stopped.set(false);
        Log.debug(String
            .format("Task execute rate is limited to %d per %d %s", maxThreshold, period, unit.name().toLowerCase()));
    }

    @Override
    public void run() {
        Thread.currentThread().setName("stable-rate-limiter");
        while (true) {
            try {
                synchronized (this) {
                    this.threshold.set(maxThreshold);
                    this.notifyAll();
                }
                this.unit.sleep(this.period);
            } catch (InterruptedException ex) {
                return;
            }
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
                    Log.error(ex);
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
