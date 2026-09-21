package com.cozyfireplace.server.roles.dto;


import lombok.Builder;

/**
 * @param name TODO: validation
 */
@Builder
public record RoleUpdateRequest(String name) {
}
