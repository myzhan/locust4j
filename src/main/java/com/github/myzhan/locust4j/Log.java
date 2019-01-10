package com.github.myzhan.locust4j;

/**
 * @author myzhan
 */
public class Log {

    // TODO: replace this with log4j

    public static void debug(Object log) {
        if (Locust.getInstance().isVerbose()) {
            System.out.println(log);
        }
    }

    public static void debug(Exception ex) {
        if (Locust.getInstance().isVerbose()) {
            ex.printStackTrace();
        }
    }

    public static void error(Object log) {
        if (Locust.getInstance().isVerbose()) {
            System.out.println(log);
        }
    }

    public static void error(Exception ex) {
        if (Locust.getInstance().isVerbose()) {
            ex.printStackTrace();
        }
    }
}
