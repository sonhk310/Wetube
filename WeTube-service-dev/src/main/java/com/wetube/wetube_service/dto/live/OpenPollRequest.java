package com.wetube.wetube_service.dto.live;

import java.util.List;

/** Mở Poll: câu hỏi tối đa 150 ký tự, 2–4 lựa chọn, mỗi lựa chọn tối đa 60 ký tự. */
public record OpenPollRequest(String question, List<String> options) {
}
