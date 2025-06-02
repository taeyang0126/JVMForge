package com.lei.java.forge.storage.crc32;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.zip.CRC32;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * <p>
 * CRC32Test
 * </p>
 *
 * @author 伍磊
 */
public class CRC32Test {

    @Test
    public void testBasicString() {
        CRC32 crc32 = new CRC32();
        crc32.update("Hello".getBytes());
        long value1 = crc32.getValue();

        // 相同输入应该得到相同的值
        CRC32 crc32Again = new CRC32();
        crc32Again.update("Hello".getBytes());
        long value2 = crc32Again.getValue();

        assertEquals(value1, value2);
    }

    @Test
    public void testEmptyInput() {
        CRC32 crc32 = new CRC32();
        crc32.update(new byte[0]);
        long emptyValue = crc32.getValue();

        // 空输入应该有确定的值
        assertEquals(0, emptyValue);
    }

    @Test
    public void testIncrementalUpdate() {
        // 分段更新
        CRC32 crc32 = new CRC32();
        crc32.update("Hello".getBytes());
        crc32.update("World".getBytes());
        long incrementalValue = crc32.getValue();

        // 一次性更新
        CRC32 crc32Complete = new CRC32();
        crc32Complete.update("HelloWorld".getBytes());
        long completeValue = crc32Complete.getValue();

        // 结果应该相同
        assertEquals(incrementalValue, completeValue);
    }

    @Test
    public void testReset() {
        CRC32 crc32 = new CRC32();
        crc32.update("Hello".getBytes());
        long value1 = crc32.getValue();

        // 重置后应该能重新计算
        crc32.reset();
        crc32.update("Hello".getBytes());
        long value2 = crc32.getValue();

        assertEquals(value1, value2);
    }

    @Test
    public void testDifferentInputs() {
        CRC32 crc32 = new CRC32();
        crc32.update("Hello".getBytes());
        long value1 = crc32.getValue();

        crc32.reset();
        crc32.update("hello".getBytes());  // 注意大小写不同
        long value2 = crc32.getValue();

        // 不同输入应该得到不同的值
        assertNotEquals(value1, value2);
    }

    @Test
    public void testLargeData() throws IOException {
        // 测试大数据的分块处理
        byte[] largeData = new byte[1024 * 1024];  // 1MB
        // 填充一些数据
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte)(i % 256);
        }

        // 方式1：一次性处理
        CRC32 crc32Once = new CRC32();
        crc32Once.update(largeData);
        long valueOnce = crc32Once.getValue();

        // 方式2：分块处理
        CRC32 crc32Chunked = new CRC32();
        int chunkSize = 8192;
        for (int offset = 0; offset < largeData.length; offset += chunkSize) {
            int length = Math.min(chunkSize, largeData.length - offset);
            crc32Chunked.update(largeData, offset, length);
        }
        long valueChunked = crc32Chunked.getValue();

        assertEquals(valueOnce, valueChunked);
    }

    @Test
    public void testStreamProcessing() throws IOException {
        String content = "Hello World";

        try (InputStream inputStream = new ByteArrayInputStream(content.getBytes())) {
            byte[] buffer = new byte[4];
            CRC32 crc32 = new CRC32();
            int read = 0;
            while ((read = inputStream.read(buffer)) != -1) {
                crc32.update(buffer, 0,read);
            }
            long value1 = crc32.getValue();

            CRC32 directCrc32 = new CRC32();
            directCrc32.update(content.getBytes(StandardCharsets.UTF_8));
            long value2 = crc32.getValue();

            assertEquals(value1, value2);
        }
    }

    @Test
    public void testPartialUpdate() {
        byte[] data = "Hello World".getBytes();

        // 只处理部分数据
        CRC32 crc32Partial = new CRC32();
        crc32Partial.update(data, 6, 5);  // 只处理"World"
        long partialValue = crc32Partial.getValue();

        // 对比完整处理"World"
        CRC32 crc32Complete = new CRC32();
        crc32Complete.update("World".getBytes());
        long completeValue = crc32Complete.getValue();

        assertEquals(completeValue, partialValue);
    }

    @Test
    public void testFile() throws IOException {
        File tempFile = File.createTempFile(UUID.randomUUID().toString(), ".txt");
        tempFile.deleteOnExit();

        String content = "Hello World";
        try(OutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = content.getBytes(StandardCharsets.UTF_8);
            outputStream.write(buffer);
            outputStream.flush();
        }

        try(InputStream inputStream = new FileInputStream(tempFile)) {
            byte[] buffer = new byte[content.length()];
            inputStream.read(buffer);

            CRC32 crc32 = new CRC32();
            crc32.update(buffer, 0, buffer.length);
            long value1 = crc32.getValue();

            crc32.reset();
            crc32.update(content.getBytes(StandardCharsets.UTF_8));
            long value2 = crc32.getValue();

            assertEquals(value1, value2);
        }
    }

    @Test
    public void testByteBuf() throws IOException {
        CompositeByteBuf byteBufs = Unpooled.compositeBuffer();
        ByteBuf byteBuf1 = Unpooled.wrappedBuffer("Hello".getBytes());
        ByteBuf byteBuf2 = Unpooled.wrappedBuffer("World".getBytes());
        byteBufs.addComponents(true, byteBuf1, byteBuf2);
        CRC32 crc32 = new CRC32();
        for (ByteBuffer byteBuffer : byteBufs.nioBuffers()) {
            crc32.update(byteBuffer);
        }
        long value1 = crc32.getValue();

        crc32.reset();
        crc32.update("HelloWorld".getBytes());
        long value2 = crc32.getValue();

        assertEquals(value1, value2);

        byteBufs.release();
    }

}
