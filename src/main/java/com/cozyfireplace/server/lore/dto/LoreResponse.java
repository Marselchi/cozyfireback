package com.cozyfireplace.server.lore.dto;

import java.util.List;

public record LoreResponse(
        Long id,
        String title,
        String description,
        String date,
        String content,
        String accountName,
        boolean createdByRoomCreator,
        List<IdName> roles,
        List<IdName> tags
) {}

