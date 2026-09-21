package com.cozyfireplace.server.notifications.dto;

import lombok.Builder;

import java.util.Set;

@Builder
public record RoomNotificationCommand(
        Long recipientId,
        String roomName,
        String roomUrl,
        Long roomId,
        Long authorAccountId,
        NotificationType type,
        NotificationEventCode eventCode,
        String title,
        String body,
        Long targetId,
        Set<Long> roleIds,
        TargetType targetType,
        Long answerId //edge-case
        ) {}
