package com.wetube.wetube_service.dto.live;

/** Nội dung gửi lên /app/live/{id}/chat/delete. */
public record ChatDeleteRequest(String messageId) {
}
