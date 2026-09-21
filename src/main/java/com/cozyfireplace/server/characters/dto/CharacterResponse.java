package com.cozyfireplace.server.characters.dto;

import com.cozyfireplace.server.lore.dto.IdName;

import java.util.List;

public record CharacterResponse(
        Long id,
        String name,
        String description,
        String status,
        String content,
        String accountName,
        boolean createdByRoomCreator,
        List<IdName> roles
) {}
