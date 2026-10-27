package com.wetube.wetube_service.service.video;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wetube.wetube_service.dto.video.RecommendVideoDto;
import com.wetube.wetube_service.entity.video.Tag;
import com.wetube.wetube_service.entity.video.UserTag;
import com.wetube.wetube_service.enumeration.PlaylistType;
import com.wetube.wetube_service.mapper.video.VideoMapper;
import com.wetube.wetube_service.repository.PlaylistVideoRepository;
import com.wetube.wetube_service.repository.channel.SubscriptionRepository;
import com.wetube.wetube_service.repository.video.TagRepository;
import com.wetube.wetube_service.repository.video.UserTagRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.security.VideoAccess;

import lombok.RequiredArgsConstructor;

/**
 * Các chip ở trang chủ (Kênh đăng ký, Mới tải lên, Chưa xem, Đã xem, chủ đề theo thẻ) và video của từng chip.
 * Chip không có video thì không được trả về (ẩn). "Tất cả" và chip chủ đề do client tự xử lý bằng API gợi ý / by-tag.
 */
@Service
@RequiredArgsConstructor
public class HomeFeedService {

    public enum Kind { SUBSCRIPTIONS, NEW, UNWATCHED, WATCHED }

    /** id: tên một {@link Kind} hoặc "tag:{tên thẻ}". */
    public record Chip(String id, String label) {
    }

    static final int NEW_DAYS = 7;
    static final int MAX_TOPIC_CHIPS = 8;
    private static final int TAG_CANDIDATES = 50;

    private final RecService recService;
    private final VideoRepository videoRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlaylistVideoRepository playlistVideoRepository;
    private final UserTagRepository userTagRepository;
    private final TagRepository tagRepository;
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
        for (String tag : topicTags(userId)) {
            // Nhãn là tên thẻ, không có dấu # (như chip chủ đề của YouTube)
            chips.add(new Chip("tag:" + tag, tag));
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

    /** Thẻ người xem hay xem trước (chỉ thẻ còn video công khai), rồi thẻ phổ biến; tối đa MAX_TOPIC_CHIPS. */
    private List<String> topicTags(UUID userId) {
        List<String> available = tagRepository
                .findPopularTagNames(VideoAccess.LISTED, PageRequest.of(0, TAG_CANDIDATES)).stream()
                .map(row -> String.valueOf(row[0])).toList();
        Set<String> availableSet = new LinkedHashSet<>(available);

        Set<String> chosen = new LinkedHashSet<>();
        for (UserTag affinity : userTagRepository.findAllByUserIdOrderByPointDesc(userId)) {
            if (chosen.size() >= MAX_TOPIC_CHIPS) {
                break;
            }
            tagRepository.findById(affinity.getTagId()).map(Tag::getName)
                    .filter(availableSet::contains).ifPresent(chosen::add);
        }
        for (String name : available) {
            if (chosen.size() >= MAX_TOPIC_CHIPS) {
                break;
            }
            chosen.add(name);
        }
        return new ArrayList<>(chosen);
    }
}
