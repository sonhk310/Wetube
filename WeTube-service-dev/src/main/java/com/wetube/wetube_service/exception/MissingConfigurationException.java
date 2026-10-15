package com.wetube.wetube_service.exception;

public class MissingConfigurationException extends RuntimeException {
    public MissingConfigurationException(String message) {
        super(message);
    }
}
