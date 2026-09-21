package com.cozyfireplace.server.util.exception;

public class AlreadyExistsException extends RuntimeException {
    public AlreadyExistsException(String message) {
        super(message);
    }

    public AlreadyExistsException(String entityType, Object id) {
        this(String.format("%s with ID %s already exists", entityType, id));
    }

    public AlreadyExistsException(String entityType, String field, Object value) {
        this(String.format("%s with %s '%s' already exists", entityType, field, value));
    }
}
