package com.lei.java.forge.jmh.sample;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * FirstBenchmark
 * </p>
 *
 * @author 伍磊
 */
@BenchmarkMode(Mode.AverageTime)  // 测量平均执行时间
@OutputTimeUnit(TimeUnit.MICROSECONDS)  // 以微秒为单位输出
@State(Scope.Thread)  // 每个测试线程一个实例
public class FirstBenchmark {

    @Benchmark
    public void testMethod() {
        // 这是我们要测量的方法
        // 示例: 计算1到1000的和
        int sum = 0;
        for (int i = 1; i <= 1000; i++) {
            sum += i;
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(FirstBenchmark.class.getSimpleName())
                .forks(1)  // 使用1个独立进程
                .warmupIterations(3)  // 预热3次
                .measurementIterations(5)  // 测量5次
                .build();
        new Runner(opt).run();
    }

}
