package com.wetube.wetube_service.dto.room;

import lombok.Data;

/** Nội dung client gửi lên `/app/rooms/{code}/...`. Người gửi luôn lấy từ phiên đăng nhập, không có trong nội dung. */
public final class RoomIn {

    private RoomIn() {
    }

    /** type: PLAY, PAUSE, SEEK, CHANGE (videoId), ENDED. version = phiên bản trạng thái client đang thấy. */
    @Data
    public static class Control {
        private String type;
        private long version;
        private double position;
        private String videoId;
    }

    @Data
    public static class Chat {
        private String content;
    }

    /** emoji: like, love, laugh, wow, sad, fire. */
    @Data
    public static class Reaction {
        private String emoji;
    }

    /** op: ADD hoặc REMOVE. */
    @Data
    public static class QueueOp {
        private String op;
        private String videoId;
    }

    @Data
    public static class Kick {
        private String userId;
    }

    @Data
    public static class Leave {
        /** true: bấm "Rời phòng", rời ngay; false: đóng / tải lại trang, chờ vài giây phòng khi vào lại. */
        private boolean immediate;
    }

    @Data
    public static class Time {
        private long clientSent;
    }
}
