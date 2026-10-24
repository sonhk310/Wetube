package com.wetube.wetube_service.service.channel;

import java.util.List;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.search.ChannelDocument;
import com.wetube.wetube_service.search.SearchIndexes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Đồng bộ kênh vào chỉ mục "channels" (lúc khởi động và khi tạo kênh). Lỗi chỉ log, không chặn việc đang làm. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelIndexer {

    private final ElasticsearchOperations operations;
    private final SearchIndexes indexes;

    public void index(Channel channel) {
        if (!indexes.channelsReady()) {
            return;
        }
        try {
            String id = channel.getId().toString();
            if (channel.getStatus() == ActiveStatus.INACTIVE || channel.getStatus() == ActiveStatus.DELETED) {
                operations.delete(id, SearchIndexes.CHANNELS);
            } else {
                operations.save(new ChannelDocument(id, channel.getName(), channel.getTotalSubscribers()),
                        SearchIndexes.CHANNELS);
            }
        } catch (Exception e) {
            log.warn("Không đồng bộ được kênh {} vào Elasticsearch: {}", channel.getId(), e.getMessage());
        }
    }

    public void reindexAll(List<Channel> channels) {
        channels.forEach(this::index);
    }
}
