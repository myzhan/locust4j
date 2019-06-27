package com.github.myzhan.locust4j.message;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author myzhan
 * @date 2018/12/07
 */
public class TestLongIntMap {

    @Test
    public void TestAddAndGet() {
        LongIntMap map = new LongIntMap();
        map.add(1000l);
        map.add(1000l);

        Assert.assertEquals(2, (int)map.get(1000l));
    }

    @Test
    public void TestToString() {
        LongIntMap map = new LongIntMap();
        map.add(1000l);
        map.add(1000l);

        Assert.assertEquals("{1000=2}", map.toString());
    }
}
