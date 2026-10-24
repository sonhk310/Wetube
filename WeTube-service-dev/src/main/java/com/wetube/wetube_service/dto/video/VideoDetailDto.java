package com.wetube.wetube_service.dto.video;

import java.time.LocalDateTime;
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
    /** Ảnh thu nhỏ, dùng cho thẻ chia sẻ (og:image). */
    private String thumbnailUrl;
    private Float duration;
    private LocalDateTime createAt;
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
    /** Giây đã xem lần trước (người xem đã đăng nhập), để phát tiếp; null nếu chưa xem. */
    private Float resumeAt;
}
