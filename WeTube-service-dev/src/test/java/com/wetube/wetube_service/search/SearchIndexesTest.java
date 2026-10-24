package com.wetube.wetube_service.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchIndexesTest {

    @Mock ElasticsearchOperations operations;
    @Mock IndexOperations videosOps;
    @Mock IndexOperations channelsOps;

    SearchIndexes indexes;

    @BeforeEach
    void setUp() {
        when(operations.indexOps(IndexCoordinates.of("videos_v2"))).thenReturn(videosOps);
        when(operations.indexOps(IndexCoordinates.of("channels"))).thenReturn(channelsOps);
        indexes = new SearchIndexes(operations);
    }

    @Test
    void beforeSetupUsesOldVideoIndexAndNoChannelIndex() {
        assertThat(indexes.videos()).isEqualTo(IndexCoordinates.of("videos"));
        assertThat(indexes.channelsReady()).isFalse();
    }

    @Test
    void createsMissingIndexesWithAnalyzersAndSwitches() {
        when(videosOps.exists()).thenReturn(false);
        when(channelsOps.exists()).thenReturn(false);
        when(videosOps.create(anyMap(), any(Document.class))).thenReturn(true);
        when(channelsOps.create(anyMap(), any(Document.class))).thenReturn(true);

        indexes.ensureIndexes();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> settings = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Document> mapping = ArgumentCaptor.forClass(Document.class);
        verify(videosOps).create(settings.capture(), mapping.capture());
        assertThat(settings.getValue().toString()).contains("folded", "compact", "autocomplete", "asciifolding");
        assertThat(mapping.getValue().toString()).contains("channelName", "compact", "prefix", "isShort");
        assertThat(indexes.videos()).isEqualTo(IndexCoordinates.of("videos_v2"));
        assertThat(indexes.channelsReady()).isTrue();
    }

    @Test
    void existingIndexesAreReusedNotRecreated() {
        when(videosOps.exists()).thenReturn(true);
        when(channelsOps.exists()).thenReturn(true);

        indexes.ensureIndexes();

        verify(videosOps, never()).create(anyMap(), any(Document.class));
        assertThat(indexes.videos()).isEqualTo(IndexCoordinates.of("videos_v2"));
        assertThat(indexes.channelsReady()).isTrue();
    }

    @Test
    void failureKeepsOldVideoIndexAndDisablesChannelIndex() {
        when(videosOps.exists()).thenThrow(new RuntimeException("403 forbidden"));
        when(channelsOps.exists()).thenReturn(false);
        when(channelsOps.create(anyMap(), any(Document.class))).thenThrow(new RuntimeException("403 forbidden"));

        indexes.ensureIndexes();

        assertThat(indexes.videos()).isEqualTo(IndexCoordinates.of("videos"));
        assertThat(indexes.channelsReady()).isFalse();
    }
}
