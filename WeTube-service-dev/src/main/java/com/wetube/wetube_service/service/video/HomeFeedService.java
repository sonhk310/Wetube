package com.wetube.wetube_service.service.video;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wetube.wetube_service.dto.video.RecommendVideoDto;
import com.wetube.wetube_service.entity.video.UserTopic;
import com.wetube.wetube_service.enumeration.LiveStatus;
import com.wetube.wetube_service.enumeration.PlaylistType;
import com.wetube.wetube_service.mapper.video.VideoMapper;
import com.wetube.wetube_service.repository.PlaylistVideoRepository;
import com.wetube.wetube_service.repository.channel.SubscriptionRepository;
import com.wetube.wetube_service.repository.live.LiveStreamRepository;
import com.wetube.wetube_service.repository.video.UserTopicRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.security.VideoAccess;

import lombok.RequiredArgsConstructor;

/**
 * Các chip ở trang chủ (Kênh đăng ký, Mới tải lên, Chưa xem, Đã xem, rồi chủ đề C8) và video của từng chip.
 * Chip không có video thì không được trả về (ẩn). "Tất cả" do client tự xử lý bằng API gợi ý.
 */
@Service
@RequiredArgsConstructor
public class HomeFeedService {

    public enum Kind { SUBSCRIPTIONS, NEW, UNWATCHED, WATCHED }

    public static final String TOPIC_PREFIX = "topic:";

    /** Id của chip Trực tiếp (không thuộc {@link Kind}). */
    public static final String LIVE_CHIP = "LIVE";

    /** id: tên một {@link Kind} hoặc "topic:{id chủ đề}". */
    public record Chip(String id, String label) {
    }

    static final int NEW_DAYS = 7;
    static final int MAX_TOPIC_CHIPS = 8;

    private final RecService recService;
    private final VideoRepository videoRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlaylistVideoRepository playlistVideoRepository;
    private final UserTopicRepository userTopicRepository;
    private final TopicCatalog topicCatalog;
    private final VideoMapper videoMapper;
    private final LiveStreamRepository liveStreamRepository;

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
        // Chip riêng: lưới của nó là các buổi phát (client tự lấy từ /api/live), không đi qua feed(...)
        if (liveStreamRepository.existsByStatus(LiveStatus.LIVE)) {
            chips.add(new Chip(LIVE_CHIP, "Trực tiếp"));
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
        for (String topicId : topicChips(userId)) {
            chips.add(new Chip(TOPIC_PREFIX + topicId, topicCatalog.label(topicId)));
        }
        return chips;
    }

    /** Video của một chủ đề (nhóm thì gồm cả chủ đề con), mới nhất trước, không có video của chính mình. */
    @Transactional(readOnly = true)
    public Page<RecommendVideoDto> topicFeed(UUID userId, String topicId, Pageable pageable) {
        return videoRepository.findListedByTopic(VideoAccess.LISTED, topicId, topicId + ".%", userId, pageable)
                .map(videoMapper::toRecommendDto);
    }

    /**
     * Chủ đề người xem có điểm cao trước, rồi chủ đề nhiều video nhất; tối đa MAX_TOPIC_CHIPS, chỉ chủ đề còn video
     * (không tính video của chính mình). Nhóm mà mọi video đều thuộc một chủ đề con thì bỏ: chip nhóm và chip con
     * sẽ ra đúng cùng các video.
     */
    private List<String> topicChips(UUID userId) {
        Map<String, Long> direct = new HashMap<>();
        for (Object[] row : videoRepository.countListedByTopic(VideoAccess.LISTED, userId)) {
            direct.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        Map<String, Long> counts = new HashMap<>();
        direct.forEach((id, n) -> topicCatalog.withGroup(id).forEach(key -> counts.merge(key, n, Long::sum)));
        for (TopicCatalog.Group g : topicCatalog.groups()) {
            long children = direct.keySet().stream().filter(id -> id.startsWith(g.id() + ".")).count();
            if (!direct.containsKey(g.id()) && children == 1) {
                counts.remove(g.id());
            }
        }

        Set<String> chosen = new LinkedHashSet<>();
        for (UserTopic affinity : userTopicRepository.findAllByUserIdOrderByPointDesc(userId)) {
            if (chosen.size() >= MAX_TOPIC_CHIPS) {
                break;
            }
            if (counts.containsKey(affinity.getTopicId())) {
                chosen.add(affinity.getTopicId());
            }
        }
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .forEach(id -> {
                    if (chosen.size() < MAX_TOPIC_CHIPS) {
                        chosen.add(id);
                    }
                });
        return new ArrayList<>(chosen);
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
