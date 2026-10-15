package com.wetube.wetube_service.controller;


import com.wetube.wetube_service.dto.room.*;
import com.wetube.wetube_service.service.watchRoom.RoomService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
@AllArgsConstructor
@Slf4j
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping
    public ResponseEntity<Map<String, Room>> getAllRooms(){
        return ResponseEntity.ok(roomService.getRooms());
    }

    @MessageMapping("/room/create")
    @SendTo("/topic/room/create")
    public Room handleCreateRoom(@Payload Map<String, String> payload) {
        String username = payload.get("username");
        System.out.println("Received username: " + username);
        WatchMember host = new WatchMember();
        host.setUsername(username);
        host.setHost(true);


        Room room = roomService.createRoom(host);
        return room;
    }

    @MessageMapping("/room/join/{roomId}")
    @SendTo("/topic/rooms/join/{roomId}")
    public Room handleJoinRoom(@DestinationVariable String roomId,
                               @Payload Map<String, String> body ) {
        String username = body.get("username");
        return roomService.addMember(roomId,username);
        // TODO: Handle member + chat
    }

    @MessageMapping("/room/member/{roomId}")
    @SendTo("/topic/rooms/member/{roomId}")
    public List<WatchMember> handleMemberRoom(@DestinationVariable String roomId,
                                              @Payload Map<String, String> body) {
        String username = body.get("username");
        System.out.println("Request to ADD member in room " + roomId + ": " + username);
        Room room = roomService.getRooms().get(roomId);
        System.out.println("Members BEFORE add: " + room.getMembers());
        List<WatchMember> updatedMembers = roomService.addMember(roomId, username).getMembers();
        System.out.println("Members AFTER add: " + updatedMembers);

        return updatedMembers;
    }


    @MessageMapping("/room/member/leave/{roomId}")
    @SendTo("/topic/rooms/member/{roomId}")
    public List<WatchMember> handleRemoveMember(@DestinationVariable String roomId,
                                                @Payload Map<String, String> body) {
        String username = body.get("username");
        System.out.println("Request to REMOVE member in room " + roomId + ": " + username);
        Room room = roomService.getRooms().get(roomId);
        System.out.println("Members BEFORE remove: " + room.getMembers());
        List<WatchMember> updatedMembers = roomService.removeMember(roomId, username).getMembers();
        System.out.println("Members AFTER remove: " + updatedMembers);

        return updatedMembers;
    }


    @MessageMapping("/chat/{roomId}")
    @SendTo("/topic/rooms/chat/{roomId}")
    public ClientChatMessage handleChat(
            @DestinationVariable String roomId,
            @Payload ClientChatMessage message) {
        System.out.println("Received message in room " + roomId + ": " + message);
        return message;
    }

    @MessageMapping("/room/addSong/{roomId}")
    @SendTo("/topic/rooms/addSong/{roomId}")
    public VideoRoom handleAddSong(
            @DestinationVariable String roomId,
            @Payload Map<String, String> body) {
        String videoId = body.get("videoId");
        System.out.println("Received Video Add in room " + roomId + ": " + videoId);
        return roomService.addSong(roomId,videoId);
    }

    @MessageMapping("/room/mediaState/{roomId}")
    @SendTo("/topic/rooms/mediaState/{roomId}")
    public MediaPlayerState handlePickSong(
            @DestinationVariable String roomId,
            @Payload MediaPlayerState mediaState) {

        log.debug("Received MediaState in room {}: {}", roomId, mediaState);

        // Gọi service để xử lý và cập nhật trạng thái
        MediaPlayerState updatedState = roomService.handleState(roomId, mediaState);

        // Log trạng thái đã được cập nhật
        log.debug("Updated MediaState in room {}: {}", roomId, updatedState);
        return updatedState;
    }

}

