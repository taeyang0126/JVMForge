/*
package com.lei.java.forge.fileio;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 0)
@Measurement(iterations = 1)
public class ReadBenchmark {

*/
/*    @Param({"64", "128", "512", "1024", "2048", "4096", "8192", "32768", "65536",
            "1048576", "33554432", "67108864", "536870912"})  // 从64B到512MB
    private int bufferSize;*//*


    @Param({"64", "128", "512", "1024", "2048", "4096", "8192"})
    private int bufferSize;

    private File file;
    private FileChannel fileChannel;
    private MappedByteBuffer mappedByteBuffer;
    private ByteBuffer directBuffer;
    private MemorySegment memorySegment;
    private static final long FILE_SIZE = 1024 * 1024 * 1024; // 1GB
    private static Linker linker = Linker.nativeLinker();

    @Setup
    public void setup() throws Exception {
        // 初始化文件和缓冲区
        file = new File("ReadWithPageCache");
        fileChannel = new RandomAccessFile(file, "rw").getChannel();
        mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, FILE_SIZE);
        memorySegment = MemorySegment.ofBuffer(mappedByteBuffer);
        directBuffer = ByteBuffer.allocateDirect(512 * 1024 * 1024);

        // 锁定内存
        try {
            int result = (int) find_mlock().invokeExact(memorySegment, FILE_SIZE);
            if (result != 0) {
                throw new RuntimeException("Failed to lock memory: " + result);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

    }

    @TearDown
    public void tearDown() throws Exception {
        // 清理资源
        try {
            int munlockR = (int) find_munlock().invokeExact(memorySegment, FILE_SIZE);
            if (munlockR != 0) {
                System.err.println("Failed to unlock memory: " + munlockR);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            ((sun.nio.ch.DirectBuffer) mappedByteBuffer).cleaner().clean();
            fileChannel.close();
            file.delete();
        }
    }

    @Benchmark
    public void mmapRead() {
        byte[] bytes = new byte[bufferSize];
        mappedByteBuffer.rewind();
        while (mappedByteBuffer.hasRemaining()) {
            mappedByteBuffer.get(bytes);
        }
    }

    @Benchmark
    public void fileChannelRead() throws IOException {
        fileChannel.position(0);
        for (int i = 0; i < FILE_SIZE; i += bufferSize) {
            directBuffer.position(0);
            directBuffer.limit(bufferSize);
            fileChannel.read(directBuffer);
        }
    }

    private static MethodHandle find_mlock() {
        SymbolLookup symbolLookup = linker.defaultLookup();
        return findMethod(symbolLookup,
                "mlock",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
    }

    private static MethodHandle find_munlock() {
        SymbolLookup symbolLookup = linker.defaultLookup();
        return findMethod(symbolLookup,
                "munlock",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
    }

    private static MethodHandle findMethod(SymbolLookup symbolLookup,
                                           String methodName,
                                           FunctionDescriptor function,
                                           Linker.Option... options) {
        MemorySegment address = symbolLookup.find(methodName)
                .orElseThrow(() -> new RuntimeException("未找到此方法"));
        return linker.downcallHandle(address, function, options);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ReadBenchmark.class.getSimpleName())
                .resultFormat(ResultFormatType.JSON)
                .result("benchmark-result.json")
                .build();
        new Runner(opt).run();
    }
}
*/
