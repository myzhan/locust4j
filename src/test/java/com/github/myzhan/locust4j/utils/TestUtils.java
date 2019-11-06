package com.github.myzhan.locust4j.utils;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestUtils {

    @Test
    public void TestMD5() {
        assertEquals(Utils.md5("hello", "world"), "fc5e038d38a57032085441e7fe7010b0");
    }

    @Test
    @Ignore
    public void TestGetHostname() throws IOException {
        Process proc = Runtime.getRuntime().exec("hostname");
        java.io.InputStream is = proc.getInputStream();
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter(System.lineSeparator());
        String hostname = "";
        if (s.hasNext()) {
            hostname = s.next();
        }
        assertEquals(hostname, Utils.getHostname());
    }

    @Test
    public void TestGetNodeID() {
        String hostname = Utils.getHostname();
        assertTrue(Utils.getNodeID().matches(hostname + "_[a-f0-9]{32}$"));
    }

    @Test
    public void TestRound() {
        assertEquals(150, Utils.round(147, -1));
        assertEquals(Utils.round(3432, -2), 3400);
        assertEquals(Utils.round(58760, -3), Utils.round(58960, -3));
        assertEquals(Utils.round(58360, -3), Utils.round(58460, -3));
    }
}
