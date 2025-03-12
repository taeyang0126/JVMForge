package com.lei.java.forge.http;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * OkHttpClientTest
 * </p>
 *
 * @author 伍磊
 */
public class OkHttpClientTest {

    @Test
    public void test_wireMockServer() throws IOException, InterruptedException {

        // 使用 WireMockServer 进行测试
        // 测试 1000 个请求
        // 每个请求耗时 0.1 s

        // 最大连接为 1
        // =====> 总的时间在 106s 左右，1 * 106 / 0.1 = 1000个请求

        // 最大连接为 5 (默认情况)
        // =====> 总的时间在 21s 左右，5 * 21 / 0.1 = 1000个请求

        // 最大连接为 100
        // ======> 总的时间在 1.2s 左右，线程数量相比其他有很多的增加

        int requestTotal = 1000;
        int fixedDelayMs = 100;
        int maxConnection = 100;

        printActiveThreadCount();

        CustomWireMockServer customWireMockServer = new CustomWireMockServer(fixedDelayMs);
        String url = customWireMockServer.getUrl();
        Request request = new Request.Builder()
                .url(url)
                .build();

        CountDownLatch countDownLatch = new CountDownLatch(requestTotal);

        OkHttpClient okHttpClient = createOkHttpClient(maxConnection);
        Callback callback = getcallback(countDownLatch);
        long start = System.currentTimeMillis();

        for (int i = 0; i < requestTotal; i++) {
            okHttpClient.newCall(request).enqueue(callback);
        }

        countDownLatch.await();

        long end = System.currentTimeMillis();
        System.out.println("请求完成 -> 耗时: " + (end - start) + "ms");


    }

    @NotNull
    private static Callback getcallback(CountDownLatch countDownLatch) {
        return new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                countDownLatch.countDown();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try {
                    countDownLatch.countDown();
                } finally {
                    response.close(); // 确保关闭
                }
            }
        };
    }


    private OkHttpClient createOkHttpClient(int maxConnection) {
        // 活跃连接
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(maxConnection);
        dispatcher.setMaxRequestsPerHost(maxConnection);

        return new OkHttpClient.Builder()
                // 连接超时
                .connectTimeout(10, TimeUnit.SECONDS)
                // 读取超时
                .readTimeout(10, TimeUnit.SECONDS)
                // 写入超时
                .writeTimeout(10, TimeUnit.SECONDS)
                // 活跃连接配置
                .dispatcher(dispatcher)
                // 允许重试
                .retryOnConnectionFailure(true)
                .build();
    }

    private void printActiveThreadCount() {
        Executors.newScheduledThreadPool(1)
                .scheduleAtFixedRate(() -> {
                    var threadMXBean = ManagementFactory.getThreadMXBean();
                    int activeThreadCount = threadMXBean.getThreadCount();
                    System.out.println("当前活跃线程数量: " + activeThreadCount);
                }, 0, 1, TimeUnit.SECONDS);
    }

}
