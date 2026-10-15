package com.wetube.wetube_service.dto.channel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class ChannelStructureResponse {
    private boolean success;
    private String message;
    private List<CategoryDto> categories;

    @Data
    @Builder
    public static class CategoryDto {
        private UUID id;
        private String title;
        private Integer orderPosition;
        private Integer videoCount;
        private List<VideoSummaryDto> videos;
    }

    @Data
    @Builder
    public static class VideoSummaryDto  {
        private UUID id;
        private String title;
        private String description;
        private String thumbnailUrl;
        private String videoUrl;
        private Float duration;
        private Integer totalView;
        private Integer orderPosition;
    }
}
