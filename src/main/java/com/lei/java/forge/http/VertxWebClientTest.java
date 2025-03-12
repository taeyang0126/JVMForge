package com.lei.java.forge.http;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.junit.Test;

import java.lang.management.ManagementFactory;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * VertxWebClientTest
 * </p>
 *
 * @author 伍磊
 */
public class VertxWebClientTest {

    @Test
    public void test_wireMockServer() throws InterruptedException {


        // 使用 WireMockServer 进行测试
        // 测试 1000 个请求
        // 每个请求耗时 0.1 s
        // 不设置连接数量
        // 最大连接为 100
        // 总的请求时间在 1.3s 左右，活跃线程=37

        int requestTotal = 1000;
        int fixedDelayMs = 100;
        int maxConnection = 100;

        CustomWireMockServer customWireMockServer = new CustomWireMockServer(fixedDelayMs);
        String url = customWireMockServer.getUrl();

        printActiveThreadCount();
        WebClient webClient = createWebClient(maxConnection);

        long start = System.currentTimeMillis();

        CountDownLatch countDownLatch = new CountDownLatch(requestTotal);

        for (int i = 0; i < requestTotal; i++) {
            webClient.getAbs(url)
                    .send(event -> countDownLatch.countDown());
        }

        countDownLatch.await();
        long end = System.currentTimeMillis();
        System.out.println("请求完成 -> 耗时: " + (end - start));

    }

    private WebClient createWebClient(int maxConnection) {
        Vertx vertx = Vertx.vertx();
        // 配置 WebClient
        WebClientOptions options = new WebClientOptions()
                .setUserAgent("Vert.x-WebClient/1.0")
                .setKeepAlive(true)
                .setHttp2ClearTextUpgrade(false) // 是否尝试HTTP/1.1升级到HTTP/2
                .setConnectTimeout(5000) // 连接超时时间
                .setIdleTimeout(10) // 空闲超时(秒)
                .setMaxPoolSize(maxConnection) // 连接池大小
                .setProtocolVersion(HttpVersion.HTTP_1_1); // 使用 HTTP/1.1
        // 创建 WebClient
        return WebClient.create(vertx, options);
    }

    private void printActiveThreadCount() {
        Executors.newScheduledThreadPool(1)
                .scheduleAtFixedRate(() -> {
                    var threadMXBean = ManagementFactory.getThreadMXBean();
                    int activeThreadCount = threadMXBean.getThreadCount();
                    System.out.println("当前活跃线程数量: " + activeThreadCount);
                }, 0, 100, TimeUnit.MILLISECONDS);
    }

}
