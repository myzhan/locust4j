package com.github.myzhan.locust4j.taskset;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.myzhan.locust4j.AbstractTask;

/**
 * @author nejckorasa
 *
 * OrderedTaskSet ignores weights of individual tasks by default unless {@link #distributeWeights} is called after
 * initialization
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

    /**
     * Creates ordered weighing distribution of tasks
     */
    public void distributeWeights() {
        int weightSum = getWeightSum();

        List<AbstractTask> weighingOrderedTasks = new ArrayList<>();
        for (final AbstractTask task : tasks) {
            int amount = 0 == weightSum ? 1 : task.getWeight();
            for (int i = 1; i <= amount; i++) {
                weighingOrderedTasks.add(task);
            }
        }
        tasks = weighingOrderedTasks;
    }

    public AbstractTask getTask() {
        return tasks.isEmpty() ? null : tasks.get(getNextIndex());
    }

    public Integer getNextIndex() {
        final int size = tasks.size();
        if (size == 0) {
            return null;
        }

        synchronized (lock) {
            int index = position.get() % size;
            position.set(index + 1);
            return index;
        }
    }

    private int getWeightSum() {
        int weightSum = 0;
        for (AbstractTask task : tasks) {
            weightSum += task.getWeight();
        }
        return weightSum;
    }
}
