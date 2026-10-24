package com.wetube.wetube_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.search.SearchIndexes;
import com.wetube.wetube_service.search.VideoDocument;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VideoIndexerTest {

    @Mock ElasticsearchOperations operations;
    @Mock SearchIndexes indexes;
    @InjectMocks VideoIndexer indexer;

    private final IndexCoordinates v2 = IndexCoordinates.of("videos_v2");

    private Video video(ActiveStatus status) {
        Channel channel = new Channel();
        channel.setId(UUID.randomUUID());
        channel.setName("Sơn Tùng M-TP");
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setChannel(channel);
        Video v = new Video();
        v.setId(UUID.randomUUID());
        v.setUser(user);
        v.setTitle("Lạc trôi");
        v.setVideosStatus(status);
        v.setCreatedAt(LocalDateTime.now());
        return v;
    }

    @Test
    void listedVideoGoesToCurrentIndexWithChannelName() {
        when(indexes.videos()).thenReturn(v2);
        Video v = video(ActiveStatus.ACTIVE);

        indexer.index(v);

        ArgumentCaptor<VideoDocument> doc = ArgumentCaptor.forClass(VideoDocument.class);
        verify(operations).save(doc.capture(), eq(v2));
        assertThat(doc.getValue().getChannelName()).isEqualTo("Sơn Tùng M-TP");
        assertThat(doc.getValue().getId()).isEqualTo(v.getId().toString());
    }

    @Test
    void hiddenVideoIsRemovedFromCurrentIndex() {
        when(indexes.videos()).thenReturn(v2);
        Video v = video(ActiveStatus.PRIVATE);

        indexer.index(v);

        verify(operations).delete(v.getId().toString(), v2);
    }
}
