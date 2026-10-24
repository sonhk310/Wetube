package com.wetube.wetube_service.service.channel;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wetube.wetube_service.dto.response.ChannelSearchDto;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.repository.channel.ChannelRepository;

import lombok.RequiredArgsConstructor;

/** Tìm kênh theo tên cho trang tìm kiếm; bỏ kênh bị ẩn / đã xoá. */
@Service
@RequiredArgsConstructor
public class ChannelSearchService {

    private final ChannelRepository channelRepository;

    @Transactional(readOnly = true)
    public List<ChannelSearchDto> search(String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) {
            return List.of();
        }
        return channelRepository.findTop20ByNameContainingIgnoreCaseOrderByTotalSubscribersDesc(q).stream()
                .filter(c -> c.getStatus() != ActiveStatus.INACTIVE && c.getStatus() != ActiveStatus.DELETED)
                .map(c -> new ChannelSearchDto(c.getId(), c.getName(), c.getPicture(), c.getDescription(),
                        c.getTotalSubscribers(), c.getTotalVideos()))
                .toList();
    }
}
