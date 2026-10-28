package com.wetube.wetube_service.repository.video;


import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.enumeration.ActiveStatus;

public interface VideoRepository extends JpaRepository<Video, UUID> {

    /** Video đã tạo từ một link (bản ghi buổi phát: tạo lại cùng link thì không nhân đôi video). */
    Optional<Video> findFirstByVideoUrl(String videoUrl);

    /** Bảng tin Đăng ký: video công khai mới nhất của các kênh. */
    @Query("""
            SELECT v FROM Video v
            WHERE v.user.channel.id IN :channelIds AND v.videosStatus IN :statuses
            ORDER BY v.createdAt DESC""")
    Page<Video> findFeed(@Param("channelIds") Collection<UUID> channelIds,
                         @Param("statuses") Collection<ActiveStatus> statuses,
                         Pageable pageable);
    /** Video đang được liệt kê, đăng từ `since`, mới nhất trước, trừ video của `userId` (chip "Mới tải lên"). */
    @Query("""
            SELECT v FROM Video v
            WHERE v.videosStatus IN :statuses AND v.createdAt >= :since AND v.user.id <> :userId
            ORDER BY v.createdAt DESC""")
    Page<Video> findRecentListed(@Param("statuses") Collection<ActiveStatus> statuses,
                                 @Param("since") java.time.LocalDateTime since,
                                 @Param("userId") UUID userId,
                                 Pageable pageable);
    /** Số video đang được liệt kê theo từng chủ đề (C8), trừ video của `userId`: [topicId, số video]. */
    @Query("""
            SELECT v.topicId, COUNT(v) FROM Video v
            WHERE v.videosStatus IN :statuses AND v.topicId IS NOT NULL AND v.user.id <> :userId
            GROUP BY v.topicId""")
    List<Object[]> countListedByTopic(@Param("statuses") Collection<ActiveStatus> statuses,
                                      @Param("userId") UUID userId);
    /** Video đang được liệt kê có chủ đề `topicId` hoặc chủ đề con của nó (`childPrefix` = "nhóm.%"), mới nhất trước. */
    @Query("""
            SELECT v FROM Video v
            WHERE v.videosStatus IN :statuses AND (v.topicId = :topicId OR v.topicId LIKE :childPrefix)
              AND v.user.id <> :userId
            ORDER BY v.createdAt DESC""")
    Page<Video> findListedByTopic(@Param("statuses") Collection<ActiveStatus> statuses,
                                  @Param("topicId") String topicId,
                                  @Param("childPrefix") String childPrefix,
                                  @Param("userId") UUID userId,
                                  Pageable pageable);
    @Query("SELECT v.topicId FROM Video v WHERE v.id = :id")
    String findTopicIdById(@Param("id") UUID id);

    Video findByIdAndUser_Id(UUID videoId, UUID userId);

    List<Video> findByVideoTags_Tag_NameOrderByTotalViewDesc(String tagName);

    @Query("""
                SELECT v FROM Video v
                LEFT JOIN FETCH v.videoTags vt
                LEFT JOIN FETCH vt.tag t
                WHERE v.id = :id
            """)
    Optional<Video> findByIdWithTags(@Param("id") UUID id);

    List<Video> findAllByVideosStatusOrderByCreatedAtDesc(ActiveStatus status);

    List<Video> findByVideosStatusAndUpdatedAtBefore(ActiveStatus status, LocalDateTime before);

    List<Video> findByTitleContainingIgnoreCase(String title);

    Page<Video> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Video> findAll(Pageable pageable);

    List<Video> findDistinctByVideoTags_Tag_NameInAndIdNot(List<String> tagNames, UUID excludeId);

    @Query("""
                SELECT DISTINCT v FROM Video v
                         LEFT JOIN v.videoTags vt
                         LEFT JOIN vt.tag t
                         WHERE LOWER(v.title) LIKE LOWER(:query)
                         OR LOWER(v.description) LIKE LOWER(:query)
                         OR LOWER(COALESCE(t.name, '')) LIKE LOWER(:query)                                                      OR LOWER(COALESCE(t.name, '')) LIKE LOWER(:query) 
            """)
    List<Video> findByQuery(@Param("query") String query);

    Page<Video> findByUserId(UUID userId, Pageable pageable);

    /** Video của một kênh theo trạng thái (shortFlags: true = Shorts, false = video thường). */
    @Query("""
                SELECT v FROM Video v
                WHERE v.user.id = :userId
                  AND v.videosStatus IN :statuses
                  AND v.isShort IN :shortFlags
            """)
    Page<Video> findChannelVideos(@Param("userId") UUID userId, @Param("statuses") Collection<ActiveStatus> statuses,
                                  @Param("shortFlags") Collection<Boolean> shortFlags, Pageable pageable);

    /** Như trên nhưng cho chủ kênh / admin: mọi trạng thái trừ {@code excluded}. */
    @Query("""
                SELECT v FROM Video v
                WHERE v.user.id = :userId
                  AND v.videosStatus <> :excluded
                  AND v.isShort IN :shortFlags
            """)
    Page<Video> findChannelVideosExcluding(@Param("userId") UUID userId, @Param("excluded") ActiveStatus excluded,
                                           @Param("shortFlags") Collection<Boolean> shortFlags, Pageable pageable);

    List<Video> findByVideosStatusIn(Collection<ActiveStatus> statuses);

    List<Video> findByUser_IdAndVideosStatusIn(UUID userId, Collection<ActiveStatus> statuses);

    Page<Video> findByVideosStatusIn(Collection<ActiveStatus> statuses, Pageable pageable);

    Page<Video> findDistinctByVideoTags_Tag_NameIgnoreCaseAndVideosStatusIn(String tagName,
                                                                            Collection<ActiveStatus> statuses,
                                                                            Pageable pageable);

    List<Video> findDistinctByVideoTags_Tag_NameInAndIdNotAndVideosStatusIn(List<String> tagNames, UUID excludeId,
                                                                            Collection<ActiveStatus> statuses);

    @Modifying
    @Query("UPDATE Video v SET v.totalView = v.totalView + 1 WHERE v.id = :id")
    int incrementTotalView(@Param("id") UUID id);

    @Query("SELECT v.totalView FROM Video v WHERE v.id = :id")
    Optional<Integer> findTotalViewById(@Param("id") UUID id);

    /** Shorts: video được đánh dấu isShort (xem ShortsRule). */
    @Query("""
                SELECT v FROM Video v
                WHERE v.videosStatus = :status
                  AND v.isShort = true
            """)
    Page<Video> findShorts(@Param("status") ActiveStatus status, Pageable pageable);

    /** Số video của kênh (cả Shorts) theo trạng thái; cột channels.total_videos không được cập nhật nên đếm khi đọc. */
    @Query("SELECT COUNT(v) FROM Video v WHERE v.user.channel.id = :channelId AND v.videosStatus IN :statuses")
    long countChannelVideos(@Param("channelId") UUID channelId, @Param("statuses") Collection<ActiveStatus> statuses);

    /** Tổng lượt xem các video của kênh theo trạng thái (thay cho cột channels.total_views không được cập nhật). */
    @Query("SELECT COALESCE(SUM(v.totalView), 0) FROM Video v WHERE v.user.channel.id = :channelId AND v.videosStatus IN :statuses")
    long sumChannelViews(@Param("channelId") UUID channelId, @Param("statuses") Collection<ActiveStatus> statuses);

}
