package com.github.myzhan.locust4j.taskset;

import com.github.myzhan.locust4j.AbstractTask;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author myzhan
 * @date 2018/12/06
 */
public class WeighingTaskSetTest {

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
                System.out.println("I'm " + this.name);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void TestDistribution() {
        AbstractTaskSet taskSet = new WeighingTaskSet("testWeighingTaskSet", 1);

        // range of test1 is [0, 10)
        taskSet.addTask(new TestTask("test1", 10));
        // range of test2 is [10, 30)
        taskSet.addTask(new TestTask("test2", 20));
        // range if test3 is [31, 50)
        taskSet.addTask(new TestTask("test3", 20));

        Assert.assertEquals(null, ((WeighingTaskSet)taskSet).getTask(-1));
        Assert.assertEquals(null, ((WeighingTaskSet)taskSet).getTask(50));

        Assert.assertEquals("test1", ((WeighingTaskSet)taskSet).getTask(0).getName());
        Assert.assertEquals("test1", ((WeighingTaskSet)taskSet).getTask(9).getName());

        Assert.assertEquals("test2", ((WeighingTaskSet)taskSet).getTask(10).getName());
        Assert.assertEquals("test2", ((WeighingTaskSet)taskSet).getTask(29).getName());

        Assert.assertEquals("test3", ((WeighingTaskSet)taskSet).getTask(31).getName());
        Assert.assertEquals("test3", ((WeighingTaskSet)taskSet).getTask(49).getName());
    }
}
