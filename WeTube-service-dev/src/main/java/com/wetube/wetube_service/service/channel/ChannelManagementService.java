package com.wetube.wetube_service.service.channel;

import com.wetube.wetube_service.dto.channel.ChannelStructureResponse;
import com.wetube.wetube_service.dto.channel.UpdateCategoryOrderRequest;
import com.wetube.wetube_service.dto.channel.UpdateVideoOrderRequest;
import com.wetube.wetube_service.enumeration.CategoryType;

import java.util.List;
import java.util.UUID;

public interface ChannelManagementService {
    ChannelStructureResponse getChannelStructure(UUID channelId, boolean includeVideos);

    void updateCategoryOrder(UUID channelId, List<UpdateCategoryOrderRequest.CategoryOrderDto> categories);

    void updateVideoOrderInCategory(UUID channelId, UUID categoryId,
                                    List<UpdateVideoOrderRequest.VideoOrderDto> videos);

    void addVideoToCategory(UUID channelId, CategoryType categoryType, UUID videoId);
    void addVideoToCategory(UUID channelId, UUID categoryId, UUID videoId);
    void removeVideoFromCategory(UUID channelId, UUID categoryId, UUID videoId);
    void toggleCategoryVisibility(UUID channelId, UUID categoryId, boolean isVisible);
    UUID getCategoryIdByType(UUID channelId, CategoryType categoryType);

}
