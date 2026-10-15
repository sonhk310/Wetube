package com.wetube.wetube_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDto {
    @org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank
    private String usernameOrEmail;

    @NotBlank
    private String password;
}
