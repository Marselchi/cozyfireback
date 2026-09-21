package com.cozyfireplace.server.notifications.event;

import lombok.Builder;

import java.util.Set;

@Builder
public record LoreEvent(
        OperationType type,
        Long roomId,
        Long loreId,
        String roomName,
        String roomUrl,
        Set<Long> roleIds,
        String title,
        Long authorAccountId,
        Boolean byDm
) {}

