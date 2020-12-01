package com.github.myzhan.locust4j;

import com.github.myzhan.locust4j.ratelimit.AbstractRateLimiter;
import com.github.myzhan.locust4j.ratelimit.StableRateLimiter;
import com.github.myzhan.locust4j.runtime.Runner;
import com.github.myzhan.locust4j.runtime.RunnerState;
import com.github.myzhan.locust4j.stats.RequestFailure;
import com.github.myzhan.locust4j.stats.RequestSuccess;
import com.github.myzhan.locust4j.stats.Stats;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author myzhan
 */
public class TestLocust {

    static class TestTask extends AbstractTask {

        private int weight;
        private final String name;
        private boolean onStartCalled = false, onStopCalled = false; 

        public TestTask(int weight, String name) {
            this.weight = weight;
            this.name = name;
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
        public void execute() throws Exception {
            weight++;
        }
        
        @Override
        public void onStart() throws Exception {
            onStartCalled=true;
        }

        @Override
        public void onStop() {
            onStopCalled=true;
        }

    }

    @Test
    public void TestDefaultRateLimiter() {
        Locust.getInstance().setMaxRPS(1000);
        AbstractRateLimiter rateLimiter = Locust.getInstance().getRateLimiter();

        assertTrue(Locust.getInstance().isRateLimitEnabled());

        // defaults to StableRateLimiter
        assertTrue(rateLimiter instanceof StableRateLimiter);
    }

    @Test
    public void TestDryRun() {
        TestTask task = new TestTask(1, "test");

        Locust.getInstance().dryRun(task);
        assertEquals(2, task.getWeight());

        ArrayList<AbstractTask> tasks = new ArrayList<>();
        tasks.add(task);

        Locust.getInstance().dryRun(tasks);
        assertEquals(3, task.getWeight());        
    }    

    class TestRunner extends Runner {
        RunnerState state;
        public void setState(RunnerState state) {
            this.state = state;
        }
        @Override
        public RunnerState getState() {          
            return state;
        }
    }

    @Test
    public void TestRunnerOnStartAndOnStop() {
        TestRunner runner = new TestRunner();
        Locust.getInstance().setRunner(runner);
        TestTask task = new TestTask(1, "test");
        runner.setState(RunnerState.Stopped);
        task.run();
        assertTrue(task.onStartCalled);
        assertTrue(task.onStopCalled);
    }

    @Test
    public void TestOnStartAndOnStop() {
        TestTask task = new TestTask(1, "test");
        Locust.getInstance().dryRun(task);

        assertTrue("onStart must be called", task.onStartCalled);
        assertTrue("onStop must be called", task.onStopCalled);        

        ArrayList<AbstractTask> tasks = new ArrayList<>();
        tasks.add(task);

        Locust.getInstance().dryRun(tasks);
        assertTrue("onStart must be called", task.onStartCalled);
        assertTrue("onStop must be called", task.onStopCalled);        
        task = new TestTask(0, "test") {
            @Override
            public void execute() throws Exception {            
              throw new Exception("Test");
            }
        };
        Locust.getInstance().dryRun(task);
        assertTrue("onStart must be called", task.onStartCalled);
        assertTrue("onStop must be called", task.onStopCalled);
    }

    @Test
    public void TestRecordSuccess() {
        Locust.getInstance().recordSuccess("http", "success", 1, 10);
        RequestSuccess success = Stats.getInstance().getReportSuccessQueue().poll();
        assertEquals("http", success.getRequestType());
        assertEquals("success", success.getName());
        assertEquals(1, success.getResponseTime());
        assertEquals(10, success.getContentLength());
    }

    @Test
    public void TestRecordFailure() {
        Locust.getInstance().recordFailure("http", "failure", 1, "error");
        RequestFailure failure = Stats.getInstance().getReportFailureQueue().poll();
        assertEquals("http", failure.getRequestType());
        assertEquals("failure", failure.getName());
        assertEquals(1, failure.getResponseTime());
        assertEquals("error", failure.getError());
    }

    @Test
    public void TestGetRemoteParam() {
        assertEquals(null, Locust.getInstance().getRemoteParam("none"));
    }
}
