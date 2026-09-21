package com.cozyfireplace.server.notifications.dto;

import java.util.List;

public record NotificationsCreatedEvent(
        List<NotificationDispatchItem> items
) {}
