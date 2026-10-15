package com.wetube.wetube_service.configuration;

import com.maxmind.geoip2.DatabaseReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class GeoIPConfig {

    @Value("${geoip.database}")
    private Resource geoIpDatabase;

    @Bean
    public DatabaseReader databaseReader() throws IOException {
        try (InputStream in = geoIpDatabase.getInputStream()) {
            return new DatabaseReader.Builder(in).build();
        }
    }
}
