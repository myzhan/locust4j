package com.github.myzhan.locust4j.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.github.myzhan.locust4j.AbstractTask;
import com.github.myzhan.locust4j.message.Message;
import com.github.myzhan.locust4j.stats.Stats;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author myzhan
 */
public class TestRunner {

    private static final Logger logger = LoggerFactory.getLogger(TestRunner.class);

    private Runner runner;

    @Before
    public void before() {
        runner = new Runner();
        runner.setStats(new Stats());
        runner.setTasks(Collections.singletonList((AbstractTask) new TestTask()));
    }

    private static class TestTask extends AbstractTask {

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
                logger.error(ex.getMessage());
            }
        }
    }

    @Test
    public void TestStartSpawning() {
        runner.startSpawning(10, 10);
        assertEquals(10, runner.numClients);
        runner.stop();
    }

    @Test
    public void TestOnInvalidSpawnMessage() {
        MockRPCClient client = new MockRPCClient();

        runner.setRPCClient(client);

        runner.getReady();

        Map<String, Object> spawnData = new HashMap<>();
        spawnData.put("spawn_rate", 0f);
        spawnData.put("num_users", 0);
        // send spawn message
        client.getFromServerQueue().offer(new Message(
            "spawn", spawnData, "test"));

        spawnData = new HashMap<>();
        spawnData.put("spawn_rate", 1f);
        spawnData.put("num_users", 0);
        client.getFromServerQueue().offer(new Message(
            "spawn", spawnData, "test"));

        spawnData = new HashMap<>();
        spawnData.put("spawn_rate", 0f);
        spawnData.put("num_users", 1);
        client.getFromServerQueue().offer(new Message(
            "spawn", spawnData, "test"));

        try {
            Thread.sleep(10);
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage());
            return;
        }

        assertEquals(RunnerState.Ready, runner.getState());
        runner.quit();
    }

    @Test
    public void TestOnMessage() throws Exception {
        MockRPCClient client = new MockRPCClient();

        runner.setRPCClient(client);
        runner.setHeartbeatStopped(true);

        runner.getReady();
        Message clientReady = client.getToServerQueue().take();
        assertEquals("client_ready", clientReady.getType());
        assertNull(clientReady.getData());
        assertEquals(runner.nodeID, clientReady.getNodeID());

        Map<String, Object> spawnData = new HashMap<>();
        spawnData.put("spawn_rate", 2f);
        spawnData.put("num_users", 1);
        spawnData.put("host", "www.github.com");
        // send spawn message
        client.getFromServerQueue().offer(new Message(
            "spawn", spawnData, null));

        Message spawning = client.getToServerQueue().take();
        assertEquals("spawning", spawning.getType());
        assertNull(spawning.getData());
        assertEquals(runner.nodeID, spawning.getNodeID());

        // wait for spawn complete
        Thread.sleep(100);

        // check remote params
        Assert.assertEquals("2.0", runner.getRemoteParams().get("spawn_rate"));
        Assert.assertEquals("1", runner.getRemoteParams().get("num_users"));
        Assert.assertEquals("www.github.com", runner.getRemoteParams().get("host"));

        Message spawnComplete = client.getToServerQueue().take();
        assertEquals("spawning_complete", spawnComplete.getType());
        assertEquals(1, spawnComplete.getData().get("count"));
        assertEquals(runner.nodeID, spawnComplete.getNodeID());

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

        // send spawn message again
        client.getFromServerQueue().offer(new Message(
            "spawn", spawnData, null));

        Message spawningAgain = client.getToServerQueue().take();
        assertEquals("spawning", spawningAgain.getType());
        assertNull(spawningAgain.getData());
        assertEquals(runner.nodeID, spawningAgain.getNodeID());

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

    @Test
    public void TestSendHeartbeat() throws Exception {
        MockRPCClient client = new MockRPCClient();

        runner.setRPCClient(client);
        runner.getReady();

        Message clientReady = client.getToServerQueue().take();
        assertEquals("client_ready", clientReady.getType());

        Message heartbeat = client.getToServerQueue().take();
        assertEquals("heartbeat", heartbeat.getType());
        assertNotNull(heartbeat.getData().get("current_cpu_usage"));
        assertNotNull(heartbeat.getData().get("state"));

        runner.quit();
    }
}
