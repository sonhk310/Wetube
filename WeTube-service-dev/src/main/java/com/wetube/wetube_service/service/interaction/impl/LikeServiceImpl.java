package com.wetube.wetube_service.service.interaction.impl;

import com.wetube.wetube_service.dto.LikeDto;
import com.wetube.wetube_service.entity.interaction.Like;
import com.wetube.wetube_service.entity.interaction.Like.TargetType;
import com.wetube.wetube_service.entity.interaction.VideoDislike;
import com.wetube.wetube_service.entity.video.UserTag;
import com.wetube.wetube_service.entity.video.VideoTag;
import com.wetube.wetube_service.enumeration.PlaylistType;
import com.wetube.wetube_service.repository.interaction.VideoDislikeRepository;
import com.wetube.wetube_service.repository.video.UserTagRepository;
import com.wetube.wetube_service.repository.video.VideoTagRepository;
import com.wetube.wetube_service.repository.interaction.LikeRepository;
import com.wetube.wetube_service.service.PlaylistService;
import com.wetube.wetube_service.service.interaction.LikeService;
import com.wetube.wetube_service.service.video.RecService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LikeServiceImpl implements LikeService {

    private final LikeRepository likeRepository;
    private final PlaylistService playlistService;
    private final VideoDislikeRepository dislikeRepository;
    private final VideoTagRepository videoTagRepository;
    private final UserTagRepository userTagRepository;
    private final RecService recService;

    /** Số điểm yêu thích tag bị trừ mỗi khi không thích một video (bằng điểm cộng khi thích). */
    private static final double DISLIKE_PENALTY = 5.0;

    @Override
    public LikeDto getLikeInfo(UUID targetId, Like.TargetType targetType, UUID userId) {
        int likeCount = likeRepository.countByTargetTypeAndTargetId(targetType, targetId);
        List<UUID> likedUserIds = likeRepository.findByTargetTypeAndTargetId(targetType, targetId)
                .stream()
                .map(Like::getUserId)
                .collect(Collectors.toList());

        Boolean liked = null;
        if (userId != null) {
            liked = likeRepository.existsByTargetTypeAndTargetIdAndUserId(targetType, targetId, userId);
        }

        Boolean disliked = null;
        if (userId != null && targetType == TargetType.VIDEO) {
            disliked = dislikeRepository.existsByUserIdAndVideoId(userId, targetId);
        }

        return LikeDto.builder()
                .targetId(targetId)
                .targetType(targetType)
                .likeCount(likeCount)
                .likedUserIds(likedUserIds)
                .liked(liked)
                .disliked(disliked)
                .build();
    }

    @Override
    public void toggleLike(UUID targetId, Like.TargetType targetType, UUID userId) {
        boolean alreadyLiked = likeRepository.existsByTargetTypeAndTargetIdAndUserId(targetType, targetId, userId);

        if (alreadyLiked) {
            likeRepository.deleteAll(
                    likeRepository.findByTargetTypeAndTargetId(targetType, targetId)
                            .stream()
                            .filter(like -> like.getUserId().equals(userId))
                            .toList()
            );
        } else {
            Like like = Like.builder()
                    .targetId(targetId)
                    .targetType(targetType)
                    .userId(userId)
                    .build();
            likeRepository.save(like);
            if (targetType == TargetType.VIDEO) {
                dislikeRepository.deleteByUserIdAndVideoId(userId, targetId);
            }
        }

        // Thích video thì video vào playlist "Liked" của user; bỏ thích thì bị gỡ ra.
        if (targetType == TargetType.VIDEO) {
            if (alreadyLiked) {
                playlistService.removeVideoFromSystemPlaylist(userId, targetId, PlaylistType.LIKED);
            } else {
                playlistService.addVideoToSystemPlaylist(userId, targetId, PlaylistType.LIKED);
            }
        }
    }

    @Override
    public LikeDto toggleDislike(UUID videoId, UUID userId) {
        if (dislikeRepository.existsByUserIdAndVideoId(userId, videoId)) {
            dislikeRepository.deleteByUserIdAndVideoId(userId, videoId);
        } else {
            // Không thích thì bỏ thích (kể cả khỏi playlist "Liked")
            if (likeRepository.existsByTargetTypeAndTargetIdAndUserId(TargetType.VIDEO, videoId, userId)) {
                toggleLike(videoId, TargetType.VIDEO, userId);
            }
            dislikeRepository.save(VideoDislike.builder().userId(userId).videoId(videoId).build());
            lowerTagAffinity(userId, videoId);
            recService.evictRecommendations(userId);
        }
        return getLikeInfo(videoId, TargetType.VIDEO, userId);
    }

    /** Không thích video thì giảm điểm yêu thích của người dùng với các tag của video đó (không xuống dưới 0). */
    private void lowerTagAffinity(UUID userId, UUID videoId) {
        for (VideoTag vt : videoTagRepository.findByVideo_Id(videoId)) {
            var pk = new UserTag.UserTagId(userId, vt.getTag().getId());
            userTagRepository.findById(pk).ifPresent(affinity -> {
                affinity.setPoint(Math.max(0, affinity.getPoint() - DISLIKE_PENALTY));
                userTagRepository.save(affinity);
            });
        }
    }
}
