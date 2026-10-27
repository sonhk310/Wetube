package com.wetube.wetube_service.service.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

import com.wetube.wetube_service.dto.response.ChannelSearchDto;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.repository.channel.ChannelRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.security.VideoAccess;
import com.wetube.wetube_service.search.ChannelDocument;
import com.wetube.wetube_service.search.SearchIndexes;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChannelSearchServiceTest {

    @Mock ChannelRepository channelRepository;
    @Mock ElasticsearchOperations operations;
    @Mock SearchIndexes indexes;
    @Mock VideoRepository videoRepository;
    @InjectMocks ChannelSearchService service;

    private static Channel channel(String name, ActiveStatus status) {
        Channel c = new Channel();
        c.setId(UUID.randomUUID());
        c.setName(name);
        c.setStatus(status);
        c.setTotalSubscribers(3);
        return c;
    }

    @SuppressWarnings("unchecked")
    private void esReturns(Channel... channels) {
        SearchHits<ChannelDocument> hits = mock(SearchHits.class);
        List<SearchHit<ChannelDocument>> list = new java.util.ArrayList<>();
        for (Channel c : channels) {
            SearchHit<ChannelDocument> hit = mock(SearchHit.class);
            when(hit.getContent()).thenReturn(new ChannelDocument(c.getId().toString(), c.getName(), 0));
            list.add(hit);
        }
        when(hits.getSearchHits()).thenReturn(list);
        when(operations.search(any(Query.class), eq(ChannelDocument.class), eq(SearchIndexes.CHANNELS)))
                .thenReturn(hits);
    }

    @Test
    void withoutChannelIndexUsesMySqlAndHidesHiddenChannels() {
        when(indexes.channelsReady()).thenReturn(false);
        when(channelRepository.findTop20ByNameContainingIgnoreCaseOrderByTotalSubscribersDesc("son"))
                .thenReturn(List.of(channel("Sơn TV", ActiveStatus.ACTIVE), channel("Son cũ", ActiveStatus.INACTIVE),
                        channel("Son xoá", ActiveStatus.DELETED), channel("Son mới", null)));

        List<ChannelSearchDto> result = service.search(" son ", 20, false);

        assertThat(result).extracting(ChannelSearchDto::name).containsExactly("Sơn TV", "Son mới");
        assertThat(result.get(0).totalSubscribers()).isEqualTo(3);
    }

    @Test
    void videoCountIsCountedNotReadFromTheStaleColumn() {
        Channel c = channel("Sơn TV", ActiveStatus.ACTIVE);
        when(indexes.channelsReady()).thenReturn(false);
        when(channelRepository.findTop20ByNameContainingIgnoreCaseOrderByTotalSubscribersDesc("son"))
                .thenReturn(List.of(c));
        when(videoRepository.countChannelVideos(c.getId(), VideoAccess.LISTED)).thenReturn(4L);

        assertThat(service.search("son", 20, false).get(0).totalVideos()).isEqualTo(4);
    }

    @Test
    void limitApplies() {
        when(indexes.channelsReady()).thenReturn(false);
        when(channelRepository.findTop20ByNameContainingIgnoreCaseOrderByTotalSubscribersDesc("son"))
                .thenReturn(List.of(channel("A", null), channel("B", null), channel("C", null)));

        assertThat(service.search("son", 2, true)).hasSize(2);
    }

    @Test
    void withChannelIndexKeepsElasticOrderAndLoadsFreshDataFromMySql() {
        Channel first = channel("Sơn Tùng M-TP", ActiveStatus.ACTIVE);
        Channel second = channel("Sơn TV", ActiveStatus.ACTIVE);
        Channel hidden = channel("Sơn ẩn", ActiveStatus.INACTIVE);
        when(indexes.channelsReady()).thenReturn(true);
        esReturns(first, hidden, second);
        when(channelRepository.findAllById(any())).thenReturn(List.of(second, hidden, first));

        List<ChannelSearchDto> result = service.search("sontung", 20, false);

        assertThat(result).extracting(ChannelSearchDto::name).containsExactly("Sơn Tùng M-TP", "Sơn TV");
        verify(channelRepository, never()).findTop20ByNameContainingIgnoreCaseOrderByTotalSubscribersDesc(anyString());
    }

    @Test
    void elasticErrorFallsBackToMySql() {
        when(indexes.channelsReady()).thenReturn(true);
        when(operations.search(any(Query.class), eq(ChannelDocument.class), eq(SearchIndexes.CHANNELS)))
                .thenThrow(new RuntimeException("es down"));
        when(channelRepository.findTop20ByNameContainingIgnoreCaseOrderByTotalSubscribersDesc("son"))
                .thenReturn(List.of(channel("Sơn TV", ActiveStatus.ACTIVE)));

        assertThat(service.search("son", 20, false)).extracting(ChannelSearchDto::name).containsExactly("Sơn TV");
    }

    @Test
    void blankQueryReturnsNothing() {
        assertThat(service.search("  ", 20, false)).isEmpty();
        verify(channelRepository, never()).findTop20ByNameContainingIgnoreCaseOrderByTotalSubscribersDesc(anyString());
    }
}
