package com.wetube.wetube_service.service.video.impl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import com.wetube.wetube_service.dto.response.CustomPageResponse;
import com.wetube.wetube_service.dto.video.*;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.interaction.Comment;
import com.wetube.wetube_service.entity.interaction.Like;
import com.wetube.wetube_service.entity.playlist.Playlist;
import com.wetube.wetube_service.entity.playlist.PlaylistVideo;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.enumeration.PlaylistType;
import com.wetube.wetube_service.exception.ResourceNotFoundException;
import com.wetube.wetube_service.exception.UploadFailedException;
import com.wetube.wetube_service.repository.PlaylistRepository;
import com.wetube.wetube_service.repository.PlaylistVideoRepository;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.repository.channel.SubscriptionRepository;
import com.wetube.wetube_service.repository.interaction.CommentRepository;
import com.wetube.wetube_service.repository.interaction.LikeRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wetube.wetube_service.repository.video.TagRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.repository.video.VideoTagRepository;
import com.wetube.wetube_service.dto.LikeDto;
import com.wetube.wetube_service.dto.CommentDto.CommentResponseDto;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.entity.video.VideoTag;
import com.wetube.wetube_service.mapper.interaction.CommentMapper;
import com.wetube.wetube_service.mapper.video.VideoMapper;
import com.wetube.wetube_service.entity.video.Tag;
import com.wetube.wetube_service.repository.channel.CategoryVideoRepository;
import com.wetube.wetube_service.security.VideoAccess;
import com.wetube.wetube_service.service.VideoIndexer;
import com.wetube.wetube_service.service.CloudinaryService;
import com.wetube.wetube_service.service.RedisService;
import com.wetube.wetube_service.service.interaction.LikeService;
import com.wetube.wetube_service.service.video.VideoService;
import com.wetube.wetube_service.service.video.VideoSubtitleService;
import com.wetube.wetube_service.service.video.WatchProgressService;
import com.wetube.wetube_service.service.notification.NotificationService;

import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    /** Video dài không quá 60 giây được coi là Shorts. */
    private static final float SHORT_MAX_SECONDS = 60f;

    private final VideoRepository videoRepository;
    private final TagRepository tagRepository;
    private final VideoTagRepository videoTagRepository;
    private final VideoMapper videoMapper;
    private final CommentMapper commentMapper;
    private final CloudinaryService cloudinaryService;
    private final VideoIndexer videoIndexer;
    private final CategoryVideoRepository categoryVideoRepository;
    private final VideoSubtitleService videoSubtitleService;

    private final PlaylistRepository playlistRepository;
    private final PlaylistVideoRepository playlistVideoRepository;
    private final UserRepository userRepository;

    private final CommentRepository commentRepository;
    private final LikeService likeService;

    private final SubscriptionRepository subscriptionRepository;
    private final RedisService redisService;
    private final WatchProgressService watchProgressService;
    private final NotificationService notificationService;

    private static final String ID_NOT_FOUND = "Video not found with id: ";
    private static final String VIDEO = "video";

    /** Một người xem chỉ được tính thêm 1 lượt xem cho mỗi video trong khoảng này. */
    private static final long VIEW_DEDUPE_MILLIS = 30L * 60 * 1000;
    private static final int VIEW_CACHE_PURGE_THRESHOLD = 5000;
    private final java.util.concurrent.ConcurrentHashMap<String, Long> recentViews = new java.util.concurrent.ConcurrentHashMap<>();

    /** Chế độ hiển thị người dùng được chọn: công khai (ACTIVE/PUBLIC), không công khai, riêng tư. */
    private static ActiveStatus parseVisibility(String raw) {
        if (raw == null || raw.isBlank()) {
            return ActiveStatus.ACTIVE;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "ACTIVE", "PUBLIC" -> ActiveStatus.ACTIVE;
            case "PRIVATE" -> ActiveStatus.PRIVATE;
            case "UNLISTED" -> ActiveStatus.UNLISTED;
            default -> throw new IllegalArgumentException("Chế độ hiển thị không hợp lệ: " + raw);
        };
    }

