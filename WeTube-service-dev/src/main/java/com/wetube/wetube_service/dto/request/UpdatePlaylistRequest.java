package com.wetube.wetube_service.dto.request;

/** Đổi tên / chế độ hiển thị của danh sách phát; privacy là PUBLIC hoặc PRIVATE. */
public record UpdatePlaylistRequest(String title, String privacy) {
}
