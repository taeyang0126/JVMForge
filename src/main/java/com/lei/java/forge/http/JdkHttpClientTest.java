package com.lei.java.forge.http;

import lombok.extern.log4j.Log4j2;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.lei.java.forge.http.CommonMicroServiceTest.HTTPBIN_PORT;

/**
 * <p>
 * jdk http client 测试
 * </p>
 *
 * @author 伍磊
 */
@Log4j2
public class JdkHttpClientTest {

    @Test
    public void test_httpbin() throws InterruptedException {

        // 每个请求耗时 0.1 s
        // 线程数量 = 1
        // 线程数量 = 1
        // 耗时=0.9s

        int threadCount = 1;
        int requestTotal = 1000;
        double delay = 0.1;

        String url = "http://localhost:" +
                CommonMicroServiceTest.HTTPBIN_CONTAINER.getMappedPort(HTTPBIN_PORT) + "/delay/" + delay;

        benchmarkTest(url, threadCount, requestTotal);
    }

    @Test
    public void test_wireMockServer() throws InterruptedException {

        // 使用 WireMockServer 进行测试
        // 每个请求耗时 0.1 s
        // 线程数量 = 1
        // 测试 1000 个请求 总的请求时间在 0.5 左右，说明 jdkHttpClient 使用 nio
        // 测试 10000 个请求 总的请求时间在 1.2 左右，说明 jdkHttpClient 使用 nio
        // 之所以有这么高的请求性能，是因为每个请求都建立的连接，所以效率非常高
        // netstat -an | grep tcp | awk '{print $6}' | sort | uniq -c
        // 可以看到有大量的 time_wait 状态的连接，证明此框架没有连接复用，生产不建议使用

        int threadCount = 1;
        int requestTotal = 1000;
        int fixedDelayMs = 100;



        CustomWireMockServer customWireMockServer = new CustomWireMockServer(fixedDelayMs);
        String url = customWireMockServer.getUrl();

        benchmarkTest(url, threadCount, requestTotal);

        customWireMockServer.close();
    }

    private static void benchmarkTest(String url, int threadCount, int requestTotal) throws InterruptedException {
        ExecutorService jdkExecutorService = Executors.newFixedThreadPool(threadCount);
        HttpClient jdkHttpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .executor(jdkExecutorService)
                .build();

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        printActiveThreadCount();

        CountDownLatch countDownLatch = new CountDownLatch(requestTotal);

        long start = System.currentTimeMillis();

        for (int i = 0; i < requestTotal; i++) {
            jdkHttpClient.sendAsync(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            ).thenApply(response -> {
                // 确保处理响应体
                String body = response.body(); // 这行强制消费响应体
                // 可以添加一些处理，确保编译器不会优化掉
                if (body != null && !body.isEmpty()) {
                    // 可以记录一些信息，例如每100个请求记录一次
                    if (Math.random() < 0.01) {
                        System.out.println("响应体: " + body);
                    }
                }
                return response; // 返回完整响应
            }).whenComplete((response, error) -> {
                // 无论成功失败，都减少计数
                countDownLatch.countDown();
            });
        }

        log.info("添加时间: {}", (System.currentTimeMillis() - start));

        countDownLatch.await();

        long end = System.currentTimeMillis();
        log.info("请求完成 -> 耗时: {}", (end - start));

        // 等待3s，打印timewait
        // Thread.sleep(Duration.ofSeconds(30));

        jdkHttpClient.close();
        jdkExecutorService.shutdownNow();
    }

    private static void printActiveThreadCount() {
        Executors.newScheduledThreadPool(1)
                .scheduleAtFixedRate(() -> {
                    var threadMXBean = ManagementFactory.getThreadMXBean();
                    int activeThreadCount = threadMXBean.getThreadCount();
                    log.info("当前活跃线程数量: {}", activeThreadCount);
                    try {
                        String[] cmd = {"/bin/bash", "-c", "netstat -an | grep tcp | awk '$6 == \"TIME_WAIT\" {count++} END {print count}'"};
                        Process process = Runtime.getRuntime().exec(cmd);
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                            log.info("timeWait nums: {}", reader.lines().collect(Collectors.joining("\n")));
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, 0, 100, TimeUnit.MILLISECONDS);
    }

}
