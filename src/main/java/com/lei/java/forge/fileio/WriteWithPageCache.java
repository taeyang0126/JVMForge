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
 * WriteWithPageCache
 * </p>
 *
 * @author 伍磊
 */
@SuppressWarnings("all")
public class WriteWithPageCache {

    static Linker linker = Linker.nativeLinker();

    @Test
    public void testWriteWithPageCache() throws Exception {

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
            64B        115             13629           118.51
            128B       204             6068            29.75
            512B       31              1565            50.48
            1K         27              826             30.59
            2K         25              444             17.76
            4K         29              255             8.79
            8K         53              162             3.06
            32K        29              96              3.31
            64K        23              91              3.96
            1M         23              87              3.78
            32M        41              110             2.68
            64M        37              107             2.89
            512M       74              108             1.46
         */

        File file = new File("WriteWithPageCache");
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
            long start = System.currentTimeMillis();
            while (mappedByteBuffer.hasRemaining()) {
                directBuffer.position(0);
                directBuffer.limit(dataSet.size);
                mappedByteBuffer.put(directBuffer);
            }
            long mmapTime = System.currentTimeMillis() - start;

            start = System.currentTimeMillis();
            for (int i = 0; i < fileSize; i += dataSet.size) {
                directBuffer.position(0);
                directBuffer.limit(dataSet.size);
                fileChannel.write(directBuffer);
            }
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
