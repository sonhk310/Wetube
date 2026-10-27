package com.wetube.wetube_service.dto.live;

/** URL và khoá cho phần mềm phát (OBS); chỉ trả cho chủ kênh. */
public record IngressView(String url, String streamKey) {
}
