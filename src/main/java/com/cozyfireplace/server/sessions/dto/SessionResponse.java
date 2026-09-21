package com.cozyfireplace.server.sessions.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * DTO для полного ответа по сессии
 */
public record SessionResponse(
        Long id,
        Long creatorId,
        String creatorName,
        Instant time,
        String description,
        List<IdNameBool> participants
) implements Serializable { }
