package taskfactory;

import com.github.myzhan.locust4j.AbstractTask;
import com.github.myzhan.locust4j.Locust;
import com.github.myzhan.locust4j.taskfactory.AbstractTaskFactory;
import com.github.myzhan.locust4j.taskfactory.ThreadLocalTaskFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author myzhan
 */
public class ThreadPerConnection {

    private static class TcpWorkload extends AbstractTask {

        private final String connectionId;

        public TcpWorkload(String connectionId) {
            // We can create real tcp connections.
            this.connectionId = connectionId;
        }

        /**
         * Weight will be ignored when created by a TaskFactory.
         * @return
         */
        @Override
        public int getWeight() {
            return 0;
        }

        /**
         * Name will be ignored when created by a TaskFactory.
         * @return
         */
        @Override
        public String getName() {
            return "TcpWorkload";
        }

        @Override
        public void execute() {
            // Record per connection RPS.
            Locust.getInstance().recordSuccess("tcp", connectionId, 1, 1);
        }
    }

    private static class TcpTaskCreator extends ThreadLocalTaskFactory {

        private final AtomicInteger connectionIdCounter = new AtomicInteger();

        @Override
        public AbstractTask createTask() {
            return new TcpWorkload(String.valueOf(connectionIdCounter.incrementAndGet()));
        }

        @Override
        public int getWeight() {
            return 0;
        }

        @Override
        public String getName() {
            return "TcpTaskCreator";
        }
    }

    public static void main(String[] args) {
        Locust locust = Locust.getInstance();
        AbstractTaskFactory taskFactory = new TcpTaskCreator();
        // Run a task factory instead of a task.
        locust.run(taskFactory);
    }
}
