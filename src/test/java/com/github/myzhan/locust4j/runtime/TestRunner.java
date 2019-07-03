package com.github.myzhan.locust4j.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.github.myzhan.locust4j.AbstractTask;
import com.github.myzhan.locust4j.Locust;
import com.github.myzhan.locust4j.message.Message;
import com.github.myzhan.locust4j.stats.Stats;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author myzhan
 */
public class TestRunner {

    private Runner runner;

    @Before
    public void before() {
        runner = new Runner();
        runner.setStats(new Stats());
        runner.setTasks(Collections.singletonList((AbstractTask) new TestTask()));
    }

    private class TestTask extends AbstractTask {
        private final int weight = 1;
        private final String name = "test";

        public TestTask() {
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
                Thread.sleep(10);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void TestStartHatching() {

        runner.startHatching(10, 10);

        assertEquals(10, runner.numClients);

        runner.stop();
    }

    @Test
    public void TestOnInvalidHatchMessage() {

        Locust.getInstance().setVerbose(true);

        MockRPCClient client = new MockRPCClient();

        runner.setRPCClient(client);

        runner.getReady();

        Map<String, Object> hatchData = new HashMap<>();
        hatchData.put("hatch_rate", 0f);
        hatchData.put("num_clients", 0);
        // send hatch message
        client.getFromServerQueue().offer(new Message(
            "hatch", hatchData, "test"));

        hatchData = new HashMap<>();
        hatchData.put("hatch_rate", 1f);
        hatchData.put("num_clients", 0);
        // send hatch message
        client.getFromServerQueue().offer(new Message(
            "hatch", hatchData, "test"));

        hatchData = new HashMap<>();
        hatchData.put("hatch_rate", 0f);
        hatchData.put("num_clients", 1);
        // send hatch message
        client.getFromServerQueue().offer(new Message(
            "hatch", hatchData, "test"));

        try {
            Thread.sleep(10);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            return;
        }

        assertEquals(RunnerState.Ready, runner.getState());
        runner.quit();
    }

    @Test
    public void TestOnMessage() throws Exception {
        MockRPCClient client = new MockRPCClient();

        runner.setRPCClient(client);

        runner.getReady();
        Message clientReady = client.getToServerQueue().take();
        assertEquals("client_ready", clientReady.getType());
        assertNull(clientReady.getData());
        assertEquals(runner.nodeID, clientReady.getNodeID());

        Map<String, Object> hatchData = new HashMap<>();
        hatchData.put("hatch_rate", 2f);
        hatchData.put("num_clients", 1);
        // send hatch message
        client.getFromServerQueue().offer(new Message(
            "hatch", hatchData, null));

        Message hatching = client.getToServerQueue().take();
        assertEquals("hatching", hatching.getType());
        assertNull(hatching.getData());
        assertEquals(runner.nodeID, hatching.getNodeID());

        // wait for hatch complete
        Thread.sleep(100);

        Message hatchingComplete = client.getToServerQueue().take();
        assertEquals("hatch_complete", hatchingComplete.getType());
        assertEquals(1, hatchingComplete.getData().get("count"));
        assertEquals(runner.nodeID, hatchingComplete.getNodeID());

        // send stop message
        client.getFromServerQueue().offer(new Message(
            "stop", null, null));
        Message clientStopped = client.getToServerQueue().take();
        assertEquals("client_stopped", clientStopped.getType());
        assertNull(clientStopped.getData());
        assertEquals(runner.nodeID, clientStopped.getNodeID());

        Message clientReadyAgain = client.getToServerQueue().take();
        assertEquals("client_ready", clientReadyAgain.getType());
        assertNull(clientReadyAgain.getData());
        assertEquals(runner.nodeID, clientReadyAgain.getNodeID());

        // send hatch message again
        client.getFromServerQueue().offer(new Message(
            "hatch", hatchData, null));

        Message hatchingAgain = client.getToServerQueue().take();
        assertEquals("hatching", hatchingAgain.getType());
        assertNull(hatchingAgain.getData());
        assertEquals(runner.nodeID, hatchingAgain.getNodeID());

        runner.quit();
    }

    @Test
    public void TestGetReadyAndQuit() throws Exception {
        MockRPCClient client = new MockRPCClient();

        runner.setRPCClient(client);
        runner.getReady();

        Message clientReady = client.getToServerQueue().take();
        assertEquals("client_ready", clientReady.getType());
        assertNull(clientReady.getData());
        assertEquals(runner.nodeID, clientReady.getNodeID());

        runner.quit();

        Message quit = client.getToServerQueue().take();
        assertEquals("quit", quit.getType());
        assertNull(quit.getData());
        assertEquals(runner.nodeID, quit.getNodeID());
    }
}
