package com.github.myzhan.locust4j.message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.myzhan.locust4j.Log;
import org.junit.Test;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;

import static org.junit.Assert.assertEquals;

/**
 * @author myzhan
 */
public class TestVisitor {

    @Test
    public void TestVisitNull() throws IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        Visitor visitor = new Visitor(packer);
        visitor.visit(null);

        byte[] result = packer.toByteArray();

        assertEquals(1, result.length);
        assertEquals(-64, result[0]);
    }

    @Test
    public void TestVisitString() throws IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        Visitor visitor = new Visitor(packer);
        visitor.visit("HelloWorld");

        byte[] result = packer.toByteArray();

        assertEquals(11, result.length);
        assertEquals(-86, result[0]);
        assertEquals("HelloWorld", new String(Arrays.copyOfRange(result, 1, 11)));
    }

    @Test
    public void TestVisitLong() throws IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        Visitor visitor = new Visitor(packer);
        visitor.visit(Long.MAX_VALUE);

        byte[] result = packer.toByteArray();

        assertEquals(9, result.length);
        assertEquals(-49, result[0]);
    }

    @Test
    public void TestVisitDouble() throws IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        Visitor visitor = new Visitor(packer);
        visitor.visit(Double.MAX_VALUE);

        byte[] result = packer.toByteArray();

        assertEquals(9, result.length);
        assertEquals(-53, result[0]);
    }

    @Test
    public void TestVisitMap() throws IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        Visitor visitor = new Visitor(packer);

        Map<String, Object> m = new HashMap<>();
        m.put("foo", "bar");
        visitor.visit(m);

        byte[] result = packer.toByteArray();

        assertEquals(9, result.length);
        assertEquals(-127, result[0]);
    }

    @Test
    public void TestVisitList() throws IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        Visitor visitor = new Visitor(packer);

        List<Object> l = new ArrayList<>();
        l.add("foo");
        l.add("bar");
        visitor.visit(l);

        byte[] result = packer.toByteArray();

        assertEquals(9, result.length);
        assertEquals(-110, result[0]);
    }

    @Test
    public void TestVisitLongIntMap() throws IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        Visitor visitor = new Visitor(packer);

        LongIntMap data = new LongIntMap();
        data.add(1000L);
        data.add(1000L);

        visitor.visit(data);

        byte[] result = packer.toByteArray();

        assertEquals(5, result.length);
        assertEquals(2, result[4]);
    }

    @Test(expected = IOException.class)
    public void TestVisitUnknownType() throws IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        Visitor visitor = new Visitor(packer);
        visitor.visit(new Log());
    }
}
