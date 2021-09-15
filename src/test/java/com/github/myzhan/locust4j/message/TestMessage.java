package com.github.myzhan.locust4j.message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author myzhan
 */
public class TestMessage {

    @Test
    public void TestEncodeAndDecodeNullData() throws Exception {
        Message message = new Message("test", null, "-1", "nodeId");
        Message message2 = new Message(message.getBytes());

        assertEquals("test", message2.getType());
        assertNull(message2.getData());
        assertEquals("nodeId", message2.getNodeID());
    }

    @Test
    public void TestEncodeAndDecodeWithData() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("string", "world");
        data.put("int", 1);
        data.put("float", 0.5f);
        data.put("boolean", true);
        data.put("null", null);
        data.put("array", new ArrayList<String>(Arrays.asList("foo", "bar")));

        Message message = new Message("test", data, null, "nodeId");
        Message message2 = new Message(message.getBytes());

        assertEquals("test", message2.getType());
        assertEquals("world", message2.getData().get("string"));
        assertEquals(1, message2.getData().get("int"));
        assertEquals(0.5f, message2.getData().get("float"));
        assertEquals(true, message2.getData().get("boolean"));
        assertNull(message2.getData().get("null"));
        assertEquals(new ArrayList<String>(Arrays.asList("foo", "bar")),message2.getData().get("array"));
        assertEquals("nodeId", message2.getNodeID());
    }
}
