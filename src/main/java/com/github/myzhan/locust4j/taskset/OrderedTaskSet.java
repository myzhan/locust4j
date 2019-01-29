package com.github.myzhan.locust4j.taskset;

import java.util.concurrent.atomic.AtomicInteger;

import com.github.myzhan.locust4j.AbstractTask;

/**
 * @author nejckorasa
 * @date 2019/01/28
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

    public AbstractTask getTask(int index) {
        return tasks.get(index % tasks.size());
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
        final int nextIndex = getNextIndex();
        AbstractTask task = getTask(nextIndex);
        task.execute();
    }

    public int getNextIndex() {
        final int size = tasks.size();
        int index;
        synchronized (lock) {
            index = position.get() % size;
            position.set(index + 1);
        }
        return index;
    }
}
