package com.wetube.wetube_service.dto.request;

/**
 * Sửa kênh của chính mình; trường nào null thì giữ nguyên. description rỗng = xoá mô tả,
 * backgroundImgUrl rỗng = bỏ ảnh bìa (ảnh đại diện và tên thì không được để trống).
 */
public record ChannelUpdateRequest(String name, String description, String picture, String backgroundImgUrl) {
}
