package com.cozyfireplace.server.lore.dto;

import java.util.List;

public record LoreListResponse(
        Long id,
        String title,
        String description,
        String date,
        String accountName,
        boolean createdByRoomCreator,
        boolean nonPublic,
        Boolean viewed,
        List<IdName> tags
) {}