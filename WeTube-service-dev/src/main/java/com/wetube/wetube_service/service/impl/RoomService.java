// package com.wetube.wetube_service.service.impl;

//import com.wetube.wetube_service.dto.room.room.JoinRoomRequest;
//import com.wetube.wetube_service.dto.room.room.Room;
//import com.wetube.wetube_service.dto.room.playlist.VideoStateChangeRequest;
//import com.wetube.wetube_service.dto.room.room.WatchMember;
// import lombok.Getter;
// import lombok.Setter;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.stereotype.Service;

// import java.security.SecureRandom;
// import java.util.List;
// import java.util.Map;
// import java.util.Random;
// import java.util.UUID;
// import java.util.concurrent.ConcurrentHashMap;

// @Service
// @Getter
// @Setter
// @Slf4j
// public class RoomService {
//    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
//
//    public Room createRoom(WatchMember host) {
//
//        String roomId = generateFriendlyRoomId();
//
//        Room room = new Room();
//        room.setRoomId(roomId);
//        room.getMembers().add(host);
//
//        rooms.put(roomId, room);
//
//        log.debug("Room created: {}", room);
//        return room;
//    }
//
//    public Room addMember(String roomId, String username) {
//        Room room = rooms.get(roomId);
//        if (room == null) return null;
//
//        boolean exists = room.getMembers().stream()
//                .anyMatch(m -> m.getUsername().equals(username));
//
//        if (!exists) {
//            WatchMember member = new WatchMember();
//            member.setUsername(username);
//            member.setHost(false);
//            room.getMembers().add(member);
//        }
//
//        log.debug("Member added: {} to room {}", username, roomId);
//        return room;
//    }


    // private String generateFriendlyRoomId() {
    //     return generateRoomId(3) + "-" + generateRoomId(4) + "-" + generateRoomId(3);
    // }

//     private String generateRoomId(int length) {
//         String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
//         Random random = new SecureRandom();
//         StringBuilder sb = new StringBuilder(length);
//         for (int i = 0; i < length; i++) {
//             sb.append(chars.charAt(random.nextInt(chars.length())));
//         }
//         return sb.toString();
//     }
// }
