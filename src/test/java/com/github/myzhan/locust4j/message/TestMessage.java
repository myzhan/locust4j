package com.github.myzhan.locust4j.message;

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
}
