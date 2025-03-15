package com.lei.java.forge.http;

import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.lei.java.forge.http.CommonMicroServiceTest.HTTPBIN_PORT;

/**
 * <p>
 * 异步的 apache httpClient 测试
 * </p>
 *
 * @author 伍磊
 */
@Log4j2
public class AsyncApacheHttpClientTest {

    /*    @ParameterizedTest(name = "测试 #{index}: 线程数={0}, 请求数={1}, 延迟={2}, 最大连接={3}")
        @MethodSource("performanceTestParameters")*/
    @Test
    public void test_httpbin() {

        // 使用 httpbin 进行测试
        // 测试 1000 个请求
        // 每个请求耗时 0.1 s
        // 线程数量 = 1
        // 最大连接为 100
        // 总的请求时间在 1.5s 左右  ===> 1000 * 0.1 / 100 = 1s

        int threadCount = 1;
        int requestTotal = 1000;
        double delay = 0.1;
        int maxConnection = 100;

        // 请求
        String url = "http://localhost:" +
                CommonMicroServiceTest.HTTPBIN_CONTAINER.getMappedPort(HTTPBIN_PORT) + "/delay/" + delay;

        benchmarkTest(url, threadCount, maxConnection, requestTotal);

    }

    @Test
    public void test_wireMockServer() {

        // 使用 WireMockServer 进行测试
        // 测试 1000 个请求
        // 每个请求耗时 0.1 s
        // 线程数量 = 1
        // 最大连接为 100
        // 总的请求时间在 1.2s 左右  ===> 1000 * 0.1 / 100 = 1s

        int threadCount = 1;
        int requestTotal = 1000;
        int fixedDelayMs = 100;
        int maxConnection = 100;

        CustomWireMockServer customWireMockServer = new CustomWireMockServer(fixedDelayMs);
        String url = customWireMockServer.getUrl();

        benchmarkTest(url, threadCount, maxConnection, requestTotal);

    }

    private void benchmarkTest(String url, int threadCount, int maxConnection, int requestTotal) {
        HttpGet request = new HttpGet(url);

        printActiveThreadCount();

        try (CloseableHttpAsyncClient httpAsyncClient = createHttpAsyncClient(threadCount, maxConnection)) {
            httpAsyncClient.start();
            long start = System.currentTimeMillis();

            CountDownLatch countDownLatch = new CountDownLatch(requestTotal);

            FutureCallback<HttpResponse> futureCallback = getFutureCallback(countDownLatch);

            for (int i = 0; i < requestTotal; i++) {
                httpAsyncClient.execute(request, futureCallback);
            }

            countDownLatch.await();
            long end = System.currentTimeMillis();
            log.info("请求完成 -> 耗时: {}", (end - start));

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private static FutureCallback<HttpResponse> getFutureCallback(CountDownLatch countDownLatch) {
        FutureCallback<HttpResponse> futureCallback = new FutureCallback<>() {
            @Override
            public void completed(HttpResponse httpResponse) {
                countDownLatch.countDown();
            }

            @Override
            public void failed(Exception e) {
                countDownLatch.countDown();
            }

            @Override
            public void cancelled() {
                countDownLatch.countDown();
            }
        };
        return futureCallback;
    }


    /**
     * 创建并配置异步HTTP客户端
     */
    private CloseableHttpAsyncClient createHttpAsyncClient(int threadCount, int maxConnection) throws IOReactorException {
        // 1. 配置IO线程
        IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
                .setIoThreadCount(threadCount) // IO线程数
                .setConnectTimeout(5000)      // 连接超时时间
                .setSoTimeout(5000)           // 数据传输超时时间
                .setSoKeepAlive(true)         // 保持连接活跃
                .setTcpNoDelay(true)          // 禁用Nagle算法，提高实时性
                .build();

        // 2. 创建IO反应器
        ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(ioReactorConfig);

        // 3. 创建连接管理器
        PoolingNHttpClientConnectionManager connectionManager =
                new PoolingNHttpClientConnectionManager(ioReactor);
        connectionManager.setMaxTotal(maxConnection);                // 最大连接数
        connectionManager.setDefaultMaxPerRoute(maxConnection);       // 每个路由的最大连接数，这个连接不能太大，会被 cdn 认为是类 ddos 攻击

        // 4. 请求配置
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5000)                   // 连接超时
                .setSocketTimeout(5000)                    // 读取超时
                .setConnectionRequestTimeout(5000)         // 从连接池获取连接的超时时间
                .build();

        // 5. 创建并配置HttpClient
        return HttpAsyncClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    private void printActiveThreadCount() {
        Executors.newScheduledThreadPool(1)
                .scheduleAtFixedRate(() -> {
                    var threadMXBean = ManagementFactory.getThreadMXBean();
                    int activeThreadCount = threadMXBean.getThreadCount();
                    log.info("当前活跃线程数量: {}", activeThreadCount);
                }, 0, 100, TimeUnit.MILLISECONDS);
    }

/*    static Stream<Arguments> performanceTestParameters() {
        return Stream.of(
                Arguments.of(1, 1000, 0.1, 100),
                Arguments.of(1, 1000, 0.1, 200),
                Arguments.of(10, 1000, 0.1, 100),
                Arguments.of(1, 10000, 0.1, 100)
        );
    }*/


}
