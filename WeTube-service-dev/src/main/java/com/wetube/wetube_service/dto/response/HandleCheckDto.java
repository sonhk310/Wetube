package com.wetube.wetube_service.dto.response;

/** Kết quả kiểm tra một @handle: dùng được hay không, và lý do (tiếng Việt) khi không. */
public record HandleCheckDto(boolean available, String reason) {
}
