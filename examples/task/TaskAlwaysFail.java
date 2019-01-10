package task;

import com.github.myzhan.locust4j.Locust;
import com.github.myzhan.locust4j.AbstractTask;

/**
 * This task does nothing but sleep for 1 second, then report failure.
 *
 * @author myzhan
 */
public class TaskAlwaysFail extends AbstractTask {

    public int weight = 10;
    public String name = "fail";

    public TaskAlwaysFail() {
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
    public void execute() throws Exception {
        long startTime = System.currentTimeMillis();
        Thread.sleep(1000);
        long elapsed = System.currentTimeMillis() - startTime;
        Locust.getInstance().recordFailure("http", "failure", elapsed, "timeout");
    }
}
