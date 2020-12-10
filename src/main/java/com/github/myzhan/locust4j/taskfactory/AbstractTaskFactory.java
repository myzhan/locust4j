package com.github.myzhan.locust4j.taskfactory;

import com.github.myzhan.locust4j.AbstractTask;

/**
 * @author myzhan
 * @since 1.0.13
 *
 * TaskFactory is an experimental feature, the API is not stabilized.
 * It needs to be more considered and tested.
 *
 * @see com.github.myzhan.locust4j.taskfactory.ThreadLocalTaskFactory
 */
public abstract class AbstractTaskFactory extends AbstractTask {

    /**
     * Create a task instance.
     * @return a new AbstractTask instance.
     */
    public abstract AbstractTask createTask();
}
