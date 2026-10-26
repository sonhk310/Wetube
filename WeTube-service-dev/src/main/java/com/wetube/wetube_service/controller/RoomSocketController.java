package com.wetube.wetube_service.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import com.wetube.wetube_service.dto.room.RoomIn;
import com.wetube.wetube_service.service.watchRoom.WatchRoomService;

import lombok.RequiredArgsConstructor;

/**
 * Phòng xem chung qua STOMP. Người gửi luôn là người đăng nhập của kết nối (Principal = id người dùng trong JWT);
 * kết nối chưa đăng nhập bị chặn từ {@code WebSocketStompConfig}. Trả lời riêng: /user/queue/rooms; cả phòng: /topic/rooms/{code}.
 */
@Controller
@RequiredArgsConstructor
public class RoomSocketController {

    private final WatchRoomService rooms;

    @MessageMapping("/rooms/{code}/join")
    public void join(@DestinationVariable String code, Principal principal, @Header("simpSessionId") String sessionId) {
        rooms.join(code, sessionId, principal.getName());
    }

    @MessageMapping("/rooms/{code}/leave")
    public void leave(@DestinationVariable String code, @Payload RoomIn.Leave body, Principal principal,
                      @Header("simpSessionId") String sessionId) {
        rooms.leave(code, sessionId, principal.getName(), body != null && body.isImmediate());
    }

    @MessageMapping("/rooms/{code}/control")
    public void control(@DestinationVariable String code, @Payload RoomIn.Control body, Principal principal) {
        rooms.control(code, principal.getName(), body);
    }

    @MessageMapping("/rooms/{code}/chat")
    public void chat(@DestinationVariable String code, @Payload RoomIn.Chat body, Principal principal) {
        rooms.chat(code, principal.getName(), body);
    }

    @MessageMapping("/rooms/{code}/queue")
    public void queue(@DestinationVariable String code, @Payload RoomIn.QueueOp body, Principal principal) {
        rooms.queue(code, principal.getName(), body);
    }

    @MessageMapping("/rooms/{code}/kick")
    public void kick(@DestinationVariable String code, @Payload RoomIn.Kick body, Principal principal) {
        rooms.kick(code, principal.getName(), body);
    }

    @MessageMapping("/rooms/{code}/close")
    public void close(@DestinationVariable String code, Principal principal) {
        rooms.close(code, principal.getName());
    }

    @MessageMapping("/rooms/{code}/time")
    public void time(@Payload RoomIn.Time body, Principal principal) {
        rooms.time(principal.getName(), body);
    }
}
