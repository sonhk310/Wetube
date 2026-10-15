package com.wetube.wetube_service.service.watchRoom;


import com.wetube.wetube_service.dto.room.MediaPlayerState;
import com.wetube.wetube_service.dto.room.Room;
import com.wetube.wetube_service.dto.room.VideoRoom;
import com.wetube.wetube_service.dto.room.WatchMember;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.exception.ResourceNotFoundException;
import com.wetube.wetube_service.mapper.video.VideoMapper;
import com.wetube.wetube_service.repository.video.VideoRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Getter
@Setter
@Slf4j
@AllArgsConstructor
public class RoomService {
    private final VideoRepository videoRepo;
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final VideoMapper videoMapper;

    public Room createRoom(WatchMember host) {

        String roomId = generateFriendlyRoomId();

        Room room = new Room();
        room.setRoomId(roomId);
        room.getMembers().add(host);

        rooms.put(roomId, room);

        log.debug("Room created: {}", room);
        return room;
    }

    public Room addMember(String roomId, String username) {
        Room room = rooms.get(roomId);
        if (room == null) return null;

        boolean exists = room.getMembers().stream()
                .anyMatch(m -> m.getUsername().equals(username));

        if (!exists) {
            WatchMember member = new WatchMember();
            member.setUsername(username);
            member.setHost(false);
            room.getMembers().add(member);
        }

        log.debug("Member added: {} to room {}", username, roomId);
        return room;
    }
    public Room removeMember(String roomId, String username) {
        Room room = rooms.get(roomId);
        if (room == null) return null;

        boolean removed = room.getMembers().removeIf(
                m -> m.getUsername().equals(username)
        );

        if (removed) {
            log.debug("Member removed: {} from room {}", username, roomId);
        } else {
            log.debug("Member {} not found in room {}", username, roomId);
        }

        return room;
    }

    private String generateFriendlyRoomId() {
        return generateRoomId(3) + "-" + generateRoomId(4) + "-" + generateRoomId(3);
    }

    private String generateRoomId(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    @Transactional
    public VideoRoom addSong(String roomId, String videoId) {
        Room room = rooms.get(roomId);
        Video video =null;
        try {
            UUID uuid = UUID.fromString(videoId);
            video = videoRepo.findByIdWithTags(uuid)
                    .orElseThrow(() -> new ResourceNotFoundException("video", "id", videoId));
            System.out.println("Valid UUID: " + uuid);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid UUID format: " + videoId);
        }

        VideoRoom videoRoom = videoMapper.toRoomDto(video);

        room.getPlaylist().add(videoRoom);

        log.debug("Video added to room {}: {}", roomId, room.getPlaylist());

        return videoRoom;
    }

    @Transactional
    public MediaPlayerState handleState(String roomId, MediaPlayerState mediaState) {
        Room room = rooms.get(roomId);

        room.setPlayerState(mediaState);;

        log.debug("MediaPlayerState for room {} updated to: {}", roomId, room.getPlayerState());

        return room.getPlayerState();
    }
}
