package com.wetube.wetube_service.repository.video;


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

    Page<Video> findByUserIdAndVideosStatusIn(UUID userId, Collection<ActiveStatus> statuses, Pageable pageable);

    Page<Video> findByUserIdAndVideosStatusNot(UUID userId, ActiveStatus status, Pageable pageable);

    List<Video> findByVideosStatusIn(Collection<ActiveStatus> statuses);

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

    /** Shorts: video được đánh dấu isShort, hoặc video cũ dài không quá maxSeconds. */
    @Query("""
                SELECT v FROM Video v
                WHERE v.videosStatus = :status
                  AND (v.isShort = true OR (v.duration > 0 AND v.duration <= :maxSeconds))
            """)
    Page<Video> findShorts(@Param("status") ActiveStatus status,
                           @Param("maxSeconds") float maxSeconds,
                           Pageable pageable);

}
