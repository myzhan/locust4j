import com.github.myzhan.locust4j.Locust;
import com.github.myzhan.locust4j.AbstractTask;

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
    public void execute() {
        try {
            long startTime = System.currentTimeMillis();
            Thread.sleep(10);
            long elapsed = System.currentTimeMillis() - startTime;
            Locust.getInstance().recordSuccess("http", "success", elapsed, 1);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
