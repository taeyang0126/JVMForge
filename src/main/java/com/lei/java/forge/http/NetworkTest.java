package com.lei.java.forge.http;

import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;

import static com.lei.java.forge.http.CommonMicroServiceTest.*;

/**
 * <p>
 * NetworkTest
 * </p>
 *
 * @author 伍磊
 */
@Log4j2
public class NetworkTest {

    private OkHttpClient okHttpClient;

    @Before
    public void init() {
        okHttpClient = new OkHttpClient.Builder()
                .readTimeout(Duration.ofSeconds(1))
                .writeTimeout(Duration.ofSeconds(1))
                .connectTimeout(Duration.ofSeconds(1))
                .retryOnConnectionFailure(false)
                .build();
    }

    @Test
    public void test_good() throws IOException {
        curl(GOOD_HOST, GOOD_PORT);
    }

    @Test(expected = ConnectException.class)
    public void test_connectTimeout() throws IOException {
        curl(CONNECT_TIMEOUT_HOST, CONNECT_TIMEOUT_PORT);
    }

    @Test(expected = SocketTimeoutException.class)
    public void test_readTimeout() throws IOException {
        curl(READ_TIMEOUT_HOST, READ_TIMEOUT_PORT);
    }

    @Test
    public void test_reset() throws IOException {
        curl(RESET_PEER_HOST, RESET_PEER_PORT);
    }

    private void curl(String goodHost, int goodPort) throws IOException {
        String url = "http://" + goodHost + ":" + goodPort + "/delay/0.5";

        Request request = new Request.Builder()
                .url(url)
                .build();

        var res = okHttpClient
                .newCall(request)
                .execute()
                .body()
                .string();

        log.info("res => {}", res);
    }

}
