package com.wetube.wetube_service.service.live;

import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.wetube.wetube_service.dto.live.LiveEvent;

import lombok.RequiredArgsConstructor;

/** Gửi tin STOMP của buổi phát: cả buổi qua /topic/live/{id}, riêng một người qua /user/queue/live. */
@Component
@RequiredArgsConstructor
public class LiveEventPublisher {

    private final SimpMessagingTemplate messaging;

    public void toStream(UUID streamId, LiveEvent event) {
        messaging.convertAndSend("/topic/live/" + streamId, event);
    }

    public void toUser(String userId, LiveEvent event) {
        messaging.convertAndSendToUser(userId, "/queue/live", event);
    }
}
