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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * MapBenchmarkState
 * </p>
 *
 * @author 伍磊
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 1, jvmArgs = {"-Xmx100M"})
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 3, time = 1)
public class MapBenchmark extends AbstractBenchmark {

    @State(Scope.Thread)
    public static class MapBenchmarkState {

        @Param({"100", "1000", "10000"})
        public int size;

        @Param({"HashMap", "LinkedHashMap", "TreeMap"})
        public String mapType;

        public Map<String, String> map;

        @Setup(Level.Iteration)
        public void set() {
            switch (mapType) {
                case "HashMap":
                    map = new HashMap<>();
                    break;
                case "LinkedHashMap":
                    map = new LinkedHashMap<>();
                    break;
                case "TreeMap":
                    map = new TreeMap<>();
                    break;
            }
            // 填充 Map
            for (int i = 0; i < size; i++) {
                map.put("key" + i, "value" + i);
            }
        }

    }

    @Benchmark
    public void testMapGet(Blackhole blackhole, MapBenchmarkState state) {
        blackhole.consume(state.map.get("key" + (state.size / 2)));
    }

    @Benchmark
    public void testMapContainsKey(Blackhole blackhole, MapBenchmarkState state) {
        blackhole.consume(state.map.containsKey("key" + (state.size / 2)));
    }

    public static void main(String[] args) throws RunnerException {
        runBenchmark(MapBenchmark.class);
    }


}
