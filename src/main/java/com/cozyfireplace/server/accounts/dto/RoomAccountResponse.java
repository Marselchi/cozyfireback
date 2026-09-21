package com.cozyfireplace.server.accounts.dto;

import com.cozyfireplace.server.roles.dto.RoleResponse;

import java.io.Serializable;
import java.util.Set;

public record RoomAccountResponse(
        Long id,
        String username,
        String profileName,
        Set<RoleResponse> roles)
        implements Serializable { }
