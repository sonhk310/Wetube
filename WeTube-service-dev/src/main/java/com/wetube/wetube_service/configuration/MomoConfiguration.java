    package com.wetube.wetube_service.configuration;

    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.context.annotation.Configuration;

    import lombok.Getter;


    @Configuration
    @Getter
    public class MomoConfiguration {
        @Value("${momo.partnerCode}")
        private String partnerCode;

        @Value("${momo.accessKey}")
        private String accessKey;

        @Value("${momo.secretKey}")
        private String secretKey;

        @Value("${momo.endpoint}")
        private String endpoint;
        
        @Value("${momo.redirectUrl}")
        private String redirectUrl;

        @Value("${momo.ipnUrl}")
        private String ipnUrl;
    }
