package com.cozyfireplace.server.tags.dto;

import java.io.Serializable;

/**
 * DTO for {@link com.cozyfireplace.server.tags.Tag}
 */
//TODO: record for all dto
public record TagCreateRequest(String name) implements Serializable {
}