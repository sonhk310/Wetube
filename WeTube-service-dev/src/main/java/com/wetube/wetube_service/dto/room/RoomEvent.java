package com.wetube.wetube_service.dto.room;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;

/**
 * Tin server gửi cho client: `/topic/rooms/{code}` (cả phòng) hoặc `/user/queue/rooms` (riêng một người).
 * type: SNAPSHOT, STATE, MEMBERS, QUEUE, CHAT, TIME, CLOSED, KICKED, ERROR.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoomEvent {

    public record MemberView(String userId, String name, String avatar, String handle, boolean host) {
    }

    private final String type;
    private final String code;
    private final WatchRoom.Playback state;
    private final VideoRoom video;
    private final List<MemberView> members;
    private final String hostUserId;
    private final List<VideoRoom> queue;
    private final List<WatchRoom.Chat> chat;
    private final WatchRoom.Chat message;
    private final String userId;
    private final String name;
    private final Long serverTime;
    private final Long clientSent;
    /** Mã lỗi (ERROR): NOT_FOUND, KICKED, FULL, RATE, INVALID. */
    private final String error;
    private final String text;
}
