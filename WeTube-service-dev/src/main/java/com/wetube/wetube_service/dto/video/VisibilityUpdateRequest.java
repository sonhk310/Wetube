package com.wetube.wetube_service.dto.video;

import java.util.List;
import java.util.UUID;

/** PATCH /api/videos/visibility: đổi chế độ hiển thị nhiều video (PUBLIC | UNLISTED | PRIVATE). */
public record VisibilityUpdateRequest(List<UUID> videoIds, String status) {
}
