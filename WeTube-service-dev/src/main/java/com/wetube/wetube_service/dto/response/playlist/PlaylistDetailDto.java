package com.wetube.wetube_service.dto.response.playlist;

import com.wetube.wetube_service.enumeration.PlaylistType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlaylistDetailDto {
    private UUID playlistId;
    private String playlistTitle;
    private PlaylistType playlistType;
    private int totalVideos;
    private LocalDateTime createdAt;
    private List<PlaylistVideoDetailDto> videos;
    private UUID ownerId;
    /** Tên kênh của chủ danh sách (null nếu chưa có kênh). */
    private String ownerName;
    private String privacy;
    /** Số video trong danh sách mà người xem không được thấy (riêng tư của người khác, bị ẩn, đã xoá). */
    private int hiddenCount;
    /** Lần thêm video gần nhất (trong các video xem được); danh sách rỗng thì là ngày tạo danh sách. */
    private LocalDateTime lastUpdatedAt;
}