package com.wetube.wetube_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;

import com.wetube.wetube_service.dto.video.VideoDto;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.mapper.video.VideoMapper;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.search.SearchFilters;
import com.wetube.wetube_service.search.SearchIndexes;
import com.wetube.wetube_service.search.VideoDocument;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;

class VideoSearchServiceTest {

    private static final IndexCoordinates V2 = IndexCoordinates.of("videos_v2");

    private final ElasticsearchOperations ops = mock(ElasticsearchOperations.class);
    private final VideoRepository videoRepository = mock(VideoRepository.class);
    private final VideoMapper videoMapper = new com.wetube.wetube_service.mapper.video.VideoMapperImpl();
    private final SearchIndexes indexes = mock(SearchIndexes.class);
    private final VideoSearchService service = new VideoSearchService(ops, videoRepository, videoMapper, indexes);

    private static String json(co.elastic.clients.elasticsearch._types.query_dsl.Query query) {
        return query.toString();
    }

    @Test
    void strictTierNeedsEveryWordAndOnlyFuzzesLongWords() {
        String strict = json(VideoSearchService.strictQuery("thien than"));
        assertThat(strict).contains("\"operator\":\"and\"").contains("AUTO:5,8").contains("searchKeywords");
        assertThat(strict).contains("80%");
    }

    @Test
    void looseTierNeedsAllWordsOfShortQueriesAnd75PercentOfLongOnes() {
        String loose = json(VideoSearchService.looseQuery("thien than"));
        assertThat(loose).contains("2<75%").contains("description").contains("60%");
    }

    @Test
    void relevanceKeepsRealScoresInsideEachTier() {
        BoolQuery.Builder b = new BoolQuery.Builder();
        VideoSearchService.tiered(b, "putin", true);
        String q = b.build().toString();
        assertThat(q).contains("constant_score").contains("1000.0").doesNotContain("\"boost\":1.0");
    }

    @Test
    void dateAndViewSortsUseTwoConstantScoresSoTheSecondSortWorksInsideEachTier() {
        BoolQuery.Builder b = new BoolQuery.Builder();
        VideoSearchService.tiered(b, "putin", false);
        String q = b.build().toString();
        assertThat(q).contains("1000.0").contains("\"boost\":1.0").contains("must_not");
    }

    @SuppressWarnings("unchecked")
    private static SearchHits<VideoDocument> hitsOf(UUID... ids) {
        SearchHits<VideoDocument> hits = mock(SearchHits.class);
        List<SearchHit<VideoDocument>> list = new ArrayList<>();
        for (UUID id : ids) {
            VideoDocument d = new VideoDocument();
            d.setId(id.toString());
            SearchHit<VideoDocument> hit = mock(SearchHit.class);
            when(hit.getContent()).thenReturn(d);
            list.add(hit);
        }
        when(hits.getSearchHits()).thenReturn(list);
        when(hits.getTotalHits()).thenReturn((long) list.size());
        return hits;
    }

    private static Video video(UUID id, ActiveStatus status) {
        Video v = new Video();
        v.setId(id);
        v.setTitle("t-" + id);
        v.setVideosStatus(status);
        return v;
    }

    @Test
    void resultsKeepElasticsearchOrderDropNonListedAndReadMysqlOnce() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID hidden = UUID.randomUUID();
        when(indexes.videos()).thenReturn(V2);
        SearchHits<VideoDocument> hits = hitsOf(b, hidden, a);
        when(ops.search(any(Query.class), eq(VideoDocument.class), eq(V2))).thenReturn(hits);
        // MySQL trả về thứ tự khác
        when(videoRepository.findAllById(anyIterable())).thenReturn(
                List.of(video(a, ActiveStatus.PUBLIC), video(hidden, ActiveStatus.INACTIVE), video(b, ActiveStatus.ACTIVE)));

        Page<VideoDto> page = service.searchByTitleFull("t", SearchFilters.of(null, null, null, null), 0, 10);

        assertThat(page.getContent()).extracting(VideoDto::getId).containsExactly(b, a);
        verify(videoRepository, times(1)).findAllById(anyIterable());
        verify(videoRepository, never()).findById(any());
    }

    @Test
    void similarVideosAreEmptyWhenThereIsNoMainResult() {
        when(indexes.videos()).thenReturn(V2);
        SearchHits<VideoDocument> none = hitsOf();
        when(ops.search(any(Query.class), eq(VideoDocument.class), eq(V2))).thenReturn(none);

        assertThat(service.similarVideos("zzz", SearchFilters.of(null, null, null, null), 10, 8)).isEmpty();
        verify(ops, times(1)).search(any(Query.class), eq(VideoDocument.class), eq(V2));
    }

    @Test
    void similarVideosExcludeTheMainResultsAndAreSeededByTheFirstThree() {
        UUID[] main = {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};
        UUID other = UUID.randomUUID();
        when(indexes.videos()).thenReturn(V2);
        SearchHits<VideoDocument> mainHits = hitsOf(main);
        SearchHits<VideoDocument> otherHits = hitsOf(other);
        when(ops.search(any(Query.class), eq(VideoDocument.class), eq(V2)))
                .thenReturn(mainHits)
                .thenReturn(otherHits);
        when(videoRepository.findAllById(anyIterable())).thenReturn(List.of(video(other, ActiveStatus.PUBLIC)));

        List<VideoDto> similar = service.similarVideos("a", SearchFilters.of(null, null, null, null), 10, 8);

        assertThat(similar).extracting(VideoDto::getId).containsExactly(other);
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(ops, times(2)).search(captor.capture(), eq(VideoDocument.class), eq(V2));
        String mlt = ((NativeQuery) captor.getAllValues().get(1)).getQuery().toString();
        assertThat(mlt).contains("more_like_this").contains("must_not")
                .contains(main[0].toString()).contains(main[1].toString()).contains(main[2].toString());
        // Chỉ 3 video đầu làm mẫu, nhưng cả 4 video chính đều bị loại
        String likePart = mlt.substring(mlt.indexOf("\"like\""), mlt.indexOf("\"min_term_freq\""));
        assertThat(likePart).doesNotContain(main[3].toString());
        assertThat(mlt.substring(mlt.indexOf("must_not"))).contains(main[3].toString());
    }
}
