package com.wetube.wetube_service.dto.room;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Phòng xem chung (PRD: WatchRoom). Phần có trong JSON được lưu vào Redis để sống qua lần deploy;
 * thành viên đang kết nối ({@link #members}) chỉ có trong bộ nhớ và được dựng lại khi client kết nối lại.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WatchRoom {

    private String code;
    private String hostUserId;
    private long createdAt;
    private Playback state = new Playback();
    private VideoRoom currentVideo;
    private List<VideoRoom> queue = new ArrayList<>();
    private List<Chat> chat = new ArrayList<>();
    private Set<String> kicked = new HashSet<>();

    /** userId -> thành viên, theo thứ tự vào phòng. */
    @JsonIgnore
    private Map<String, Member> members = new LinkedHashMap<>();
    /** Lúc phòng trở nên không còn ai (0 = đang có người). */
    @JsonIgnore
    private long emptySince;
    /** Lúc phát hiện chủ phòng không còn trong phòng nhưng vẫn có người khác (0 = bình thường). */
    @JsonIgnore
    private long hostAbsentSince;

    /** Trạng thái phát gốc của cả phòng; mỗi máy tự tính vị trí từ position + (giờ server - updatedAt). */
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Playback {
        private String videoId;
        private boolean playing;
        /** Giây, tại thời điểm updatedAt. */
        private double position;
        /** Giờ server (ms) lúc position được đặt. */
        private long updatedAt;
        /** Tăng mỗi lần trạng thái đổi; client gửi kèm để server bỏ thao tác lỗi thời. */
        private long version;
        private String lastActor;

        public double positionAt(long now) {
            return playing ? position + Math.max(0, now - updatedAt) / 1000.0 : position;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Member {
        private String userId;
        private String name;
        private String avatar;
        /** @handle của kênh (không có "@"), null nếu chưa có. */
        private String handle;
        private long joinedAt;
        /** Các phiên WebSocket đang mở (một người có thể mở nhiều tab). */
        private Set<String> sessions = new HashSet<>();
        /** Lúc phiên cuối cùng đóng (0 = đang kết nối); quá thời gian chờ thì bị coi là đã rời phòng. */
        private long disconnectedAt;

        public boolean isConnected() {
            return !sessions.isEmpty();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Chat {
        private String id;
        /** CHAT hoặc SYSTEM. */
        private String type;
        private String userId;
        private String name;
        private String avatar;
        private String handle;
        private String content;
        private long at;
    }
}
