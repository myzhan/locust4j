package com.github.myzhan.locust4j.rpc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import com.github.myzhan.locust4j.message.Message;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author myzhan
 * @date 2018/12/05
 */
public class TestZeromqClient {

    @Test
    @Ignore
    public void TestPingPong() throws Exception {
        // randomized the port to avoid conflicts
        int masterPort = ThreadLocalRandom.current().nextInt(1000) + 1024;

        TestServer server = new TestServer("0.0.0.0", masterPort + 1, masterPort);
        server.start();

        Client client = new ZeromqClient("0.0.0.0", masterPort);
        Map<String, Object> data = new HashMap<>();
        data.put("hello", "world");

        client.send(new Message("test", data, "node"));
        Message message = client.recv();

        Assert.assertEquals("test", message.getType());
        Assert.assertEquals("node", message.getNodeID());
        Assert.assertEquals(data, message.getData());

        Thread.sleep(100);
        server.stop();
        client.close();
    }
}
