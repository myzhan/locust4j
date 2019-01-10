package com.github.myzhan.locust4j.utils;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * @author myzhan
 */
public class BenchmarkMD5 {

    @Benchmark
    public String calculateMD5() {
        return Utils.md5("hello", "world", "locust4j");
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(BenchmarkMD5.class.getSimpleName())
            .forks(1)
            .warmupIterations(1)
            .measurementIterations(2)
            .build();

        new Runner(opt).run();
    }
}
