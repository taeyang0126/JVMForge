package com.lei.java.forge.fileio;

import org.junit.Test;
import sun.nio.ch.DirectBuffer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

/**
 * <p>
 * ReadWithOutPageCache
 * </p>
 *
 * @author 伍磊
 */
@SuppressWarnings("all")
public class ReadWithOutPageCache {

    @Test
    public void testReadWithOutPageCache() throws IOException {
         /*
            VM options
            --add-opens java.base/sun.nio.ch=ALL-UNNAMED
            --add-exports java.base/sun.nio.ch=ALL-UNNAMED
            --add-opens java.base/jdk.internal.ref=ALL-UNNAMED
            --add-exports java.base/jdk.internal.ref=ALL-UNNAMED
            --enable-native-access=ALL-UNNAMED

            Size       Mmap(ms)        FileChannel(ms) 倍数
            --------------------------------------------
            64B        248             8781            35.41
            128B       216             4481            20.75
            512B       131             1210            9.24
            1K         133             632             4.75
            2K         128             368             2.88
            4K         128             234             1.83
            8K         131             172             1.31
            32K        133             126             0.95
            64K        136             116             0.85
            1M         124             99              0.80
            32M        143             202             1.41
            64M        152             191             1.26
            512M       169             209             1.24
         */

        long fileSize = 1024 * 1024 * 1024;
        List<DataSet> testDataSet = DataSet.loadTestDataSet();
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(512 * 1024 * 1024);

        System.out.printf("\n%-10s %-15s %-15s %-10s%n",
                "Size", "Mmap(ms)", "FileChannel(ms)", "倍数");
        System.out.println("--------------------------------------------");

        for (DataSet dataSet : testDataSet) {

            String fileName = "mmap" + dataSet.name;
            File mappedFile = new File(fileName);
            FileChannel mappedFileChannel = new RandomAccessFile(mappedFile, "rw").getChannel();
            MappedByteBuffer mappedByteBuffer = mappedFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, fileSize);

            byte[] bytes = new byte[dataSet.size];
            long start = System.currentTimeMillis();
            while (mappedByteBuffer.hasRemaining()) {
                mappedByteBuffer.get(bytes);
            }
            long mmapTime = System.currentTimeMillis() - start;
            ((DirectBuffer) mappedByteBuffer).cleaner().clean();
            mappedFileChannel.close();
            mappedFile.delete();

            fileName = "fileChannel" + dataSet.name;
            File file = new File(fileName);
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            randomAccessFile.setLength(fileSize);
            FileChannel fileChannel = randomAccessFile.getChannel();
            start = System.currentTimeMillis();
            for (int i = 0; i < fileSize; i += dataSet.size) {
                directBuffer.position(0);
                directBuffer.limit(dataSet.size);
                fileChannel.read(directBuffer);
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
