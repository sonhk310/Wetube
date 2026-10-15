package com.wetube.wetube_service.dto;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "livekit")
public class LivekitProperties {
    private String url;
    private String apiKey;
    private String apiSecret;
}
