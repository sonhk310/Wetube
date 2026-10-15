package com.wetube.wetube_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wetube.wetube_service.entity.playlist.Playlist;
import com.wetube.wetube_service.enumeration.PlaylistType;

@Repository
public interface PlaylistRepository extends CrudRepository<Playlist, UUID>{
     List<Playlist> findByUser_Id(UUID userId);
     List<Playlist> findByUser_IdAndPlaylistType(UUID userId, PlaylistType playlistType);
     Optional<Playlist> findByUser_IdAndTitle(UUID userId, String title);

     @Query("""
       SELECT v.videoUrl
       FROM Playlist p
       JOIN p.playlistVideos pv
       JOIN pv.video v
       WHERE p.user.id = :userId
         AND p.playlistType = com.wetube.wetube_service.enumeration.PlaylistType.USER_UPLOADED
       ORDER BY v.totalView DESC
       """)
     List<String> findVideoUrlsByUserUploaded(@Param("userId") UUID userId, Pageable pageable);

     Optional<Playlist> findByUserIdAndPlaylistType(UUID userId, PlaylistType playlistType);
}
