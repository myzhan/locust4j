package taskset;

import com.github.myzhan.locust4j.AbstractTask;
import com.github.myzhan.locust4j.Locust;
import com.github.myzhan.locust4j.taskset.AbstractTaskSet;
import com.github.myzhan.locust4j.taskset.WeighingTaskSet;

/**
 * @author myzhan
 * @date 2018/12/07
 */
public class WeighingRps {

    private static class TestTask extends AbstractTask {
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
            Locust.getInstance().recordSuccess("test", name, 1, 1);
        }
    }

    public static void main(String[] args) {
        Locust locust = Locust.getInstance();
        locust.setVerbose(true);
        locust.setMaxRPS(1000);

        AbstractTaskSet taskSet = new WeighingTaskSet("test", 1);

        // RPS(foo) / RPS(bar) is expected to be close to 1/2
        taskSet.addTask(new TestTask("foo", 10));
        taskSet.addTask(new TestTask("bar", 20));

        locust.run(taskSet);
    }

}
