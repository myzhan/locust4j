package com.github.myzhan.locust4j.ratelimit;

import java.util.concurrent.TimeUnit;

/**
 * @author myzhan
 * @date 2018/12/07
 */
public class RateUpdater implements Runnable {

    private long period;
    private TimeUnit unit;
    private final AbstractRateLimiter abstractRateLimiter;

    public RateUpdater(AbstractRateLimiter abstractRateLimiter) {
        this(abstractRateLimiter, 1, TimeUnit.SECONDS);
    }

    public RateUpdater(AbstractRateLimiter abstractRateLimiter, long period, TimeUnit unit) {
        this.abstractRateLimiter = abstractRateLimiter;
        this.period = period;
        this.unit = unit;
    }

    @Override
    public void run() {
        String name = Thread.currentThread().getName();
        Thread.currentThread().setName(name + "token-updater");
        while (true) {
            try {
                synchronized (this.abstractRateLimiter) {
                    this.abstractRateLimiter.update();
                    this.abstractRateLimiter.notifyAll();
                }
                this.unit.sleep(period);
            } catch (InterruptedException ex) {
                return;
            }
        }
    }
}
