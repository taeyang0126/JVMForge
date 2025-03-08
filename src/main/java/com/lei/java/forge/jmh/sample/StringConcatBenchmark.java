package com.lei.java.forge.jmh.sample;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 比较字符串连接的两种不同方式
 * </p>
 *
 * @author 伍磊
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class StringConcatBenchmark {

    @Param({"10", "100", "1000"})
    private int length;

    @Benchmark
    public String testStringConcatenation() {
        String result = "";
        for (int i = 0; i < length; i++) {
            result += i;
        }
        return result;
    }

    @Benchmark
    public String testStringBuilder() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(i);
        }
        return sb.toString();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(StringConcatBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();


        /*

        Benchmark                                      (length)  Mode  Cnt   Score    Error  Units
        StringConcatBenchmark.testStringBuilder              10  avgt    5   0.040 ±  0.001  us/op
        StringConcatBenchmark.testStringBuilder             100  avgt    5   0.225 ±  0.005  us/op
        StringConcatBenchmark.testStringBuilder            1000  avgt    5   3.433 ±  0.104  us/op
        StringConcatBenchmark.testStringConcatenation        10  avgt    5   0.085 ±  0.002  us/op
        StringConcatBenchmark.testStringConcatenation       100  avgt    5   1.128 ±  0.085  us/op
        StringConcatBenchmark.testStringConcatenation      1000  avgt    5  44.292 ±  6.484  us/op

        * */
    }
}
