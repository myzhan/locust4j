package com.github.myzhan.locust4j;

public class Log {

    // TODO: replace this with log4j

    protected static void debug(Object log) {
        System.out.println(log);
    }

    protected static void debug(Exception ex) {
        ex.printStackTrace();
    }

    protected static void error(Object log) {
        System.out.println(log);
    }

    protected static void error(Exception ex) {
        ex.printStackTrace();
    }
}
