package com.cozyfireplace.server.lore.dto;

import java.util.Set;

public record LoreListFilter(
        Boolean createdByRoomCreator,
        Set<String> tagNames,
        String title,
        LoreStatus status
) {}
