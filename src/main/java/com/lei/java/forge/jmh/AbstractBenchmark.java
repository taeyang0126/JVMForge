package com.lei.java.forge.jmh;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * <p>
 * AbstractBenchmark
 * </p>
 *
 * @author 伍磊
 */
public abstract class AbstractBenchmark {

    public static void runBenchmark(Class<?> benchmarkClass) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(benchmarkClass.getSimpleName())
                .build();
        new Runner(opt).run();
    }

}
