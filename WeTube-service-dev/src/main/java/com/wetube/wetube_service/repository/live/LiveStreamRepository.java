package com.wetube.wetube_service.repository.live;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.wetube.wetube_service.entity.live.LiveStream;
import com.wetube.wetube_service.enumeration.LiveStatus;

public interface LiveStreamRepository extends JpaRepository<LiveStream, UUID> {

    /** Buổi chưa kết thúc của kênh (mỗi kênh tối đa một). */
    Optional<LiveStream> findFirstByChannelIdAndStatusIn(UUID channelId, Collection<LiveStatus> statuses);

    List<LiveStream> findByStatusIn(Collection<LiveStatus> statuses);

    /** Trang /live: nhiều người xem trước. */
    List<LiveStream> findByStatusOrderByCurrentViewersDesc(LiveStatus status);

    boolean existsByStatus(LiveStatus status);

    /** Lịch sử buổi phát của kênh, mới nhất trước. */
    Page<LiveStream> findByChannelIdAndStatusOrderByEndedAtDesc(UUID channelId, LiveStatus status, Pageable pageable);
}
