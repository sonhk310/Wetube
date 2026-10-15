package com.wetube.wetube_service.exception;

public class ChannelAlreadyExistsException extends RuntimeException {
    public ChannelAlreadyExistsException(String message) {
        super(message);
    }
}
