package com.lei.java.forge.http;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;


/**
 * <p>
 * CustomWireMockServer
 * </p>
 *
 * @author 伍磊
 */
public class CustomWireMockServer {

    private final WireMockServer mockServer;

    private final String url;

    public CustomWireMockServer(int fixedDelayMs) {

        int containerThreads = 100; // 增加容器线程数
        int responseThreads = 100;  // 增加响应线程数

        WireMockConfiguration options = WireMockConfiguration.options()
                .port(8080)
                .containerThreads(containerThreads)      // 增加容器线程数
                .jettyAcceptors(4)                       // 增加接收器数量
                .jettyAcceptQueueSize(100)               // 增加接受队列大小
                .asynchronousResponseEnabled(true)       // 启用异步响应
                .asynchronousResponseThreads(responseThreads); // 设置异步响应线程数

        this.mockServer = new WireMockServer(options);
        this.mockServer.start();

        mockServer.stubFor(get(urlEqualTo("/test"))
                .willReturn(aResponse()
                        .withFixedDelay(fixedDelayMs)
                        .withStatus(200)
                        .withBody("Hello")));

        this.url = mockServer.baseUrl() + "/test";
    }

    public String getUrl() {
        return url;
    }

    public void close() {
        this.mockServer.shutdown();
    }

}

