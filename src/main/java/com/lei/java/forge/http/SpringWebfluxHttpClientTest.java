package com.lei.java.forge.http;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.function.Tuple2;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * SpringWebfluxHttpClientTest
 * </p>
 *
 * @author 伍磊
 */
public class SpringWebfluxHttpClientTest {

    @Test
    public void test_wireMockServer() {


        // 使用 WireMockServer 进行测试
        // 测试 1000 个请求
        // 每个请求耗时 0.1 s
        // 不设置连接数量
        // 最大连接为 100
        // 总的请求时间在 1.4s 左右，活跃线程=54

        int requestTotal = 1000;
        int fixedDelayMs = 100;
        int maxConnection = 100;

        CustomWireMockServer customWireMockServer = new CustomWireMockServer(fixedDelayMs);
        String url = customWireMockServer.getUrl();

        printActiveThreadCount();

        WebClient webClient = createWebClient(maxConnection);

        List<Mono<String>> monos = Lists.newArrayList();
        for (int i = 0; i < requestTotal; i++) {
            Mono<String> stringMono = webClient.get().uri(url)
                    .retrieve()
                    .bodyToMono(String.class);
            monos.add(stringMono);
        }

        long start = System.currentTimeMillis();
        String block = Mono.zip(monos, objects -> "ok").block();
        long end = System.currentTimeMillis();
        System.out.println("请求完成 -> 耗时: " + (end - start));
    }

    private WebClient createWebClient(int maxConnection) {
        // 创建一个自定义的连接提供者
        ConnectionProvider provider = ConnectionProvider.builder("customConnectionProvider")
                .maxConnections(maxConnection) // 增加最大连接数，这个不能太大，否则会被 cloudflare 等 cdn 认为是类 ddos 攻击
                .pendingAcquireMaxCount(10000) // 增加等待队列的大小
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .responseTimeout(Duration.ofMillis(100000)); // 响应超时时间
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient)).build();
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
