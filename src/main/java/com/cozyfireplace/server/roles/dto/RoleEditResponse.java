package com.cozyfireplace.server.roles.dto;

import java.io.Serializable;

/**
 * DTO for {@link com.cozyfireplace.server.roles.Role}
 */
public record RoleEditResponse(Long id, String name, String[] users) implements Serializable {
}