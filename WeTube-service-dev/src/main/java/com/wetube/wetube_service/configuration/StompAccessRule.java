package com.wetube.wetube_service.configuration;

import org.springframework.messaging.simp.stomp.StompCommand;

/**
 * Ai được làm gì trên kênh STOMP. Khách chưa đăng nhập chỉ được kết nối và nghe {@code /topic/live/...} (xem chat
 * của buổi phát trực tiếp); gửi tin và mọi topic khác (phòng xem chung, hàng đợi riêng) cần đăng nhập.
 */
final class StompAccessRule {

    private static final String LIVE_TOPIC_PREFIX = "/topic/live/";

    private StompAccessRule() {
    }

    static boolean allowed(StompCommand command, String destination, boolean authenticated) {
        if (command == null) {
            return true;
        }
        return switch (command) {
            case SEND -> authenticated;
            case SUBSCRIBE -> authenticated || (destination != null && destination.startsWith(LIVE_TOPIC_PREFIX));
            default -> true;
        };
    }
}
