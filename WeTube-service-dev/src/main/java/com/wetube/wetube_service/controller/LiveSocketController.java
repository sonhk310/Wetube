package com.wetube.wetube_service.controller;

import java.security.Principal;
import java.util.UUID;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import com.wetube.wetube_service.dto.live.ChatDeleteRequest;
import com.wetube.wetube_service.dto.live.ChatTextRequest;
import com.wetube.wetube_service.service.live.LiveChatService;

import lombok.RequiredArgsConstructor;

/**
 * Chat của buổi phát trực tiếp qua STOMP. Người gửi luôn là người đăng nhập của kết nối (Principal = id người dùng
 * trong JWT); khách không gửi được (xem StompAccessRule). Tin của cả buổi: /topic/live/{id}; lỗi riêng: /user/queue/live.
 */
@Controller
@RequiredArgsConstructor
public class LiveSocketController {

    private final LiveChatService chat;

    @MessageMapping("/live/{id}/chat")
    public void post(@DestinationVariable String id, @Payload ChatTextRequest body, Principal principal) {
        UUID streamId = parse(id);
        if (streamId != null && body != null) {
            chat.post(streamId, principal.getName(), body.text());
        }
    }

    @MessageMapping("/live/{id}/chat/delete")
    public void delete(@DestinationVariable String id, @Payload ChatDeleteRequest body, Principal principal) {
        UUID streamId = parse(id);
        if (streamId != null && body != null && body.messageId() != null) {
            chat.delete(streamId, principal.getName(), body.messageId());
        }
    }

    private static UUID parse(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
