package com.wetube.wetube_service.dto.response;

import io.micrometer.common.lang.Nullable;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class LoginResponseDto {
    @NotBlank
    private String message;

    @Nullable
    private String accessToken;

    @Nullable
    private String refreshToken;
}
