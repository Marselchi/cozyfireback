package com.cozyfireplace.server.notifications.event;

import lombok.Builder;

import java.util.Set;

@Builder
public record CharacterEvent(
        OperationType type,
        Long roomId,
        String roomName,
        String roomUrl,
        Long characterId,
        String name,
        Set<Long> roleIds,
        Long authorAccountId,
        Boolean byDm
) {}


