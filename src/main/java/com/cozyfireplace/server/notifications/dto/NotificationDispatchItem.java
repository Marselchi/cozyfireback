package com.cozyfireplace.server.notifications.dto;

import java.util.EnumSet;

public record NotificationDispatchItem(
        Long accountId,
        NotificationResponse notification,
        EnumSet<NotificationChannel> channels
) {}
