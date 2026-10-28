package com.wetube.wetube_service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.MapPropertySource;
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
    void theDocumentedEnvironmentVariablesFillTheProperties() {
        LiveRecordingProperties p = bind(Map.of(
                "LIVE_RECORDING_ENABLED", "true",
                "LIVE_RECORDING_S3_ENDPOINT", "https://acc.r2.cloudflarestorage.com",
                "LIVE_RECORDING_S3_BUCKET", "wetube-live",
                "LIVE_RECORDING_S3_ACCESS_KEY", "access",
                "LIVE_RECORDING_S3_SECRET", "secret",
                "LIVE_RECORDING_PUBLIC_URL", "https://pub.example",
                "LIVE_RECORDING_CHANNEL_MINUTES", "20",
                "LIVE_RECORDING_TOTAL_MINUTES", "50"));

        assertThat(p.isEnabled()).isTrue();
        assertThat(p.getS3Endpoint()).isEqualTo("https://acc.r2.cloudflarestorage.com");
        assertThat(p.getS3Bucket()).isEqualTo("wetube-live");
        assertThat(p.getS3AccessKey()).isEqualTo("access");
        assertThat(p.getS3Secret()).isEqualTo("secret");
        assertThat(p.getPublicUrl()).isEqualTo("https://pub.example");
        assertThat(p.getChannelMinutes()).isEqualTo(20);
        assertThat(p.getTotalMinutes()).isEqualTo(50);
        assertThat(p.isAvailable()).isTrue();
    }

    @Test
    void theRegionVariableIsOptionalAndDefaultsToAuto() {
        assertThat(bind(Map.of()).getS3Region()).isEqualTo("auto");
        assertThat(bind(Map.of("LIVE_RECORDING_S3_REGION", "us-west-004")).getS3Region()).isEqualTo("us-west-004");
    }

    @Test
    void withoutAnyVariableTheFeatureIsOffWithTheDefaultShares() {
        LiveRecordingProperties p = bind(Map.of());

        assertThat(p.isAvailable()).isFalse();
        assertThat(p.getChannelMinutes()).isEqualTo(15);
        assertThat(p.getTotalMinutes()).isEqualTo(55);
    }

    @Test
    void enabledButMissingAnyStorageSettingStaysOff() {
        LiveRecordingProperties p = bind(Map.of(
                "LIVE_RECORDING_ENABLED", "true",
                "LIVE_RECORDING_S3_ENDPOINT", "https://acc.r2.cloudflarestorage.com",
                "LIVE_RECORDING_S3_BUCKET", "wetube-live",
                "LIVE_RECORDING_S3_ACCESS_KEY", "access",
                "LIVE_RECORDING_S3_SECRET", "secret"));

        assertThat(p.isAvailable()).isFalse(); // thiếu LIVE_RECORDING_PUBLIC_URL
    }

    @Test
    void anyOtherPropertySourceStillWorksToo() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("yml", Map.of("live.recording.enabled", "true")));
        LiveRecordingProperties p = new Binder(ConfigurationPropertySources.get(environment))
                .bind("live.recording", LiveRecordingProperties.class).get();

        assertThat(p.isEnabled()).isTrue();
    }
}
