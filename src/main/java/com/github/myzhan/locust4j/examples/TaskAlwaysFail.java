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
        Locust.getInstance().recordFailure("http", "failure", 1000, "timeout");
        try {
            Thread.sleep(100);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
