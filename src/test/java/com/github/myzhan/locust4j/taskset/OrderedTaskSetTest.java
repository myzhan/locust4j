package com.github.myzhan.locust4j.taskset;

import com.github.myzhan.locust4j.AbstractTask;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author nejckorasa
 * @date 2019/01/28
 */
public class OrderedTaskSetTest
{
    private static class TestTask extends AbstractTask {
        public int weight;
        public String name;

        private TestTask(String name, int weight) {
            this.name = name;
            this.weight = weight;
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
        public void execute() {
            try {
                System.out.println("I'm " + name);
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void testOrderedTasksDistribution() throws Exception
    {
        OrderedTaskSet taskSet = new OrderedTaskSet("orderedTaskSet", 1);

        // taskSet size = 5
        taskSet.addTask(new TestTask("test1", 10));
        taskSet.addTask(new TestTask("test2", 20));
        taskSet.addTask(new TestTask("test3", 20));
        taskSet.addTask(new TestTask("test4", 20));
        taskSet.addTask(new TestTask("test5", 20));

        // execute 7 times
        taskSet.execute();
        taskSet.execute();
        taskSet.execute();
        taskSet.execute();
        taskSet.execute();
        taskSet.execute();
        taskSet.execute();

        Assert.assertEquals(2, taskSet.getNextIndex());
        Assert.assertEquals(3, taskSet.getNextIndex());
        Assert.assertEquals(4, taskSet.getNextIndex());
    }
}
