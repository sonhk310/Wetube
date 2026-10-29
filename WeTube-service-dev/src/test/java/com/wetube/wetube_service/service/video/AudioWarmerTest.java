package com.wetube.wetube_service.service.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;
import com.wetube.wetube_service.service.RedisService;

/** Làm nóng MP3 phát nền: một yêu cầu cho mỗi video, hỏng thì bỏ dấu để thử lại lần sau. */
class AudioWarmerTest {

    private final RedisService redis = mock(RedisService.class);
    private HttpServer server;
    private final AtomicInteger hits = new AtomicInteger();
    private CountDownLatch served = new CountDownLatch(1);
    private volatile int status = 200;
    private AudioWarmer warmer;
    private String url;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/a.mp3", exchange -> {
            hits.incrementAndGet();
            byte[] body = new byte[200_000];
            exchange.sendResponseHeaders(status, status == 200 ? body.length : -1);
            if (status == 200) {
                try {
                    exchange.getResponseBody().write(body);
                } catch (IOException ignored) {
                    // client ngắt sau khi đọc đủ: đúng như thiết kế
                }
            }
            exchange.close();
            served.countDown();
        });
        server.start();
        url = "http://127.0.0.1:" + server.getAddress().getPort() + "/a.mp3";
        warmer = new AudioWarmer(redis, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
    }

    @AfterEach
    void tearDown() {
        warmer.shutdown();
        server.stop(0);
    }

    @Test
    void theFirstRequestForAVideoFetchesTheMp3Once() throws Exception {
        UUID video = UUID.randomUUID();
        when(redis.setIfAbsent(eq("audio-warm:" + video), org.mockito.ArgumentMatchers.any())).thenReturn(true);

        warmer.warm(video, url);

        assertThat(served.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(hits.get()).isEqualTo(1);
        verify(redis, never()).delete(anyString());
    }

    @Test
    void aVideoAlreadyWarmedIsNotFetchedAgain() throws Exception {
        UUID video = UUID.randomUUID();
        when(redis.setIfAbsent(eq("audio-warm:" + video), org.mockito.ArgumentMatchers.any())).thenReturn(false);

        warmer.warm(video, url);

        assertThat(served.await(500, TimeUnit.MILLISECONDS)).isFalse();
        assertThat(hits.get()).isZero();
    }

    @Test
    void aFailedWarmUpClearsTheFlagSoTheNextViewRetries() throws Exception {
        UUID video = UUID.randomUUID();
        when(redis.setIfAbsent(eq("audio-warm:" + video), org.mockito.ArgumentMatchers.any())).thenReturn(true);
        status = 500;

        warmer.warm(video, url);

        assertThat(served.await(5, TimeUnit.SECONDS)).isTrue();
        verify(redis, timeout(3000)).delete("audio-warm:" + video);
    }

    @Test
    void withoutRedisEachVideoIsStillWarmedOnlyOnce() throws Exception {
        UUID video = UUID.randomUUID();
        when(redis.setIfAbsent(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(null);

        warmer.warm(video, url);
        assertThat(served.await(5, TimeUnit.SECONDS)).isTrue();
        warmer.warm(video, url);
        Thread.sleep(300);

        assertThat(hits.get()).isEqualTo(1);
    }
}
