package com.wetube.wetube_service.dto.live;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;

/**
 * Tin phát qua STOMP cho một buổi. type: CHAT, CHAT_DELETED, CHAT_STATE, VIEWERS, POLL, STATUS, THUMBNAIL, ERROR
 * (ERROR chỉ gửi riêng một người). Chỉ các trường thuộc type đó có giá trị.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LiveEvent {
    private final String type;
    private final Integer viewers;
    private final String status;
    private final LiveChatMessage message;
    private final String messageId;
    private final Boolean chatEnabled;
    private final PollView poll;
    private final String text;
    private final String thumbnailUrl;
}
