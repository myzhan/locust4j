package com.github.myzhan.locust4j.message;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author myzhan
 */
public class TestLongIntMap {

    @Test
    public void TestAddAndGet() {
        LongIntMap map = new LongIntMap();
        map.add(1000L);
        map.add(1000L);

        assertEquals(2, (int)map.get(1000L));
    }

    @Test
    public void TestToString() {
        LongIntMap map = new LongIntMap();
        map.add(1000L);
        map.add(1000L);

        assertEquals("{1000=2}", map.toString());
    }
}
