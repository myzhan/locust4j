package com.github.myzhan.locust4j.message;

import java.io.IOException;
import java.util.Arrays;

import com.github.myzhan.locust4j.Log;
import org.junit.Assert;
import org.junit.Test;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;

/**
 * @author myzhan
 * @date 2018/12/07
 */
public class TestVisitor {

    @Test(expected = IOException.class)
    public void TestVisitUnknownType() throws IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        Visitor visitor = new Visitor(packer);
        visitor.visit(new Log());
        System.out.println(Arrays.toString(packer.toByteArray()));
    }

    @Test
    public void TestVisitNull() throws IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        Visitor visitor = new Visitor(packer);
        visitor.visit(null);

        byte[] result = packer.toByteArray();

        Assert.assertEquals(1, result.length);
        Assert.assertEquals(-64, result[0]);
    }

    @Test
    public void TestVisitString() throws IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        Visitor visitor = new Visitor(packer);
        visitor.visit("HelloWorld");

        byte[] result = packer.toByteArray();

        System.out.println(Arrays.toString(result));

        Assert.assertEquals(11, result.length);
        Assert.assertEquals(-86, result[0]);
        Assert.assertEquals("HelloWorld", new String(Arrays.copyOfRange(result, 1, 11)));
    }

    @Test
    public void TestVisitLongIntMap() throws IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        Visitor visitor = new Visitor(packer);

        LongIntMap data = new LongIntMap();
        data.add(1000l);
        data.add(1000l);

        visitor.visit(data);

        byte[] result = packer.toByteArray();

        Assert.assertEquals(5, result.length);
        Assert.assertEquals(2, result[4]);
    }
}
