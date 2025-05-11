package com.lei.java.forge.fileio;

import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

/**
 * <p>
 * WriteWithOutPageCache
 * </p>
 *
 * @author 伍磊
 */
public class WriteWithOutPageCache {

    public static void main(String[] args) throws Exception {

         /*
            VM options
            --add-opens java.base/sun.nio.ch=ALL-UNNAMED
            --add-exports java.base/sun.nio.ch=ALL-UNNAMED
            --add-opens java.base/jdk.internal.ref=ALL-UNNAMED
            --add-exports java.base/jdk.internal.ref=ALL-UNNAMED
            --enable-native-access=ALL-UNNAMED

            Size       Mmap(ms)        FileChannel(ms) 倍数
            --------------------------------------------
            64B        253             12566           49.67
            128B       250             6867            27.47
            512B       130             2047            15.75
            1K         130             1307            10.05
            2K         130             959             7.38
            4K         135             712             5.27
            8K         143             447             3.13
            32K        135             210             1.56
            64K        131             186             1.42
            1M         134             149             1.11
            32M        134             150             1.12
            64M        137             153             1.12
            512M       145             145             1.00
         */

        long fileSize = 1024 * 1024 * 1024;
        List<DataSet> testDataSet = DataSet.loadTestDataSet();
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(512 * 1024 * 1024);
        MemorySegment memorySegment = MemorySegment.ofBuffer(directBuffer);
        memorySegment.fill((byte) 6);

        System.out.printf("\n%-10s %-15s %-15s %-10s%n",
                "Size", "Mmap(ms)", "FileChannel(ms)", "倍数");
        System.out.println("--------------------------------------------");

        for (DataSet dataSet : testDataSet) {

            String fileName = "mmapWrite" + dataSet.name;
            File mappedFile = new File(fileName);
            FileChannel mappedFileChannel = new RandomAccessFile(mappedFile, "rw").getChannel();
            MappedByteBuffer mappedByteBuffer = mappedFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, fileSize);


            long start = System.currentTimeMillis();
            while (mappedByteBuffer.hasRemaining()) {
                directBuffer.position(0);
                directBuffer.limit(dataSet.size);
                mappedByteBuffer.put(directBuffer);
            }
            long mmapTime = System.currentTimeMillis() - start;
            ((DirectBuffer) mappedByteBuffer).cleaner().clean();
            mappedFileChannel.close();
            mappedFile.delete();

            fileName = "fileChannelWrite" + dataSet.name;
            File file = new File(fileName);
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            randomAccessFile.setLength(fileSize);
            FileChannel fileChannel = randomAccessFile.getChannel();

            start = System.currentTimeMillis();
            for (int i = 0; i < fileSize; i += dataSet.size) {
                directBuffer.position(0);
                directBuffer.limit(dataSet.size);
                fileChannel.write(directBuffer);
            }
            long fcTime = System.currentTimeMillis() - start;
            fileChannel.close();
            file.delete();

            double ratio = (double) fcTime / mmapTime;
            System.out.printf("%-10s %-15d %-15d %.2f%n",
                    dataSet.name,
                    mmapTime,
                    fcTime,
                    ratio);

        }
    }

}
