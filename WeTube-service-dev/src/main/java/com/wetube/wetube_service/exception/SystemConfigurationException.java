package com.wetube.wetube_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class SystemConfigurationException extends RuntimeException {
    public SystemConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
