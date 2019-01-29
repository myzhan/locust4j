package com.github.myzhan.locust4j.taskset;

import java.util.concurrent.atomic.AtomicInteger;

import com.github.myzhan.locust4j.AbstractTask;

/**
 * @author nejckorasa
 *
 * OrderedTaskSet ignores weights of individual tasks
 */
public class OrderedTaskSet extends AbstractTaskSet {

    private int weight;
    private String name;

    private final Object lock = new Object();
    private AtomicInteger position = new AtomicInteger(0);

    public OrderedTaskSet(String name, int weight) {
        super();
        this.name = name;
        this.weight = weight;
    }

    @Override
    public void addTask(AbstractTask task) {
        tasks.add(task);
    }

    @Override
    public int getWeight() {
        return weight;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void execute() throws Exception {
        AbstractTask task = getTask();
        task.execute();
    }

    public AbstractTask getTask() {
        return tasks.isEmpty() ? null : tasks.get(getNextIndex());
    }

    public Integer getNextIndex() {
        final int size = tasks.size();
        if (size == 0) {
            return 0;
        }

        int index;
        synchronized (lock) {
            index = position.get() % size;
            position.set(index + 1);
        }
        return index;
    }
}
