package task;

import com.github.myzhan.locust4j.Locust;

/**
 * It's a example about using locust4j's API.
 * It's also very handy when you are writing codes for locust4j, and want to test.
 *
 * @author myzhan
 */
public class Main {

    public static void main(String[] args) {

        // setup locust
        Locust locust = Locust.getInstance();
        locust.setMasterHost("127.0.0.1");
        locust.setMasterPort(5557);

        // run tasks without connecting to master, for debug purpose.
        locust.dryRun(new TaskAlwaysSuccess(), new TaskAlwaysFail());

        // limit max RPS that Locust4j can generate
        locust.setMaxRPS(1000);

        // user specified task
        // task instance is shared across multiple threads
        // if you want to keep some context like Socket, use ThreadLocal
        locust.run(new TaskAlwaysSuccess(), new TaskAlwaysFail());

        // multiply tasks
        // locust.run(new TaskAlwaysSuccess(), new TaskAlwaysFail());
    }
}
