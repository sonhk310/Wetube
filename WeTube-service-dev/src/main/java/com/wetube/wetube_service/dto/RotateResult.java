package com.wetube.wetube_service.dto;


import com.wetube.wetube_service.entity.AppUser;

public record RotateResult(String sessionId, String newRawRefresh, AppUser user) {
}
