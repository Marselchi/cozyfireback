package com.cozyfireplace.server.lore.dto;

import java.util.List;

public record LoreUserInlineResponse(
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
        List<ExcerptResponse> excerpts,
        Long viewCount,
        long questionCount
) {}
