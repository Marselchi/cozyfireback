package com.cozyfireplace.server.notifications.event;

import lombok.Builder;

@Builder
public record QAEvent(
        OperationType type,
        Long roomId,
        String roomName,
        String roomUrl,
        Long questionId,
        Long answerId,
        Long authorAccountId,
        Long replyToId,
        Long replyToAccountId,
        String authorName,
        boolean byDm
) {}
