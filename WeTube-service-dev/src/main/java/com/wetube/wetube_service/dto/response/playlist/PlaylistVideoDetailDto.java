package com.wetube.wetube_service.dto.response.playlist;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PlaylistVideoDetailDto {
    private UUID videoId;
    private String videoTitle;
    private String channelName;
    private UUID channelId;
    private String videoUrl;
    private String thumbnailUrl;
    private String description;
    private float duration;
    /** Boolean (không phải boolean) để JSON có khoá "isShort" thay vì "short". */
    private Boolean isShort;
    private int totalView;
    /** Số bình luận (gồm cả trả lời); chỉ có ở API chi tiết playlist theo id (Studio dùng). */
    private Long commentCount;
    /** Chế độ hiển thị của video (ACTIVE, UNLISTED, PRIVATE, INACTIVE...), để Studio hiện nhãn. */
    private String videoStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
