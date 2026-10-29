package com.wetube.wetube_service.dto.live;

/** Một tin chat; at là mili-giây epoch (UTC); premium: người gửi đang có Premium (huy hiệu cạnh tên). */
public record LiveChatMessage(String id, String userId, String name, String avatar, String handle, String text,
                              boolean owner, long at, boolean premium) {
}
