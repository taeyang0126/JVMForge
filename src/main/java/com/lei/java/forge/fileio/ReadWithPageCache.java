package com.lei.java.forge.fileio;

import org.junit.Test;

import java.io.File;
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
import java.util.List;

/**
 * <p>
 * ReadWithPageCache
 * </p>
 *
 * @author 伍磊
 */
@SuppressWarnings("all")
public class ReadWithPageCache {

    static Linker linker = Linker.nativeLinker();

    @Test
    public void testReadWithPageCache() throws Exception {

        /*
            VM options
            --add-opens java.base/sun.nio.ch=ALL-UNNAMED
            --add-exports java.base/sun.nio.ch=ALL-UNNAMED
            --add-opens java.base/jdk.internal.ref=ALL-UNNAMED
            --add-exports java.base/jdk.internal.ref=ALL-UNNAMED
            --enable-native-access=ALL-UNNAMED

            结果
            Size       Mmap(ms)        FileChannel(ms) 倍数
            --------------------------------------------
            64B        130             8738            67.22
            128B       115             4404            38.30
            512B       29              1148            39.59
            1K         21              594             28.29
            2K         20              322             16.10
            4K         37              198             5.35
            8K         25              139             5.56
            32K        27              85              3.15
            64K        28              77              2.75
            1M         33              71              2.15
            32M        30              152             5.07
            64M        33              154             4.67
            512M       39              156             4.00

         */

        File file = new File("ReadWithPageCache");
        FileChannel fileChannel = new RandomAccessFile(file, "rw").getChannel();
        long fileSize = 1024 * 1024 * 1024;
        MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, fileSize);
        MemorySegment memorySegment = MemorySegment.ofBuffer(mappedByteBuffer);
        try {
            // 锁定内存
            int result = (int) find_mlock().invokeExact(memorySegment, fileSize);
            System.out.println("mlock result: " + result);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        System.out.printf("\n%-10s %-15s %-15s %-10s%n",
                "Size", "Mmap(ms)", "FileChannel(ms)", "倍数");
        System.out.println("--------------------------------------------");

        List<DataSet> testDataSet = DataSet.loadTestDataSet();
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(512 * 1024 * 1024);
        for (DataSet dataSet : testDataSet) {
            // System.out.println("  *************** " + dataSet.name + " *********公众号：bin的技术小屋");
            byte[] bytes = new byte[dataSet.size];
            long start = System.currentTimeMillis();
            while (mappedByteBuffer.hasRemaining()) {
                mappedByteBuffer.get(bytes);
            }
            // System.out.println("-- mmap 耗时：" + (System.currentTimeMillis() - start) + " ms");
            long mmapTime = System.currentTimeMillis() - start;

            start = System.currentTimeMillis();
            for (int i = 0; i < fileSize; i += dataSet.size) {
                directBuffer.position(0);
                directBuffer.limit(dataSet.size);
                fileChannel.read(directBuffer);
            }
            // System.out.println("-- filechannel 耗时：" + (System.currentTimeMillis() - start) + " ms");
            long fcTime = System.currentTimeMillis() - start;

            mappedByteBuffer.rewind();
            fileChannel.position(0);

            double ratio = (double) fcTime / mmapTime;
            System.out.printf("%-10s %-15d %-15d %.2f%n",
                    dataSet.name,
                    mmapTime,
                    fcTime,
                    ratio);
        }

        try {
            int munlockR = (int) find_munlock().invokeExact(memorySegment, fileSize);
            System.out.println("\nmunlock result: " + munlockR);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        ((sun.nio.ch.DirectBuffer) mappedByteBuffer).cleaner().clean();
        fileChannel.close();
        file.delete();
    }

    private static MethodHandle find_mlock() {
        SymbolLookup symbolLookup = linker.defaultLookup();
        // https://dashdash.io/2/mlock
        // mlock 锁定内存，禁止 swap
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
        MemorySegment address = symbolLookup.find(methodName).orElseThrow(() -> new RuntimeException("未找到此方法"));
        return linker.downcallHandle(
                address,
                function,
                options
        );
    }

}