@Override
@Transactional(rollbackFor = Exception.class)
public VideoDto createVideo(MultipartFile videoFile,
        MultipartFile thumbnailFile,
        VideoDto videoDto, UUID authenticatedUserId) throws Exception {

    if (videoFile == null || videoFile.isEmpty()) {
        throw new IllegalArgumentException("Video file is required");
    }
    ActiveStatus visibility = parseVisibility(videoDto.getVideosStatus());

    AppUser user = userRepository.findById(authenticatedUserId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + authenticatedUserId));

    String videoUrl;
    try {
        videoUrl = cloudinaryService.uploadVideo(videoFile);
    } catch (Exception e) {
        log.error("❌ Upload video lên Cloudinary thất bại: {}", e.getMessage(), e);
        throw new UploadFailedException("Upload video thất bại, vui lòng thử lại sau.", e);
    }

    String thumbnailUrl = null;
    if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
        try {
            thumbnailUrl = cloudinaryService.uploadThumbnail(thumbnailFile);
        } catch (Exception e) {
            log.warn("⚠️ Upload thumbnail thất bại: {}", e.getMessage());
            // tuỳ bạn: hoặc tiếp tục flow, hoặc throw new UploadFailedException(...)
        }
    }

    Video entity = videoMapper.toEntity(videoDto);
    entity.setUser(user);
    entity.setVideoUrl(videoUrl);
    entity.setThumbnailUrl(thumbnailUrl);
    entity.setVideosStatus(visibility);
    entity.setShort(entity.getDuration() > 0 && entity.getDuration() <= SHORT_MAX_SECONDS);

    LocalDateTime now = LocalDateTime.now();
    if (entity.getCreatedAt() == null) entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    try {
        Video savedVideo = videoRepository.save(entity);
        videoRepository.flush();

        addVideoToUserUploadedPlaylist(savedVideo);

        String tagsAsString = videoDto.getTagsAsString();
        VideoDto resultDto = (tagsAsString != null && !tagsAsString.isBlank())
                ? this.addTags(savedVideo.getId(), tagsAsString)
                : videoMapper.toDto(savedVideo);

        indexToElasticsearch(savedVideo);
        notifySubscribers(savedVideo);
        return resultDto;

    } catch (Exception e) {
        log.error("❌ Lỗi khi lưu video vào DB: {}", e.getMessage(), e);

        // rollback Cloudinary nếu DB save fail
        try {
            cloudinaryService.deleteVideo(videoUrl);
            log.info("Đã rollback, xoá video {} khỏi Cloudinary", videoUrl);
        } catch (Exception ex) {
            log.warn("⚠️ Không thể xoá video trên Cloudinary sau khi DB rollback: {}", ex.getMessage());
        }

        throw new UploadFailedException("Lưu video thất bại, vui lòng thử lại.", e);
    }
}

    private void indexToElasticsearch(Video video) {
        videoIndexer.index(video);
    }

    private void addVideoToUserUploadedPlaylist(Video video) {
        try {
            UUID userId = video.getUser().getId();

            Playlist uploadedPlaylist = playlistRepository
                    .findByUserIdAndPlaylistType(userId, PlaylistType.USER_UPLOADED)
                    .orElseGet(() -> {
                        log.info("USER_UPLOADED playlist not found for user {}. Creating new one.", userId);
                        AppUser user = userRepository.findById(userId)
                                .orElseThrow(
                                        () -> new IllegalArgumentException("User not found for creating playlist"));

                        Playlist newPlaylist = Playlist.builder()
                                .title("Uploaded videos")
                                .playlistType(PlaylistType.USER_UPLOADED)
                                .user(user)
                                .build();
                        return playlistRepository.save(newPlaylist);
                    });

            PlaylistVideo playlistVideoLink = PlaylistVideo.builder()
                    .playlist(uploadedPlaylist)
                    .video(video)
                    .build();

            playlistVideoRepository.save(playlistVideoLink);
            log.info("Successfully added video {} to USER_UPLOADED playlist for user {}", video.getId(), userId);

        } catch (Exception e) {
            log.error("Failed to add video {} to USER_UPLOADED playlist for user {}. Error: {}",
                    video.getId(), video.getUser().getId(), e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VideoDto addTags(UUID videoId, String hashtagText) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException(ID_NOT_FOUND + videoId));

        Set<String> names = parseHashtagText(hashtagText);

        for (String name : names) {
            Tag tag = tagRepository.findByNameIgnoreCase(name).orElse(null);
            if (tag == null) {
                tag = new Tag();
                tag.setName(name);
                tag.setCount(0);
                tag.setCreatedAt(LocalDateTime.now());
                tag = tagRepository.save(tag);
            }

            if (!videoTagRepository.existsByVideo_IdAndTag_Id(video.getId(), tag.getId())) {
                VideoTag vt = new VideoTag();
                vt.setVideo(video);
                vt.setTag(tag);
                videoTagRepository.save(vt);

                video.getVideoTags().add(vt);

                tag.setCount((tag.getCount() == null ? 0 : tag.getCount()) + 1);
                tagRepository.save(tag);
            }
        }

        Video dtoSource = videoRepository.findByIdWithTags(videoId)
                .orElseThrow(() -> new ResourceNotFoundException(ID_NOT_FOUND + videoId));

        // update index sau khi add tag
        indexToElasticsearch(dtoSource);

        return videoMapper.toDto(dtoSource);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomPageResponse<VideoDto> getVideosByUserId(UUID userId, int page, int size, UUID viewerId, boolean admin) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        boolean seesAll = admin || userId.equals(viewerId);
        Page<Video> videoPage = seesAll
                ? videoRepository.findByUserIdAndVideosStatusNot(userId, ActiveStatus.DELETED, pageable)
                : videoRepository.findByUserIdAndVideosStatusIn(userId, VideoAccess.LISTED, pageable);
        Page<VideoDto> videoDtoPage = videoPage.map(videoMapper::toDto);
        return new CustomPageResponse<>(videoDtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomPageResponse<VideoDto> getShorts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Video> shorts = videoRepository.findShorts(ActiveStatus.ACTIVE, SHORT_MAX_SECONDS, pageable);
        return new CustomPageResponse<>(shorts.map(videoMapper::toDto));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VideoDto> getAllVideo() {
        List<Video> videos = videoRepository.findByVideosStatusIn(VideoAccess.LISTED);
        return videoMapper.toDtoList(videos);
    }

    private Set<String> parseHashtagText(String hashtagText) {
        if (hashtagText == null || hashtagText.isBlank())
            return Collections.emptySet();
        String[] parts = hashtagText.split("[\\s#]+");
        Set<String> out = new LinkedHashSet<>();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                out.add(part.trim().toLowerCase(Locale.ROOT));
            }
        }
        return out;
    }

    @Override
    public List<VideoDto> searchByTitle(String title) {
        return videoRepository.findByTitleContainingIgnoreCase(title)
                .stream().map(videoMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VideoDto> searchByTitlePaging(String title, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return videoRepository.findByTitleContainingIgnoreCase(title, pageable)
                .map(videoMapper::toDto);
    }

    @Override
    public Page<VideoDto> getAllVideosPaging(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return videoRepository.findAll(pageable).map(videoMapper::toDto);
    }

    @Override
    public VideoDetailResponseDto getDetailWithRecommend(UUID videoId, UUID userId, boolean admin) {
        var video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException(ID_NOT_FOUND + videoId));
        // Riêng tư / bị admin ẩn: chỉ chủ video và admin xem được; đã xoá: không ai xem được
        if (!VideoAccess.canView(video, userId, admin)) {
            throw new ResourceNotFoundException("Video", "id", videoId.toString());
        }

        List<TagDto> tags = tagRepository.findByVideoTags_Video_Id(videoId)
                .stream().map(tag -> new TagDto(tag.getId(), tag.getName(), tag.getCreatedAt(), 0)).toList();

        LikeDto likeInfo = null;
        if (userId != null) {
            likeInfo = likeService.getLikeInfo(videoId, Like.TargetType.VIDEO, userId);
        }
        List<CommentResponseDto> comments = commentRepository
                .findByTargetTypeAndTargetId(Comment.TargetType.VIDEO, videoId)
                .stream()
                .map(commentMapper::toResponseDto)
                .toList();
        UUID channelId = video.getUser().getChannel().getId();

        Integer totalSubscribers = subscriptionRepository.countById_Tier_Channel_Id(channelId);

        boolean subscribed = false;
        if (userId != null) {
            subscribed = subscriptionRepository.existsById_Subscriber_IdAndId_Tier_Channel_Id(userId, channelId);
        }

        var subtitles = videoSubtitleService.getSubtitlesByVideo(videoId);

        var detail = VideoDetailDto.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .videoUrl(video.getVideoUrl())
                .thumbnailUrl(video.getThumbnailUrl())
                .duration(video.getDuration())
                .createAt(video.getCreatedAt())
                .resumeAt(userId != null ? watchProgressService.get(userId, videoId) : null)
                .totalView(video.getTotalView())
                .name(video.getUser().getName())
                .picture(video.getUser().getPicture())
                .like(likeInfo)
                .totalSubscribers(totalSubscribers)
                .subscribed(subscribed)
                .channelId(channelId)
                .comments(comments)
                .subtitles(subtitles)
                .build();

        List<String> tagNames = tags.stream().map(TagDto::getName).toList();
        List<Video> related = tagNames.isEmpty()
                ? List.of()
                : videoRepository.findDistinctByVideoTags_Tag_NameInAndIdNotAndVideosStatusIn(
                        tagNames, videoId, VideoAccess.LISTED);
        if (related.isEmpty()) {
            // Video chưa có tag chung: gợi ý các video mới nhất đang công khai
            related = videoRepository.findByVideosStatusIn(VideoAccess.LISTED,
                            PageRequest.of(0, 13, Sort.by("createdAt").descending()))
                    .getContent().stream()
                    .filter(v -> !v.getId().equals(videoId))
                    .toList();
        }
        var relatedVideos = related.stream()
                .limit(30)
                .map(v -> new RecommendVideoDto(
                        v.getId(),
                        v.getTitle(),
                        v.getThumbnailUrl(),
                        Integer.valueOf(v.getTotalView()),
                        v.getCreatedAt().atZone(ZoneOffset.UTC).toInstant(),
                        v.getUser().getName(),
                        (long) v.getDuration(),
                        v.getUser().getPicture(),
                        v.getVideoUrl(),
                        v.getUser().getChannel() != null ? v.getUser().getChannel().getId() : null))
                .toList();

        var recommend = RecommendResponseDto.builder()
                .video(relatedVideos)
                .tags(tags)
                .build();

        return new VideoDetailResponseDto(detail, recommend);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomPageResponse<VideoDto> getVideosByTag(String tagName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("totalView").descending());
        Page<Video> videos = videoRepository.findDistinctByVideoTags_Tag_NameIgnoreCaseAndVideosStatusIn(
                tagName.trim().replaceFirst("^#", ""), VideoAccess.LISTED, pageable);
        return new CustomPageResponse<>(videos.map(videoMapper::toDto));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPopularTags(int limit) {
        return tagRepository.findPopularTagNames(VideoAccess.LISTED, PageRequest.of(0, Math.max(1, limit))).stream()
                .map(row -> Map.<String, Object>of("name", row[0], "count", row[1]))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VideoDto> getVideoResult(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        String processedQuery = query.trim().replaceAll("\\s+", "%");

        log.info("Executing search with processed query: {}", processedQuery);

        List<Video> videos = videoRepository.findByQuery(processedQuery);

        return videoMapper.toDtoList(videos);
    }

    @Override
    @Transactional(readOnly = true)
    public VideoFormDetailDto getVideoDetail(UUID videoId) {
        Video video = videoRepository.findByIdWithTags(videoId)
                .orElseThrow(() -> new ResourceNotFoundException(VIDEO, "id", videoId.toString()));

        Set<TagDto> tags = video.getVideoTags().stream()
                .map(videoTag -> {
                    Tag tag = videoTag.getTag();
                    return new TagDto(
                            tag.getId(),
                            tag.getName(),
                            tag.getCreatedAt(),
                            tag.getCount());
                })
                .collect(Collectors.toSet());

        return VideoFormDetailDto.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .videoUrl(video.getVideoUrl())
                .thumbnailUrl(video.getThumbnailUrl())
                .status(video.getVideosStatus() != null ? video.getVideosStatus().toString() : null)
                .duration(video.getDuration())
                .createdAt(video.getCreatedAt())
                .updatedAt(video.getUpdatedAt())
                .tags(tags)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VideoDto updateVideo(UUID videoId, VideoUpdateDto updateDto, MultipartFile thumbnailFile,
            UUID authenticatedUserId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException(VIDEO, "id", videoId.toString()));

        if (!video.getUser().getId().equals(authenticatedUserId)) {
            throw new AccessDeniedException("User does not have permission to update this video");
        }

        video.setTitle(updateDto.getTitle());
        video.setDescription(updateDto.getDescription());
        if (video.getVideosStatus() == ActiveStatus.DELETED) {
            throw new ResourceNotFoundException(VIDEO, "id", videoId.toString());
        }
        boolean wasListed = VideoAccess.isListed(video.getVideosStatus());
        // Video bị admin ẩn (INACTIVE): chủ video vẫn sửa được nội dung nhưng không tự bỏ ẩn được
        if (video.getVideosStatus() != ActiveStatus.INACTIVE) {
            video.setVideosStatus(parseVisibility(updateDto.getStatus()));
        }

        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            try {
                String newThumbnailUrl = cloudinaryService.uploadThumbnail(thumbnailFile);
                video.setThumbnailUrl(newThumbnailUrl);
            } catch (Exception e) {
                log.error("Failed to upload new thumbnail for video {}", videoId, e);
            }
        }

        for (VideoTag oldVideoTag : new HashSet<>(video.getVideoTags())) {
            Tag tag = oldVideoTag.getTag();
            tag.setCount(Math.max(0, tag.getCount() - 1));
            tagRepository.save(tag);
        }
        videoTagRepository.deleteAll(video.getVideoTags());
        video.getVideoTags().clear();
        videoRepository.flush();

        if (updateDto.getTags() != null && !updateDto.getTags().isBlank()) {
            addTags(videoId, updateDto.getTags());
        }

        Video updatedVideo = videoRepository.save(video);

        Video dtoSource = videoRepository.findByIdWithTags(updatedVideo.getId())
                .orElseThrow(() -> new ResourceNotFoundException(VIDEO, "id", videoId.toString()));

        // update index
        indexToElasticsearch(dtoSource);
        // Riêng tư / không công khai -> công khai: báo người đăng ký (mỗi video chỉ báo một lần)
        if (!wasListed) {
            notifySubscribers(dtoSource);
        }

        return videoMapper.toDto(dtoSource);
    }

    /** Video công khai: báo cho người đăng ký kênh. Lỗi khi gửi thông báo không làm hỏng việc đang làm. */
    private void notifySubscribers(Video video) {
        if (!VideoAccess.isListed(video.getVideosStatus())) {
            return;
        }
        try {
            var channel = video.getUser().getChannel();
            notificationService.notifyNewVideo(video.getId(), video.getUser().getId(),
                    channel != null ? channel.getId() : null);
        } catch (Exception e) {
            log.warn("Không gửi được thông báo video mới {}: {}", video.getId(), e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVideo(UUID videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException(VIDEO, "id", videoId.toString()));
        if (video.getVideosStatus() == ActiveStatus.DELETED) {
            throw new ResourceNotFoundException(VIDEO, "id", videoId.toString());
        }

        String videoUrl = video.getVideoUrl();
        String thumbnailUrl = video.getThumbnailUrl();

        video.setVideosStatus(ActiveStatus.DELETED);
        videoRepository.save(video);
        videoRepository.flush();
        playlistVideoRepository.deleteByVideoId(videoId);
        categoryVideoRepository.deleteByVideoId(videoId);
        videoIndexer.remove(videoId);
        recentViews.keySet().removeIf(k -> k.startsWith(videoId + ":"));

        // Chỉ xoá file trên Cloudinary sau khi DB đã commit thành công
        Runnable cleanup = () -> {
            cloudinaryService.deleteVideo(videoUrl);
            cloudinaryService.deleteImage(thumbnailUrl);
        };
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            cleanup.run();
                        }
                    });
        } else {
            cleanup.run();
        }
        log.info("Đã xoá (mềm) video {}", videoId);
    }

    @Override
    @Transactional
    public Map<String, Object> registerView(UUID videoId, String viewerKey, UUID userId, boolean admin) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException(VIDEO, "id", videoId.toString()));
        if (!VideoAccess.canView(video, userId, admin)) {
            throw new ResourceNotFoundException(VIDEO, "id", videoId.toString());
        }

        String key = videoId + ":" + viewerKey;
        Boolean first = redisService.setIfAbsent("view:" + key, java.time.Duration.ofMillis(VIEW_DEDUPE_MILLIS));
        boolean counted = (first != null) ? first : firstViewInMemory(key);

        int total = video.getTotalView();
        if (counted) {
            videoRepository.incrementTotalView(videoId);
            total = videoRepository.findTotalViewById(videoId).orElse(total + 1);
            videoIndexer.updateTotalView(videoId, total);
        }
        return Map.of("counted", counted, "totalView", total);
    }

    /** Dự phòng khi không có Redis: nhớ lượt xem trong bộ nhớ (mất khi deploy lại). */
    private boolean firstViewInMemory(String key) {
        long now = System.currentTimeMillis();
        if (recentViews.size() > VIEW_CACHE_PURGE_THRESHOLD) {
            recentViews.values().removeIf(t -> now - t > VIEW_DEDUPE_MILLIS);
        }
        boolean[] counted = {false};
        recentViews.compute(key, (k, last) -> {
            if (last == null || now - last > VIEW_DEDUPE_MILLIS) {
                counted[0] = true;
                return now;
            }
            return last;
        });
        return counted[0];
    }
}
