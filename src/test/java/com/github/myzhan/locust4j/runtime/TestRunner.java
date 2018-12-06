package com.github.myzhan.locust4j.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.myzhan.locust4j.AbstractTask;
import com.github.myzhan.locust4j.message.Message;
import com.github.myzhan.locust4j.rpc.Client;
import com.github.myzhan.locust4j.stats.Stats;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author myzhan
 * @date 2018/12/06
 */
public class TestRunner {

    private class TestTask extends AbstractTask {
        public int weight = 1;
        public String name = "test";

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
        List<AbstractTask> tasks = new ArrayList<AbstractTask>(2);
        tasks.add(new TestTask());

        Runner runner = Runner.getInstance();
        runner.setStats(new Stats());
        runner.setTasks(tasks);

        runner.startHatching(10, 10);

        Assert.assertEquals(10, runner.numClients);

        runner.stop();
    }

    @Test
    public void TestOnMessage() throws Exception {
        List<AbstractTask> tasks = new ArrayList<AbstractTask>(2);
        tasks.add(new TestTask());

        Client client = new MockRPCClient();

        Runner runner = Runner.getInstance();
        runner.setStats(new Stats());
        runner.setTasks(tasks);
        runner.setRPCClient(client);

        runner.getReady();
        Message clientReady = ((MockRPCClient)client).getToServerQueue().take();
        Assert.assertEquals("client_ready", clientReady.getType());
        Assert.assertEquals(null, clientReady.getData());
        Assert.assertEquals(runner.nodeID, clientReady.getNodeID());

        Map<String, Object> hatchData = new HashMap<String, Object>();
        hatchData.put("hatch_rate", 2f);
        hatchData.put("num_clients", 1);
        // send hatch message
        ((MockRPCClient)client).getFromServerQueue().offer(new Message(
            "hatch", hatchData, null));

        Message hatching = ((MockRPCClient)client).getToServerQueue().take();
        Assert.assertEquals("hatching", hatching.getType());
        Assert.assertEquals(null, hatching.getData());
        Assert.assertEquals(runner.nodeID, hatching.getNodeID());

        // wait for hatch complete
        Thread.sleep(100);

        Message hatchingComplete = ((MockRPCClient)client).getToServerQueue().take();
        Assert.assertEquals("hatch_complete", hatchingComplete.getType());
        Assert.assertEquals(1, hatchingComplete.getData().get("count"));
        Assert.assertEquals(runner.nodeID, hatchingComplete.getNodeID());

        // send stop message
        ((MockRPCClient)client).getFromServerQueue().offer(new Message(
            "stop", null, null));
        Message clientStopped = ((MockRPCClient)client).getToServerQueue().take();
        Assert.assertEquals("client_stopped", clientStopped.getType());
        Assert.assertEquals(null, clientStopped.getData());
        Assert.assertEquals(runner.nodeID, clientStopped.getNodeID());

        Message clientReadyAgain = ((MockRPCClient)client).getToServerQueue().take();
        Assert.assertEquals("client_ready", clientReadyAgain.getType());
        Assert.assertEquals(null, clientReadyAgain.getData());
        Assert.assertEquals(runner.nodeID, clientReadyAgain.getNodeID());

        // send hatch message again
        ((MockRPCClient)client).getFromServerQueue().offer(new Message(
            "hatch", hatchData, null));

        Message hatchingAgain = ((MockRPCClient)client).getToServerQueue().take();
        Assert.assertEquals("hatching", hatchingAgain.getType());
        Assert.assertEquals(null, hatchingAgain.getData());
        Assert.assertEquals(runner.nodeID, hatchingAgain.getNodeID());

        runner.quit();
    }

    @Test
    public void TestGetReadyAndQuit() throws Exception {
        List<AbstractTask> tasks = new ArrayList<AbstractTask>(2);
        tasks.add(new TestTask());

        Client client = new MockRPCClient();

        Runner runner = Runner.getInstance();
        runner.setStats(new Stats());
        runner.setTasks(tasks);
        runner.setRPCClient(client);
        runner.getReady();

        Message clientReady = ((MockRPCClient)client).getToServerQueue().take();
        Assert.assertEquals("client_ready", clientReady.getType());
        Assert.assertEquals(null, clientReady.getData());
        Assert.assertEquals(runner.nodeID, clientReady.getNodeID());

        runner.quit();

        Message quit = ((MockRPCClient)client).getToServerQueue().take();
        Assert.assertEquals("quit", quit.getType());
        Assert.assertEquals(null, quit.getData());
        Assert.assertEquals(runner.nodeID, quit.getNodeID());
    }
}
