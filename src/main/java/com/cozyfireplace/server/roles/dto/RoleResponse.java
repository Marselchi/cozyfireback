package com.cozyfireplace.server.roles.dto;

import lombok.Builder;

@Builder
public record RoleResponse(Long id, String name) {
}
