package com.github.myzhan.locust4j;

import org.junit.Assert;
import org.junit.Test;

public class TestUtils {

    @Test
    public void TestRound() {
        Assert.assertEquals(150, Utils.round(147, -1));
        Assert.assertEquals(Utils.round(3432, -2), 3400);
        Assert.assertEquals(Utils.round(58760, -3), Utils.round(58960, -3));
        Assert.assertEquals(Utils.round(58360, -3), Utils.round(58460, -3));
    }
}
