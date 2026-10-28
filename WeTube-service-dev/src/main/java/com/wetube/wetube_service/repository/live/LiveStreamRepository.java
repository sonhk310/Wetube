package com.wetube.wetube_service.repository.live;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wetube.wetube_service.entity.live.LiveStream;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.enumeration.LiveStatus;
import com.wetube.wetube_service.enumeration.RecordingStatus;

public interface LiveStreamRepository extends JpaRepository<LiveStream, UUID> {

    /** Buổi chưa kết thúc của kênh (mỗi kênh tối đa một). */
    Optional<LiveStream> findFirstByChannelIdAndStatusIn(UUID channelId, Collection<LiveStatus> statuses);

    List<LiveStream> findByStatusIn(Collection<LiveStatus> statuses);

    /** Trang /live: nhiều người xem trước. */
    List<LiveStream> findByStatusOrderByCurrentViewersDesc(LiveStatus status);

    boolean existsByStatus(LiveStatus status);

    /** Buổi đang ghi hoặc đang chờ trình duyệt tải bản ghi lên (kể cả buổi đã kết thúc). */
    List<LiveStream> findByRecordingStatusIn(Collection<RecordingStatus> statuses);

    /**
     * Số giây đã ghi của một kênh trong khoảng [from, to) (tính theo lúc bắt đầu ghi). Chưa gồm buổi đang ghi: số của
     * buổi đó chỉ chốt khi dừng, phần đang chạy do dịch vụ cộng theo đồng hồ.
     */
    @Query("select coalesce(sum(s.recordingSeconds), 0) from LiveStream s "
            + "where s.channelId = :channelId and s.recordingStartedAt >= :from and s.recordingStartedAt < :to")
    long sumRecordingSecondsByChannel(@Param("channelId") UUID channelId, @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    /** Như trên cho cả WeTube. */
    @Query("select coalesce(sum(s.recordingSeconds), 0) from LiveStream s "
            + "where s.recordingStartedAt >= :from and s.recordingStartedAt < :to")
    long sumRecordingSeconds(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Mọi buổi đã kết thúc của kênh, mới nhất trước (Studio quản lý). */
    Page<LiveStream> findByChannelIdAndStatusOrderByEndedAtDesc(UUID channelId, LiveStatus status, Pageable pageable);

    /**
     * Tab Trực tiếp công khai: chỉ buổi đã kết thúc mà video xem lại còn và đang công khai (buổi không có video, hoặc
     * video đã bị xoá / ẩn, không hiện), mới nhất trước.
     */
    @Query("select s from LiveStream s where s.channelId = :channelId and s.status = :status "
            + "and s.replayVideoId in (select v.id from Video v where v.videosStatus in :visible) "
            + "order by s.endedAt desc")
    Page<LiveStream> findPublicReplays(@Param("channelId") UUID channelId, @Param("status") LiveStatus status,
                                       @Param("visible") Collection<ActiveStatus> visible, Pageable pageable);
}
