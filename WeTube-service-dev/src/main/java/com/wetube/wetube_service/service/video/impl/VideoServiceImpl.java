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
import com.wetube.wetube_service.repository.video.VideoSearchRepository;
import com.wetube.wetube_service.repository.video.VideoTagRepository;
import com.wetube.wetube_service.dto.LikeDto;
import com.wetube.wetube_service.dto.CommentDto.CommentResponseDto;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.entity.video.VideoTag;
import com.wetube.wetube_service.mapper.interaction.CommentMapper;
import com.wetube.wetube_service.mapper.video.VideoMapper;
import com.wetube.wetube_service.entity.video.Tag;
import com.wetube.wetube_service.search.VideoDocument;
import com.wetube.wetube_service.service.CloudinaryService;
import com.wetube.wetube_service.service.interaction.LikeService;
import com.wetube.wetube_service.service.video.VideoService;
import com.wetube.wetube_service.service.video.VideoSubtitleService;

import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final VideoRepository videoRepository;
    private final TagRepository tagRepository;
    private final VideoTagRepository videoTagRepository;
    private final VideoMapper videoMapper;
    private final CommentMapper commentMapper;
    private final CloudinaryService cloudinaryService;
    private final VideoSearchRepository videoSearchRepository;
    private final VideoSubtitleService videoSubtitleService;

    private final PlaylistRepository playlistRepository;
    private final PlaylistVideoRepository playlistVideoRepository;
    private final UserRepository userRepository;

    private final CommentRepository commentRepository;
    private final LikeService likeService;

    private final SubscriptionRepository subscriptionRepository;

    private static final String ID_NOT_FOUND = "Video not found with id: ";
    private static final String VIDEO = "video";

@Override
@Transactional(rollbackFor = Exception.class)
public VideoDto createVideo(MultipartFile videoFile,
        MultipartFile thumbnailFile,
        VideoDto videoDto, UUID authenticatedUserId) throws Exception {

    if (videoFile == null || videoFile.isEmpty()) {
        throw new IllegalArgumentException("Video file is required");
    }

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
    entity.setVideosStatus(ActiveStatus.ACTIVE);

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
        try {
            VideoDocument doc = new VideoDocument();
            doc.setId(video.getId().toString());
            doc.setTitle(video.getTitle());
            doc.setDescription(video.getDescription());
            doc.setUserId(video.getUser().getId().toString());
            doc.setTags(video.getVideoTags().stream()
                    .map(vt -> vt.getTag().getName())
                    .toList());
            doc.setCategories(List.of());
            doc.setCreatedAt(video.getCreatedAt().atZone(ZoneOffset.UTC).toInstant());
            doc.setThumbnailUrl(video.getThumbnailUrl());
            doc.setVideoUrl(video.getVideoUrl());
            doc.setTotalView(video.getTotalView());
            doc.setDuration(video.getDuration());
            doc.setName(video.getUser().getName());
            doc.setPicture(video.getUser().getPicture());

            videoSearchRepository.save(doc);
            log.info("Indexed video {} to Elasticsearch", video.getId());
        } catch (Exception e) {
            log.error("Failed to index video {} to Elasticsearch: {}", video.getId(), e.getMessage());
        }
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
                .orElseThrow(() -> new IllegalArgumentException(ID_NOT_FOUND + videoId));

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
                .orElseThrow(() -> new IllegalArgumentException(ID_NOT_FOUND + videoId));

        // update index sau khi add tag
        indexToElasticsearch(dtoSource);

        return videoMapper.toDto(dtoSource);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomPageResponse<VideoDto> getVideosByUserId(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Video> videoPage = videoRepository.findByUserId(userId, pageable);
        Page<VideoDto> videoDtoPage = videoPage.map(videoMapper::toDto);
        return new CustomPageResponse<>(videoDtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VideoDto> getAllVideo() {
        List<Video> videos = videoRepository.findAll();
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
    public VideoDetailResponseDto getDetailWithRecommend(UUID videoId, UUID userId) {
        var video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException(ID_NOT_FOUND + videoId));

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
                .createAt(video.getCreatedAt().toLocalDate())
                .totalView(video.getTotalView())
                .name(video.getUser().getName())
                .picture(video.getUser().getPicture())
                .like(likeInfo)
                .totalSubscribers(totalSubscribers)
                .subscribed(subscribed)
                .comments(comments)
                .subtitles(subtitles)
                .build();

        var relatedVideos = videoRepository.findDistinctByVideoTags_Tag_NameInAndIdNot(
                tags.stream().map(TagDto::getName).toList(),
                videoId)
                .stream()
                .map(v -> new RecommendVideoDto(
                        v.getId(),
                        v.getTitle(),
                        v.getThumbnailUrl(),
                        Integer.valueOf(v.getTotalView()),
                        v.getCreatedAt().atZone(ZoneOffset.UTC).toInstant(),
                        v.getUser().getName(),
                        (long) v.getDuration(),
                        v.getUser().getPicture(),
                        v.getVideoUrl()))
                .toList();

        var recommend = RecommendResponseDto.builder()
                .video(relatedVideos)
                .tags(tags)
                .build();

        return new VideoDetailResponseDto(detail, recommend);
    }

    @Override
    public List<VideoDto> getVideosByTag(String tagName) {
        return videoRepository.findByVideoTags_Tag_NameOrderByTotalViewDesc(tagName)
                .stream()
                .map(videoMapper::toDto)
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
                .createdAt(video.getCreatedAt() != null ? video.getCreatedAt().toLocalDate() : null)
                .updatedAt(video.getUpdatedAt() != null ? video.getUpdatedAt().toLocalDate() : null)
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
        video.setVideosStatus(ActiveStatus.valueOf(updateDto.getStatus().toUpperCase()));

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

        return videoMapper.toDto(dtoSource);
    }
}
