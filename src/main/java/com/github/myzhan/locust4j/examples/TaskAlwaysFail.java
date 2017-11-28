package com.github.myzhan.locust4j.examples;

import com.github.myzhan.locust4j.Locust;
import com.github.myzhan.locust4j.AbstractTask;

public class TaskAlwaysFail extends AbstractTask {

    public int weight = 10;
    public String name = "fail";

    public TaskAlwaysFail() {
    }

    @Override
    public int getWeight() {
        return this.weight;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void execute() {
        try {
            long startTime = System.currentTimeMillis();
            Thread.sleep(1000);
            long elapsed = System.currentTimeMillis() - startTime;
            Locust.getInstance().recordFailure("http", "failure", elapsed, "timeout");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
