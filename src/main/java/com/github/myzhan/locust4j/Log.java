package com.github.myzhan.locust4j;

public class Log {

    // TODO: replace this with log4j

    protected static void debug(Object log) {
        if (Locust.getInstance().isVerbose()) {
            System.out.println(log);
        }
    }

    protected static void debug(Exception ex) {
        if (Locust.getInstance().isVerbose()) {
            ex.printStackTrace();
        }
    }

    protected static void error(Object log) {
        if (Locust.getInstance().isVerbose()) {
            System.out.println(log);
        }
    }

    protected static void error(Exception ex) {
        if (Locust.getInstance().isVerbose()) {
            ex.printStackTrace();
        }
    }
}
