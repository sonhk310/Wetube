package com.wetube.wetube_service.service.channel;

import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.bool;
import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.match;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import com.wetube.wetube_service.dto.response.ChannelSearchDto;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.repository.channel.ChannelRepository;
import com.wetube.wetube_service.search.ChannelDocument;
import com.wetube.wetube_service.search.SearchIndexes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tìm kênh theo tên cho trang tìm kiếm; bỏ kênh bị ẩn / đã xoá. Dùng chỉ mục "channels" (bỏ dấu, chịu sai chính tả,
 * chữ viết dính); chưa có chỉ mục hoặc Elasticsearch lỗi thì tìm bằng MySQL.
 * strict = chỉ kênh khớp chặt (thẻ kênh ở đầu tab "Tất cả"); không strict = khớp rộng (tab "Kênh").
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelSearchService {
    public static final int MAX_LIMIT = 20;

    private final ChannelRepository channelRepository;
    private final ElasticsearchOperations operations;
    private final SearchIndexes indexes;

    @Transactional(readOnly = true)
    public List<ChannelSearchDto> search(String query, int limit, boolean strict) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) {
            return List.of();
        }
        int size = Math.max(1, Math.min(limit, MAX_LIMIT));
        List<Channel> channels = null;
        if (indexes.channelsReady()) {
            try {
                channels = searchElastic(q, size, strict);
            } catch (Exception e) {
                log.warn("Tìm kênh bằng Elasticsearch lỗi, dùng MySQL: {}", e.getMessage());
            }
        }
        if (channels == null) {
            channels = channelRepository.findTop20ByNameContainingIgnoreCaseOrderByTotalSubscribersDesc(q);
        }
        return channels.stream()
                .filter(c -> c.getStatus() != ActiveStatus.INACTIVE && c.getStatus() != ActiveStatus.DELETED)
                .limit(size)
                .map(c -> new ChannelSearchDto(c.getId(), c.getName(), c.getPicture(), c.getDescription(),
                        c.getTotalSubscribers(), c.getTotalVideos(), c.getHandle()))
                .toList();
    }

    private List<Channel> searchElastic(String q, int size, boolean strict) {
        NativeQuery query = new NativeQueryBuilder()
                .withQuery(bool(b -> b
                        .should(match(m -> m.field("name").query(q).fuzziness("AUTO")
                                .operator(strict ? Operator.And : Operator.Or).boost(2f)))
                        .should(match(m -> m.field("name.compact").query(q)
                                .minimumShouldMatch(strict ? "80%" : "60%")))
                        .minimumShouldMatch("1")))
                .withSort(Sort.by(Sort.Order.desc("_score"), Sort.Order.desc("totalSubscribers")))
                .withMaxResults(size)
                .build();
        List<UUID> ids = operations.search(query, ChannelDocument.class, SearchIndexes.CHANNELS)
                .getSearchHits().stream()
                .map(h -> UUID.fromString(h.getContent().getId()))
                .toList();
        // Dữ liệu hiển thị (số người đăng ký, ảnh) lấy từ MySQL, giữ thứ tự của Elasticsearch
        Map<UUID, Channel> byId = StreamSupport.stream(channelRepository.findAllById(ids).spliterator(), false)
                .collect(Collectors.toMap(Channel::getId, Function.identity()));
        return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
    }
}
