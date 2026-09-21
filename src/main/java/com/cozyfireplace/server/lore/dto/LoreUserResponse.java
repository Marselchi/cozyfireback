package com.cozyfireplace.server.lore.dto;

import java.util.List;

public record LoreUserResponse(
        Long id,
        String title,
        String description,
        String date,
        String content,
        boolean isAuthor,
        String accountName,
        boolean nonPublic,
        boolean createdByRoomCreator,
        List<IdName> tags,
        Long viewCount
) {}
