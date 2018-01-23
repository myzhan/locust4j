package com.github.myzhan.locust4j.examples;

import com.github.myzhan.locust4j.Locust;

public class Main {

    public static void main(String[] args) {

        // setup locust
        Locust locust = Locust.getInstance();
        locust.setMasterHost("127.0.0.1");
        locust.setMasterPort(5557);

        // print out locust4j's internal logs.
        locust.setVerbose(true);

        // run tasks without connecting to master, for debug purpose.
        locust.dryRun(new TaskAlwaysSuccess(), new TaskAlwaysFail());

        // limit max RPS that Locust4j can generate
        locust.setMaxRPS(1000);

        // user specified task
        locust.run(new TaskAlwaysSuccess());

        // multiply tasks
        // locust.run(new TaskAlwaysSuccess(), new TaskAlwaysFail());
    }
}
