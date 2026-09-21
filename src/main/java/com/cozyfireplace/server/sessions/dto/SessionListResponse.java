package com.cozyfireplace.server.sessions.dto;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO для краткого ответа по сессии в списке
 */
public record SessionListResponse(
        Long id,
        Long creatorId,
        String creatorName,
        Instant time,
        String description
) implements Serializable { }
