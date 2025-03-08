package com.lei.java.forge.jmh.sample;

import com.lei.java.forge.jmh.AbstractBenchmark;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * ListBenchmark
 * 验证 ArrayList、LinkedHashList
 * add/del/get
 * </p>
 *
 * @author 伍磊
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1, time = 3)
@Measurement(iterations = 3, time = 5)
@Fork(value = 1)
@State(Scope.Thread)
public class ListBenchmark extends AbstractBenchmark {

    @Param({"100", "1000", "10000"})
    private int size;

    @Param({"ArrayList", "LinkedList"})
    private String type;

    private List<String> list;

    private List<String> delList;

    @Setup(Level.Iteration)
    public void set() {
        switch (type) {
            case "ArrayList":
                list = new ArrayList<>();
                delList = new LinkedList<>();
                break;
            case "LinkedList":
                list = new LinkedList<>();
                delList = new LinkedList<>();
                break;
        }
        for (int i = 0; i < size; i++) {
            delList.add("key" + i);
        }
    }

    @Benchmark
    public void add(Blackhole blackhole) {
        for (int i = 0; i < size; i++) {
            list.add("key" + i);
        }
        blackhole.consume(list);
    }

    @Benchmark
    public void delWithIterator(Blackhole blackhole) {
        Iterator<String> iterator = delList.iterator();
        int i = 0;
        int totalSize = delList.size();
        while (iterator.hasNext() && i++ < (totalSize / 2)) {
            iterator.next();
            iterator.remove();
        }
        blackhole.consume(delList);
    }

    @Benchmark
    public void del(Blackhole blackhole) {
        int totalSize = delList.size();
        for (int i = 0; i < totalSize / 2; i++) {
            delList.removeFirst();
        }
        blackhole.consume(delList);
    }

    @Benchmark
    public void get(Blackhole blackhole) {
        for (int i = 0; i < size / 2; i++) {
            blackhole.consume(delList.get(i));
        }
    }

    public static void main(String[] args) throws RunnerException {
        runBenchmark(ListBenchmark.class);
    }

}
