package com.wetube.wetube_service.service.video.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.wetube.wetube_service.dto.request.CreatePlaylistRequest;
import com.wetube.wetube_service.entity.playlist.Playlist;
import com.wetube.wetube_service.entity.playlist.PlaylistVideo;
import com.wetube.wetube_service.enumeration.PlaylistType;
import com.wetube.wetube_service.repository.PlaylistRepository;
import com.wetube.wetube_service.repository.PlaylistVideoRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.service.PlaylistService;
import com.wetube.wetube_service.service.RedisService;
import com.wetube.wetube_service.service.video.WatchProgressService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WatchProgressServiceImpl implements WatchProgressService {

    /** Vị trí mới nhất nằm trong Redis; MySQL chỉ ghi tối đa mỗi khoảng này một lần, trừ khi flush. */
    private static final Duration DB_WRITE_INTERVAL = Duration.ofMinutes(1);
    private static final Duration REDIS_TTL = Duration.ofDays(2);

    private final RedisService redisService;
    private final PlaylistRepository playlistRepo;
    private final PlaylistVideoRepository playlistVideoRepo;
    private final VideoRepository videoRepo;
    private final PlaylistService playlistService;

    @Override
    public void record(UUID userId, UUID videoId, float seconds, boolean flush) {
        if (playlistService.isHistoryPaused(userId)) {
            return; // tạm dừng lưu lịch sử: không ghi lịch sử lẫn vị trí đang xem
        }
        float position = Math.max(0f, seconds);
        redisService.set(WatchProgressService.redisKey(userId, videoId), Float.toString(position), REDIS_TTL);

        // null: Redis không dùng được -> ghi MySQL luôn
        Boolean due = redisService.setIfAbsent("wpdb:" + userId + ":" + videoId, DB_WRITE_INTERVAL);
        if (flush || due == null || due) {
            saveToHistory(userId, videoId, position);
        }
    }

    @Override
    public Float get(UUID userId, UUID videoId) {
        Float cached = parse(redisService.get(WatchProgressService.redisKey(userId, videoId)));
        if (cached != null) {
            return cached > 0 ? cached : null;
        }
        return findHistory(userId)
                .flatMap(h -> playlistVideoRepo.findByPlaylist_IdAndVideo_Id(h.getId(), videoId))
                .map(PlaylistVideo::getHistoryDuration)
                .filter(d -> d > 0)
                .orElse(null);
    }

    @Override
    public Map<UUID, Float> getMany(UUID userId, List<UUID> videoIds) {
        Map<UUID, Float> result = new HashMap<>();
        if (videoIds.isEmpty()) {
            return result;
        }
        findHistory(userId).ifPresent(h -> playlistVideoRepo.findByPlaylist_IdAndVideo_IdIn(h.getId(), videoIds)
                .forEach(pv -> {
                    if (pv.getHistoryDuration() > 0) {
                        result.put(pv.getVideo().getId(), pv.getHistoryDuration());
                    }
                }));
        // Redis có vị trí mới hơn MySQL (MySQL chỉ ghi mỗi phút)
        List<String> cached = redisService.multiGet(
                videoIds.stream().map(id -> WatchProgressService.redisKey(userId, id)).toList());
        if (cached != null) {
            for (int i = 0; i < videoIds.size(); i++) {
                Float value = parse(cached.get(i));
                if (value != null && value > 0) {
                    result.put(videoIds.get(i), value);
                }
            }
        }
        return result;
    }

    private void saveToHistory(UUID userId, UUID videoId, float position) {
        Playlist history = findHistory(userId).orElseGet(() -> {
            // user cũ có thể chưa có playlist lịch sử: tạo bù
            playlistService.createPlaylist(new CreatePlaylistRequest("History", userId, PlaylistType.HISTORY, "public"));
            return findHistory(userId).orElseThrow();
        });
        PlaylistVideo pv = playlistVideoRepo.findByPlaylist_IdAndVideo_Id(history.getId(), videoId)
                .orElseGet(() -> PlaylistVideo.builder()
                        .playlist(history)
                        .video(videoRepo.getReferenceById(videoId))
                        .build());
        pv.setHistoryDuration(position);
        pv.setUpdatedAt(LocalDateTime.now()); // đưa lên đầu lịch sử
        try {
            playlistVideoRepo.save(pv);
        } catch (DataIntegrityViolationException e) {
            // Hai request cùng lúc cùng tạo dòng mới: cập nhật dòng đã có
            playlistVideoRepo.findByPlaylist_IdAndVideo_Id(history.getId(), videoId).ifPresent(existing -> {
                existing.setHistoryDuration(position);
                existing.setUpdatedAt(LocalDateTime.now());
                playlistVideoRepo.save(existing);
            });
        }
    }

    private Optional<Playlist> findHistory(UUID userId) {
        return playlistRepo.findByUser_IdAndPlaylistType(userId, PlaylistType.HISTORY).stream().findFirst();
    }

    private static Float parse(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Float.parseFloat(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
