package com.wetube.wetube_service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

/** Tên biến môi trường trên Vibe Host phải gán đúng vào các trường (quy tắc ghép tên của Spring dễ sai với dấu gạch). */
class LiveRecordingPropertiesTest {

    private static LiveRecordingProperties bind(Map<String, Object> env) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource("test-env", env));
        return new Binder(ConfigurationPropertySources.get(environment))
                .bind("live.recording", LiveRecordingProperties.class)
                .orElseGet(LiveRecordingProperties::new);
    }

    @Test
    void withoutAnyVariableTheFeatureIsOnWithTheDefaultShares() {
        LiveRecordingProperties p = bind(Map.of());

        assertThat(p.isEnabled()).isTrue();
        assertThat(p.getChannelMinutes()).isEqualTo(15);
        assertThat(p.getTotalMinutes()).isEqualTo(55);
    }

    @Test
    void theDocumentedEnvironmentVariablesFillTheProperties() {
        LiveRecordingProperties p = bind(Map.of(
                "LIVE_RECORDING_ENABLED", "false",
                "LIVE_RECORDING_CHANNEL_MINUTES", "20",
                "LIVE_RECORDING_TOTAL_MINUTES", "50"));

        assertThat(p.isEnabled()).isFalse();
        assertThat(p.getChannelMinutes()).isEqualTo(20);
        assertThat(p.getTotalMinutes()).isEqualTo(50);
    }
}
