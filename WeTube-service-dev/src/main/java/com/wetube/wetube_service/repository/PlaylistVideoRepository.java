package com.wetube.wetube_service.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.wetube.wetube_service.entity.playlist.PlaylistVideo;

@Repository
public interface PlaylistVideoRepository extends JpaRepository<PlaylistVideo, UUID>{
    Optional<PlaylistVideo> findByPlaylist_IdAndVideo_Id(UUID playlistId, UUID videoId);

    List<PlaylistVideo> findByPlaylist_IdAndVideo_IdIn(UUID playlistId, Collection<UUID> videoIds);

    @Query("SELECT pv FROM PlaylistVideo pv WHERE pv.playlist.id = :playlistId ORDER BY pv.updatedAt DESC")
    List<PlaylistVideo> findByPlaylist_IdOrderByUpdatedAtDesc(UUID playlistId);

    @Modifying
    @Query("DELETE FROM PlaylistVideo pv WHERE pv.video.id = :videoId")
    int deleteByVideoId(UUID videoId);

    /** Id các playlist của user có chứa video này (trừ playlist tự sinh USER_UPLOADED). */
    @Query("""
            SELECT pv.playlist.id FROM PlaylistVideo pv
            WHERE pv.video.id = :videoId AND pv.playlist.user.id = :userId
              AND pv.playlist.playlistType <> com.wetube.wetube_service.enumeration.PlaylistType.USER_UPLOADED
            """)
    List<UUID> findPlaylistIdsContaining(UUID userId, UUID videoId);
}
