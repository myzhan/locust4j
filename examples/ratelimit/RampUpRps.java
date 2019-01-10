package ratelimit;

import com.github.myzhan.locust4j.AbstractTask;
import com.github.myzhan.locust4j.Locust;
import com.github.myzhan.locust4j.ratelimit.AbstractRateLimiter;
import com.github.myzhan.locust4j.ratelimit.RampUpRateLimiter;

import java.util.concurrent.TimeUnit;

/**
 * @author zhanqp
 */
public class RampUpRps {

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

        // use a custom rate limiter
        AbstractRateLimiter rateLimiter = new RampUpRateLimiter(100, 10, 1, TimeUnit.SECONDS,
                1, TimeUnit.SECONDS);
        locust.setRateLimiter(rateLimiter);

        locust.run(new TestTask("foo", 10));
    }
}
