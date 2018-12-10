package task;

import com.github.myzhan.locust4j.Locust;
import com.github.myzhan.locust4j.AbstractTask;

/**
 * This task does nothing but sleep for 100 milliseconds, then report success.
 *
 * @author myzhan
 * @date 2017/11/28
 */
public class TaskAlwaysSuccess extends AbstractTask {

    public int weight = 20;
    public String name = "success";

    public TaskAlwaysSuccess() {
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
        Thread.sleep(10);
        long elapsed = System.currentTimeMillis() - startTime;
        Locust.getInstance().recordSuccess("http", "success", elapsed, 1);
    }
}
