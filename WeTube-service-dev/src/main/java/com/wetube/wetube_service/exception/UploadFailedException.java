package com.wetube.wetube_service.exception;

public class UploadFailedException extends RuntimeException {
    public UploadFailedException(String message) {
        super(message);
    }

    public UploadFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
