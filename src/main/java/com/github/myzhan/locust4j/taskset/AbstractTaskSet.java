package com.github.myzhan.locust4j.taskset;

import java.util.ArrayList;
import java.util.List;

import com.github.myzhan.locust4j.AbstractTask;

/**
 * @author myzhan
 * @date 2018/12/06
 *
 * TaskSet is an experimental feature, the API is not stabilized.
 * It needs to be more considered and tested.
 */
public abstract class AbstractTaskSet extends AbstractTask {

    protected List<AbstractTask> tasks;

    public AbstractTaskSet() {
        this.tasks = new ArrayList<>();
    }

    public abstract void addTask(AbstractTask task);

}
