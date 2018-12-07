package com.github.myzhan.locust4j.ratelimit;

import java.util.concurrent.TimeUnit;

/**
 * @author myzhan
 * @date 2018/12/07
 */
public class RateUpdater implements Runnable {

    private long period;
    private TimeUnit unit;
    private final RateLimiter rateLimiter;

    public RateUpdater(RateLimiter rateLimiter) {
        this(rateLimiter, 1, TimeUnit.SECONDS);
    }

    public RateUpdater(RateLimiter rateLimiter, long period, TimeUnit unit) {
        this.rateLimiter = rateLimiter;
        this.period = period;
        this.unit = unit;
    }

    @Override
    public void run() {
        String name = Thread.currentThread().getName();
        Thread.currentThread().setName(name + "token-updater");
        while (true) {
            try {
                synchronized (this.rateLimiter) {
                    this.rateLimiter.update();
                    this.rateLimiter.notifyAll();
                }
                this.unit.sleep(period);
            } catch (InterruptedException ex) {
                return;
            }
        }
    }
}
