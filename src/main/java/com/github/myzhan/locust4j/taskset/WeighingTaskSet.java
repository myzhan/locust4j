package com.github.myzhan.locust4j.taskset;

import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.myzhan.locust4j.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author myzhan
 * @since 1.0.3
 */
public class WeighingTaskSet extends AbstractTaskSet {

    private static final Logger logger = LoggerFactory.getLogger(WeighingTaskSet.class);

    private final int weight;
    private final String name;
    private final NavigableMap<Integer, AbstractTask> randomMap = new ConcurrentSkipListMap<>();
    private final AtomicInteger offset = new AtomicInteger(0);

    public WeighingTaskSet(String name, int weight) {
        super();
        this.name = name;
        this.weight = weight;
    }

    @Override
    public void addTask(AbstractTask task) {
        if (task.getWeight() <= 0) {
            logger.error("The weight of task {} is {}, ignored.", task.getName(), task.getWeight());
            return;
        }
        tasks.add(task);

        Integer nextOffset = offset.addAndGet(task.getWeight());
        randomMap.put(nextOffset, task);
    }

    public AbstractTask getTask(int roll) {
        if (roll < 0 || roll >= offset.get()) {
            return null;
        }

        Map.Entry<Integer, AbstractTask> entry = randomMap.higherEntry(roll);
        if (null == entry) {
            return null;
        }
        return entry.getValue();
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
        int roll = ThreadLocalRandom.current().nextInt(offset.get());
        AbstractTask task = getTask(roll);
        task.execute();
    }
}
