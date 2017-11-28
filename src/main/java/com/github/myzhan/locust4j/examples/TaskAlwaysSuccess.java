package com.github.myzhan.locust4j.examples;

import com.github.myzhan.locust4j.Locust;
import com.github.myzhan.locust4j.AbstractTask;

public class TaskAlwaysSuccess extends AbstractTask {

    public int weight = 20;
    public String name = "success";

    public TaskAlwaysSuccess() {
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
        Locust.getInstance().recordSuccess("http", "success", 100, 1);
        try {
            Thread.sleep(10);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
