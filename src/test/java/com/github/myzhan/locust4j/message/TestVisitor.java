package com.github.myzhan.locust4j.message;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.github.myzhan.locust4j.Log;
import org.junit.Before;
import org.junit.Test;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;

import static org.junit.Assert.assertEquals;

/**
 * @author myzhan
 */
public class TestVisitor {

    private MessageBufferPacker packer;
    private Visitor visitor;

    @Before
    public void before() {
        packer = MessagePack.newDefaultBufferPacker();
        visitor = new Visitor(packer);
    }

    @Test
    public void TestVisitNull() throws IOException {
        visitor.visit(null);

        byte[] result = packer.toByteArray();

        assertEquals(1, result.length);
        assertEquals(-64, result[0]);
    }

    @Test
    public void TestVisitString() throws IOException {
        visitor.visit("HelloWorld");

        byte[] result = packer.toByteArray();

        assertEquals(11, result.length);
        assertEquals(-86, result[0]);
        assertEquals("HelloWorld", new String(Arrays.copyOfRange(result, 1, 11)));
    }

    @Test
    public void TestVisitLong() throws IOException {
        visitor.visit(Long.MAX_VALUE);

        byte[] result = packer.toByteArray();

        assertEquals(9, result.length);
        assertEquals(-49, result[0]);
    }

    @Test
    public void TestVisitDouble() throws IOException {
        visitor.visit(Double.MAX_VALUE);

        byte[] result = packer.toByteArray();

        assertEquals(9, result.length);
        assertEquals(-53, result[0]);
    }

    @Test
    public void TestVisitMap() throws IOException {
        Map<String, Object> m = new HashMap<>();
        m.put("foo", "bar");
        visitor.visit(m);

        byte[] result = packer.toByteArray();

        assertEquals(9, result.length);
        assertEquals(-127, result[0]);
    }

    @Test
    public void TestVisitList() throws IOException {
        visitor.visit(Arrays.asList("foo", "bar"));

        byte[] result = packer.toByteArray();

        assertEquals(9, result.length);
        assertEquals(-110, result[0]);
    }

    @Test
    public void TestVisitLongIntMap() throws IOException {
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
        visitor.visit(new Log());
    }
}
