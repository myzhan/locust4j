package com.github.myzhan.locust4j.taskset;

import java.util.ArrayList;
import java.util.List;

import com.github.myzhan.locust4j.AbstractTask;

/**
 * @author myzhan
 * @since 1.0.3
 *
 * TaskSet is an experimental feature, the API is not stabilized.
 * It needs to be more considered and tested.
 */
public abstract class AbstractTaskSet extends AbstractTask {

    protected List<AbstractTask> tasks;

    public AbstractTaskSet() {
        this.tasks = new ArrayList<>();
    }

    /**
     * Task set is valid if it contains tasks
     *
     * @return true if task set is valid
     */
    @Override
    public boolean isValid() {
        return super.isValid() && !tasks.isEmpty();
    }

    /**
     * Add a task to the task set.
     *
     * @param task test task that runs in a task set
     */
    public abstract void addTask(AbstractTask task);

}
