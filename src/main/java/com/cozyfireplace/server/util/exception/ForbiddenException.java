package com.cozyfireplace.server.util.exception;

public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String action, String entityType) {
        super(String.format("Forbidden: %s %s", action, entityType));
    }
}
