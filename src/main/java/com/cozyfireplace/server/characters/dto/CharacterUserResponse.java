package com.cozyfireplace.server.characters.dto;

import com.cozyfireplace.server.lore.dto.ExcerptResponse;
import com.cozyfireplace.server.lore.dto.IdName;

import java.util.List;

public record CharacterUserResponse(
        Long id,
        String name,
        String description,
        String status,
        String content,
        boolean isAuthor,
        String accountName,
        boolean createdByRoomCreator,
        List<IdName> roles,
        List<ExcerptResponse> excerpts,
        long questionCount
) {}
