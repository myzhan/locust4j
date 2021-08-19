package com.github.myzhan.locust4j;

import com.github.myzhan.locust4j.runtime.Runner;

public class LocustTestHelper  {

    public static void setLocustRunner(Runner runner) {
        Locust.getInstance().setRunner(runner);
    }
}
