package com.wetube.wetube_service.dto.live;

import com.wetube.wetube_service.enumeration.LiveProtocol;
import com.wetube.wetube_service.enumeration.LiveSource;

/**
 * source mặc định BROWSER; protocol chỉ có nghĩa với ENCODER (mặc định WHIP); chatEnabled mặc định bật;
 * recordingEnabled (mặc định tắt, chỉ BROWSER) bắt buộc kèm topicId, là chủ đề của video xem lại.
 */
public record CreateLiveRequest(String title, String description, LiveSource source, LiveProtocol protocol,
                                Boolean chatEnabled, Boolean recordingEnabled, String topicId) {
}
