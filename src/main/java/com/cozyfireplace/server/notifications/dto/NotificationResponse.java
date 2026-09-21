package com.cozyfireplace.server.notifications.dto;

import java.time.Instant;

public record NotificationResponse (Long id, Long roomId, String roomUrl, String roomName,
                                    Long accountId, NotificationType type,
                                    NotificationEventCode eventCode, String title,
                                    String body, String route,
                                    Instant readAt, Instant createdAt) {
}
