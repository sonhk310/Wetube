package com.wetube.wetube_service.dto.live;

/** Trường nào null thì giữ nguyên. */
public record UpdateLiveRequest(String title, String description, Boolean chatEnabled) {
}
