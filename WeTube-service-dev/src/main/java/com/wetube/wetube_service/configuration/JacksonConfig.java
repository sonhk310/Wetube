package com.wetube.wetube_service.configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Thời gian trong DB là LocalDateTime theo giờ UTC (server chạy UTC, xem Application). Trả ra JSON kèm "Z"
 * để trình duyệt đổi đúng sang giờ của người xem; thiếu "Z" thì trình duyệt coi là giờ địa phương và lệch 7 tiếng.
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter UTC_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private static final JsonSerializer<LocalDateTime> UTC_LOCAL_DATE_TIME = new JsonSerializer<>() {
        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(UTC_FORMAT.format(value));
        }
    };

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer utcLocalDateTimeSerializer() {
        return builder -> builder.serializerByType(LocalDateTime.class, UTC_LOCAL_DATE_TIME);
    }
}
