package com.github.myzhan.locust4j.taskset;

import com.github.myzhan.locust4j.AbstractTask;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author myzhan
 */
public class TestWeighingTaskSet {

    private static final Logger logger = LoggerFactory.getLogger(TestWeighingTaskSet.class);

    private class TestTask extends AbstractTask {
        public int weight;
        public String name;

        public TestTask(String name, int weight) {
            this.name = name;
            this.weight = weight;
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
                logger.debug("I'm {}", this.name);
            } catch (Exception ex) {
                logger.error(ex.getMessage());
            }
        }
    }

    @Test
    public void TestDistribution() {
        WeighingTaskSet taskSet = new WeighingTaskSet("testWeighingTaskSet", 1);

        // range of test1 is [0, 10)
        taskSet.addTask(new TestTask("test1", 10));
        // range of test2 is [10, 30)
        taskSet.addTask(new TestTask("test2", 20));
        // range if test3 is [31, 50)
        taskSet.addTask(new TestTask("test3", 20));

        assertNull(taskSet.getTask(-1));
        assertNull(taskSet.getTask(50));

        assertEquals("test1", taskSet.getTask(0).getName());
        assertEquals("test1", taskSet.getTask(9).getName());

        assertEquals("test2", taskSet.getTask(10).getName());
        assertEquals("test2", taskSet.getTask(29).getName());

        assertEquals("test3", taskSet.getTask(31).getName());
        assertEquals("test3", taskSet.getTask(49).getName());
    }
}
