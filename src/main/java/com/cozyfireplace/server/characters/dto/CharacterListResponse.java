package com.cozyfireplace.server.characters.dto;

import java.util.List;

public record CharacterListResponse(
        Long id,
        String name,
        String description,
        String status,
        String accountName,
        boolean createdByRoomCreator
) {}
