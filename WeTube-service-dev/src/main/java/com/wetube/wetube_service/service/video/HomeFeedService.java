package com.wetube.wetube_service.service.video;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wetube.wetube_service.dto.video.RecommendVideoDto;
import com.wetube.wetube_service.enumeration.PlaylistType;
import com.wetube.wetube_service.mapper.video.VideoMapper;
import com.wetube.wetube_service.repository.PlaylistVideoRepository;
import com.wetube.wetube_service.repository.channel.SubscriptionRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.security.VideoAccess;

import lombok.RequiredArgsConstructor;

/**
 * Các chip ở trang chủ (Kênh đăng ký, Mới tải lên, Chưa xem, Đã xem) và video của từng chip.
 * Chip không có video thì không được trả về (ẩn). "Tất cả" do client tự xử lý bằng API gợi ý.
 */
@Service
@RequiredArgsConstructor
public class HomeFeedService {

    public enum Kind { SUBSCRIPTIONS, NEW, UNWATCHED, WATCHED }

    /** id: tên một {@link Kind}. */
    public record Chip(String id, String label) {
    }

    static final int NEW_DAYS = 7;

    private final RecService recService;
    private final VideoRepository videoRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlaylistVideoRepository playlistVideoRepository;
    private final VideoMapper videoMapper;

    /** Chưa có lịch sử xem thì trang chủ trống và không có hàng chip (giống YouTube). */
    @Transactional(readOnly = true)
    public List<Chip> chips(UUID userId, String seed) {
        if (!playlistVideoRepository.existsByPlaylist_User_IdAndPlaylist_PlaylistType(userId, PlaylistType.HISTORY)) {
            return List.of();
        }
        Pageable one = PageRequest.of(0, 1);
        List<Chip> chips = new ArrayList<>();
        if (hasSubscriptionVideos(userId)) {
            chips.add(new Chip(Kind.SUBSCRIPTIONS.name(), "Kênh đăng ký"));
        }
        if (recentVideos(userId, one).hasContent()) {
            chips.add(new Chip(Kind.NEW.name(), "Mới tải lên"));
        }
        if (recService.hasUnwatched(userId, seed)) {
            chips.add(new Chip(Kind.UNWATCHED.name(), "Chưa xem"));
        }
        if (watchedVideos(userId, one).hasContent()) {
            chips.add(new Chip(Kind.WATCHED.name(), "Đã xem"));
        }
        return chips;
    }

    @Transactional(readOnly = true)
    public Page<RecommendVideoDto> feed(UUID userId, Kind kind, Pageable pageable, String seed) {
        return switch (kind) {
            case SUBSCRIPTIONS -> subscriptionVideos(userId, pageable);
            case NEW -> recentVideos(userId, pageable).map(videoMapper::toRecommendDto);
            case UNWATCHED -> recService.unwatchedVideos(userId, pageable, seed);
            case WATCHED -> watchedVideos(userId, pageable).map(pv -> videoMapper.toRecommendDto(pv.getVideo()));
        };
    }

    private boolean hasSubscriptionVideos(UUID userId) {
        List<UUID> channelIds = subscriptionRepository.findSubscribedChannelIds(userId);
        return !channelIds.isEmpty()
                && videoRepository.findFeed(channelIds, VideoAccess.LISTED, PageRequest.of(0, 1)).hasContent();
    }

    private Page<RecommendVideoDto> subscriptionVideos(UUID userId, Pageable pageable) {
        List<UUID> channelIds = subscriptionRepository.findSubscribedChannelIds(userId);
        if (channelIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return videoRepository.findFeed(channelIds, VideoAccess.LISTED, pageable).map(videoMapper::toRecommendDto);
    }

    /** Video công khai đăng trong NEW_DAYS ngày gần đây, mới nhất trước, không tính video của chính mình. */
    private Page<com.wetube.wetube_service.entity.video.Video> recentVideos(UUID userId, Pageable pageable) {
        return videoRepository.findRecentListed(VideoAccess.LISTED, LocalDateTime.now().minusDays(NEW_DAYS), userId, pageable);
    }

    /** Video trong lịch sử xem (còn công khai), xem gần nhất trước. */
    private Page<com.wetube.wetube_service.entity.playlist.PlaylistVideo> watchedVideos(UUID userId, Pageable pageable) {
        Pageable byRecent = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Order.desc("updatedAt")));
        return playlistVideoRepository.findByPlaylist_User_IdAndPlaylist_PlaylistTypeAndVideo_VideosStatusIn(
                userId, PlaylistType.HISTORY, VideoAccess.LISTED, byRecent);
    }
}
