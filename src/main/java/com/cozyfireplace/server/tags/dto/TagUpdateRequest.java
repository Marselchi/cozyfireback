package com.cozyfireplace.server.tags.dto;

import java.io.Serializable;

/**
 * DTO for {@link com.cozyfireplace.server.tags.Tag}
 */
public record TagUpdateRequest( String name) implements Serializable {
}