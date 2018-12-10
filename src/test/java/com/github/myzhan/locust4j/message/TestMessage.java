package com.github.myzhan.locust4j.message;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author myzhan
 * @date 2018/12/06
 */
public class TestMessage {

    @Test
    public void TestEncodeAndDecodeNullData() throws Exception {
        Message message = new Message("test", null, "nodeId");
        Message message2 = new Message(message.getBytes());

        Assert.assertEquals("test", message2.getType());
        Assert.assertEquals(null, message2.getData());
        Assert.assertEquals("nodeId", message2.getNodeID());
    }

    @Test
    public void TestEncodeAndDecodeWithData() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("string", "world");
        data.put("int", 1);
        data.put("float", 0.5f);
        data.put("boolean", true);
        data.put("null", null);

        Message message = new Message("test", data, "nodeId");
        Message message2 = new Message(message.getBytes());

        Assert.assertEquals("test", message2.getType());
        Assert.assertEquals("world", message2.getData().get("string"));
        Assert.assertEquals(1, message2.getData().get("int"));
        Assert.assertEquals(0.5f, message2.getData().get("float"));
        Assert.assertEquals(true, message2.getData().get("boolean"));
        Assert.assertEquals(null, message2.getData().get("null"));
        Assert.assertEquals("nodeId", message2.getNodeID());
    }
 }
