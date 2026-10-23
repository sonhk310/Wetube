package com.wetube.wetube_service.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

class JacksonConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
            .withUserConfiguration(JacksonConfig.class);

    @Test
    void localDateTimeIsWrittenAsUtcWithZ() {
        runner.run(ctx -> {
            ObjectMapper mapper = ctx.getBean(ObjectMapper.class);
            LocalDateTime createdAt = LocalDateTime.of(2026, 10, 23, 6, 17, 16, 742_450_000);

            assertThat(mapper.writeValueAsString(Map.of("createdAt", createdAt)))
                    .isEqualTo("{\"createdAt\":\"2026-10-23T06:17:16.742Z\"}");
            // Đọc ngược lại vẫn ra đúng giá trị (client gửi lại chuỗi có "Z")
            assertThat(mapper.readValue("\"2026-10-23T06:17:16.742Z\"", LocalDateTime.class))
                    .isEqualTo(LocalDateTime.of(2026, 10, 23, 6, 17, 16, 742_000_000));
        });
    }
}
