package com.wetube.wetube_service.dto.video;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.wetube.wetube_service.dto.LikeDto;
import com.wetube.wetube_service.dto.CommentDto.CommentResponseDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VideoDetailDto {
    private UUID id;
    private String title;
    private String description;
    private String videoUrl;
    private LocalDate createAt;
    private Integer totalView;
    private String name;
    private String picture;
    private LikeDto like;
    private int totalSubscribers;
    private boolean subscribed;
    private UUID channelId;
    private List<CommentResponseDto> comments;
    private List<VideoSubtitleDto> subtitles;
    private boolean isShort;
}
