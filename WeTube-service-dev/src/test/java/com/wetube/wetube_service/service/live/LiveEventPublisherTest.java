package com.wetube.wetube_service.service.live;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.wetube.wetube_service.dto.live.LiveEvent;

/** Địa chỉ topic phải khớp với client: /topic/live/{id} cho cả buổi, /user/queue/live cho riêng một người. */
class LiveEventPublisherTest {

    private final SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
    private final LiveEventPublisher publisher = new LiveEventPublisher(messaging);

    @Test
    void streamEventsGoToTheLiveTopic() {
        UUID id = UUID.randomUUID();
        LiveEvent event = LiveEvent.builder().type("VIEWERS").viewers(3).build();

        publisher.toStream(id, event);

        verify(messaging).convertAndSend("/topic/live/" + id, event);
    }

    @Test
    void privateEventsGoToTheUserQueue() {
        LiveEvent event = LiveEvent.builder().type("ERROR").text("Bạn nhắn quá nhanh").build();

        publisher.toUser("user-1", event);

        verify(messaging).convertAndSendToUser("user-1", "/queue/live", event);
    }
}
