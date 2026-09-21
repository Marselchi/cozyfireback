package com.cozyfireplace.server.util.exception;

public class NotFoundException extends RuntimeException {

    /**
     * Constructs a new exception with a provided message
     * @param message exception message
     */
    public NotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with a message indicating that an entity of the given type
     * with the specified ID was not found.
     *
     * @param entityType the type of the entity (e.g., "Tag", "Role"); must not be null
     * @param id         the ID of the entity that was not found; may be any object whose {@code toString()}
     *                   representation is meaningful
     */
    public NotFoundException(String entityType, Object id) {
        this(String.format("%s with ID %s not found", entityType, id));
    }

    /**
     * Constructs a new exception with a message indicating that an entity of the given type
     * with the specified field and value of that field was not found.
     *
     * @param entityType the type of the entity (e.g., "Tag", "Role"); must not be null
     * @param field the field of the entity value of which was not found; may be any object whose {@code toString()}
     *              representation is meaningful
     * @param value the value of the entity field that was not found; may be any object whose {@code toString()}
     *              representation is meaningful
     */
    public NotFoundException(String entityType, String field, Object value) {
        this(String.format("%s with %s '%s' not found", entityType, field, value));
    }
}
