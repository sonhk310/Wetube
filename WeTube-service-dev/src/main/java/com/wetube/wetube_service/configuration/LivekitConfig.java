package com.wetube.wetube_service.configuration;
import io.livekit.server.IngressServiceClient;
import io.livekit.server.RoomServiceClient;
import io.livekit.server.okhttp.OkHttpFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class LivekitConfig {
    @Value("${livekit.url}")
    private String wsUrl;

    @Value("${livekit.api-key}")
    private String apiKey;

    @Value("${livekit.api-secret}")
    private String apiSecret;

    private String normalizeHost() {
        return wsUrl.replace("wss://", "https://").replace("ws://", "http://");
    }

    @Bean
    public IngressServiceClient ingressServiceClient() {
        return IngressServiceClient.createClient(
                normalizeHost(),
                apiKey,
                apiSecret,
                new OkHttpFactory()
        );
    }

    @Bean
    public RoomServiceClient roomServiceClient() {
        return RoomServiceClient.createClient(
                normalizeHost(),
                apiKey,
                apiSecret,
                new OkHttpFactory()
        );
    }
}
