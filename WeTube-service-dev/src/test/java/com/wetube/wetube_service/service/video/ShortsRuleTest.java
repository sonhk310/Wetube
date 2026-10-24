package com.wetube.wetube_service.service.video;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ShortsRuleTest {

    @Test
    void verticalOrSquareUpTo180SecondsIsShort() {
        assertThat(ShortsRule.isShort(30f, 1080, 1920)).isTrue();
        assertThat(ShortsRule.isShort(170f, 1080, 1920)).isTrue();
        assertThat(ShortsRule.isShort(60f, 1080, 1080)).isTrue();
        assertThat(ShortsRule.isShort(180f, 720, 1280)).isTrue();
    }

    @Test
    void landscapeOrLongerThan180SecondsIsNotShort() {
        assertThat(ShortsRule.isShort(60f, 1920, 1080)).isFalse();
        assertThat(ShortsRule.isShort(190f, 1080, 1920)).isFalse();
    }

    @Test
    void missingSizeOrDurationIsNotShort() {
        assertThat(ShortsRule.isShort(30f, null, null)).isFalse();
        assertThat(ShortsRule.isShort(0f, 1080, 1920)).isFalse();
        assertThat(ShortsRule.isShort(null, 1080, 1920)).isFalse();
    }
}
