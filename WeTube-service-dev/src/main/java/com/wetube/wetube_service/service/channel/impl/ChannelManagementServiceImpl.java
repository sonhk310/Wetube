package com.wetube.wetube_service.service.channel.impl;

import com.wetube.wetube_service.dto.channel.ChannelStructureResponse;
import com.wetube.wetube_service.dto.channel.UpdateCategoryOrderRequest;
import com.wetube.wetube_service.dto.channel.UpdateVideoOrderRequest;
import com.wetube.wetube_service.entity.channel.ChannelCategory;
import com.wetube.wetube_service.entity.channel.CategoryVideo;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.enumeration.CategoryType;
import com.wetube.wetube_service.repository.channel.ChannelCategoryRepository;
import com.wetube.wetube_service.repository.channel.CategoryVideoRepository;
import com.wetube.wetube_service.repository.channel.ChannelRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.service.channel.ChannelManagementService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ChannelManagementServiceImpl implements ChannelManagementService {

    private final ChannelCategoryRepository categoryRepository;
    private final CategoryVideoRepository categoryVideoRepository;
    private final ChannelRepository channelRepository;
    private final VideoRepository videoRepository;

    @Override
    @Transactional(readOnly = true)
    public ChannelStructureResponse getChannelStructure(UUID channelId, boolean includeVideos) {
        log.info("Getting channel structure for channel: {}, includeVideos: {}", channelId, includeVideos);

        try {
            validateChannelExists(channelId);

            List<ChannelCategory> categories;

            if (includeVideos) {
                categories = categoryRepository.findByChannelIdWithVideos(channelId);
            } else {
                categories = categoryRepository.findByChannelIdOrderByOrderPositionAsc(channelId);
            }

            List<ChannelStructureResponse.CategoryDto> categoryDtos = categories.stream()
                    .map(this::mapToCategoryDto)
                    .collect(Collectors.toList());

            return new ChannelStructureResponse(true, "Success", categoryDtos);

        } catch (EntityNotFoundException e) {
            log.error("Channel not found: {}", channelId);
            return new ChannelStructureResponse(false, e.getMessage(), null);
        } catch (Exception e) {
            log.error("Error getting channel structure for channel: {}", channelId, e);
            return new ChannelStructureResponse(false, "Internal server error", null);
        }
    }

    @Override
    public void updateCategoryOrder(UUID channelId, List<UpdateCategoryOrderRequest.CategoryOrderDto> categories) {
        log.info("Updating category order for channel: {} with {} categories", channelId, categories.size());

        try {
            validateChannelExists(channelId);

            // Validate ownership of all categories
            for (UpdateCategoryOrderRequest.CategoryOrderDto category : categories) {
                if (!categoryRepository.existsByIdAndChannelId(category.getId(), channelId)) {
                    throw new AccessDeniedException("Channel doesn't own category: " + category.getId());
                }
            }

            for (int i = 0; i < categories.size(); i++) {
                UpdateCategoryOrderRequest.CategoryOrderDto category = categories.get(i);
                categoryRepository.updateOrderPosition(category.getId(), channelId, i + 1);
            }

            log.info("Successfully updated category order for channel: {}", channelId);

        } catch (Exception e) {
            log.error("Error updating category order for channel: {}{}", e, channelId);
            throw e;
        }
    }

    @Override
    public void updateVideoOrderInCategory(UUID channelId, UUID categoryId,
                                           List<UpdateVideoOrderRequest.VideoOrderDto> videos) {
        log.info("Updating video order in category: {} for channel: {} with {} videos",
                categoryId, channelId, videos.size());

        try {
            validateChannelExists(channelId);
            validateCategoryOwnership(categoryId, channelId);

            // Validate video ownership
            for (UpdateVideoOrderRequest.VideoOrderDto video : videos) {
                if (!categoryVideoRepository.existsByVideoIdAndCategoryChannelId(video.getId(), channelId)) {
                    throw new AccessDeniedException("Channel doesn't own video: " + video.getId());
                }
            }

            // Update video order positions
            for (int i = 0; i < videos.size(); i++) {
                UpdateVideoOrderRequest.VideoOrderDto video = videos.get(i);
                categoryVideoRepository.updateOrderPosition(video.getId(), categoryId, i + 1);
            }

            log.info("Successfully updated video order in category: {} for channel: {}", categoryId, channelId);

        } catch (Exception e) {
            log.error("Error updating video order for channel: {}{}", e, channelId);
            throw e;
        }
    }

    @Override
    public void addVideoToCategory(UUID channelId, CategoryType categoryType, UUID videoId) {
        log.info("Adding video: {} to category type: {} in channel: {}", videoId, categoryType, channelId);

        try {
            validateChannelExists(channelId);
            validateVideoExists(videoId);

            // Find category by type
            ChannelCategory category = (ChannelCategory) categoryRepository
                    .findByChannelIdAndCategoryType(channelId, categoryType)
                    .orElseThrow(() -> new EntityNotFoundException("Category type " + categoryType + " not found in channel"));

            addVideoToCategoryInternal(category, videoId);

            log.info("Successfully added video: {} to category type: {}", videoId, categoryType);

        } catch (Exception e) {
            log.error("Error adding video to category type: {}{}", e, categoryType);
            throw e;
        }
    }

    @Override
    public void addVideoToCategory(UUID channelId, UUID categoryId, UUID videoId) {
        log.info("Adding video: {} to category: {} in channel: {}", videoId, categoryId, channelId);

        try {
            validateChannelExists(channelId);
            validateCategoryOwnership(categoryId, channelId);
            validateVideoExists(videoId);

            ChannelCategory category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new EntityNotFoundException("Category not found"));

            addVideoToCategoryInternal(category, videoId);

            log.info("Successfully added video: {} to category: {}", videoId, categoryId);

        } catch (Exception e) {
            log.error("Error adding video to category", e);
            throw e;
        }
    }

    @Override
    public void removeVideoFromCategory(UUID channelId, UUID categoryId, UUID videoId) {
        log.info("Removing video: {} from category: {} in channel: {}", videoId, categoryId, channelId);

        try {
            // Validate ownership
            validateChannelExists(channelId);
            validateCategoryOwnership(categoryId, channelId);

            // Find and delete CategoryVideo relationship
            CategoryVideo categoryVideo = categoryVideoRepository
                    .findByVideoIdAndCategoryId(videoId, categoryId)
                    .orElseThrow(() -> new EntityNotFoundException("Video not found in this category"));

            categoryVideoRepository.delete(categoryVideo);

            // Reorder remaining videos
            reorderVideosInCategory(categoryId);

            log.info("Successfully removed video: {} from category: {}", videoId, categoryId);

        } catch (Exception e) {
            log.error("Error removing video from category", e);
            throw e;
        }
    }

    @Override
    public void toggleCategoryVisibility(UUID channelId, UUID categoryId, boolean isVisible) {
        log.info("Toggling category: {} visibility to: {} in channel: {}", categoryId, isVisible, channelId);

        try {
            validateChannelExists(channelId);
            validateCategoryOwnership(categoryId, channelId);

            categoryRepository.updateVisibility(categoryId, channelId, isVisible);

            log.info("Successfully toggled category: {} visibility to: {}", categoryId, isVisible);

        } catch (Exception e) {
            log.error("Error toggling category visibility", e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UUID getCategoryIdByType(UUID channelId, CategoryType categoryType) {
        validateChannelExists(channelId);

        return categoryRepository.findByChannelIdAndCategoryType(channelId, categoryType)
                .map(ChannelCategory::getId)
                .orElse(null);
    }

    // Private helper methods
    private void addVideoToCategoryInternal(ChannelCategory category, UUID videoId) {
        // Check if video already exists in this category
        if (categoryVideoRepository.existsByVideoIdAndCategoryId(videoId, category.getId())) {
            throw new IllegalArgumentException("Video already exists in this category");
        }

        // Get current max order position in category
        List<CategoryVideo> existingVideos = categoryVideoRepository
                .findByCategoryIdOrderByOrderPositionAsc(category.getId());
        int nextOrderPosition = existingVideos.size() + 1;

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new EntityNotFoundException("Video not found"));

        CategoryVideo categoryVideo = CategoryVideo.builder()
                .category(category)
                .video(video)
                .orderPosition(nextOrderPosition)
                .build();

        categoryVideoRepository.save(categoryVideo);
    }

    private ChannelStructureResponse.CategoryDto mapToCategoryDto(ChannelCategory category) {
        ChannelStructureResponse.CategoryDto.CategoryDtoBuilder builder =
                ChannelStructureResponse.CategoryDto.builder()
                        .id(category.getId())
                        .title(category.getDisplayName())
                        .orderPosition(category.getOrderPosition())
                        .videoCount(category.getCategoryVideos().size());

        if (!category.getCategoryVideos().isEmpty()) {
            List<ChannelStructureResponse.VideoSummaryDto> videoDtos =
                    category.getCategoryVideos().stream()
                            .map(this::mapToVideoDto)
                            .collect(Collectors.toList());
            builder.videos(videoDtos);
        }

        return builder.build();
    }

    private ChannelStructureResponse.VideoSummaryDto mapToVideoDto(CategoryVideo categoryVideo) {
        Video video = categoryVideo.getVideo();
        return ChannelStructureResponse.VideoSummaryDto.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .thumbnailUrl(video.getThumbnailUrl())
                .videoUrl(video.getVideoUrl())
                .duration(video.getDuration())
                .totalView(video.getTotalView())
                .orderPosition(categoryVideo.getOrderPosition())
                .build();
    }

    private void validateChannelExists(UUID channelId) {
        if (!channelRepository.existsById(channelId)) {
            throw new EntityNotFoundException("Channel not found with id: " + channelId);
        }
    }

    private void validateCategoryOwnership(UUID categoryId, UUID channelId) {
        if (!categoryRepository.existsByIdAndChannelId(categoryId, channelId)) {
            throw new AccessDeniedException("Channel doesn't own category: " + categoryId);
        }
    }

    private void validateVideoExists(UUID videoId) {
        if (!videoRepository.existsById(videoId)) {
            throw new EntityNotFoundException("Video not found with id: " + videoId);
        }
    }

    private void reorderVideosInCategory(UUID categoryId) {
        List<CategoryVideo> videos = categoryVideoRepository
                .findByCategoryIdOrderByOrderPositionAsc(categoryId);

        for (int i = 0; i < videos.size(); i++) {
            CategoryVideo video = videos.get(i);
            video.setOrderPosition(i + 1);
            categoryVideoRepository.save(video);
        }
    }

    private void reorderCategoriesInChannel(UUID channelId) {
        List<ChannelCategory> categories = categoryRepository
                .findByChannelIdOrderByOrderPositionAsc(channelId);

        for (int i = 0; i < categories.size(); i++) {
            ChannelCategory category = categories.get(i);
            category.setOrderPosition(i + 1);
            categoryRepository.save(category);
        }
    }
}