package com.wetube.wetube_service.service.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class TopicCatalogTest {

    private final TopicCatalog catalog = new TopicCatalog();

    @Test
    void theRealFileLoadsWithUniqueIdsUnderTheirGroup() {
        // Hàm dựng tự kiểm tra id trùng / id con không bắt đầu bằng id nhóm
        assertThat(catalog.groups()).hasSize(51);
        assertThat(catalog.groups()).extracting(TopicCatalog.Group::id).doesNotContain("format");
        assertThat(catalog.groups()).allSatisfy(g -> {
            assertThat(g.chip()).isNotBlank();
            assertThat(g.topics()).isNotEmpty();
        });
    }

    @Test
    void macroAndMicroEconomicsHaveDifferentIds() {
        assertThat(catalog.exists("finance.macroeconomics")).isTrue();
        assertThat(catalog.exists("finance.microeconomics")).isTrue();
        assertThat(catalog.label("finance.macroeconomics")).isEqualTo("Kinh tế vĩ mô");
    }

    @Test
    void aSubTopicBringsItsGroupAlong() {
        assertThat(catalog.withGroup("music.lo-fi")).containsExactly("music.lo-fi", "music");
        assertThat(catalog.withGroup("music")).containsExactly("music");
        assertThat(catalog.withGroup("music.khong-co")).isEmpty();
        assertThat(catalog.withGroup(null)).isEmpty();
    }

    @Test
    void chipLabelIsTheShortGroupNameOrTheTopicName() {
        assertThat(catalog.label("gaming")).isEqualTo("Trò chơi");
        assertThat(catalog.label("sports.bong-da")).isEqualTo("Bóng đá");
    }

    @Test
    void aBrokenListFailsAtStartup() {
        var topic = new TopicCatalog.Topic("sports.bong-da", "Bóng đá");
        assertThatThrownBy(() -> new TopicCatalog(List.of(new TopicCatalog.Group("music", "Âm nhạc", "Âm nhạc", List.of(topic)))))
                .isInstanceOf(IllegalStateException.class);
        var group = new TopicCatalog.Group("music", "Âm nhạc", "Âm nhạc", List.of());
        assertThatThrownBy(() -> new TopicCatalog(List.of(group, group))).isInstanceOf(IllegalStateException.class);
    }
}
