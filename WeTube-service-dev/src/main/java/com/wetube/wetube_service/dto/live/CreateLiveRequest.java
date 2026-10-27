package com.wetube.wetube_service.dto.live;

import com.wetube.wetube_service.enumeration.LiveProtocol;
import com.wetube.wetube_service.enumeration.LiveSource;

/** source mặc định BROWSER; protocol chỉ có nghĩa với ENCODER (mặc định WHIP); chatEnabled mặc định bật. */
public record CreateLiveRequest(String title, String description, LiveSource source, LiveProtocol protocol,
                                Boolean chatEnabled) {
}
