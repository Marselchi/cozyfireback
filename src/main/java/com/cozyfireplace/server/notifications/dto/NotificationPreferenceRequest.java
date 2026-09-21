package com.cozyfireplace.server.notifications.dto;

public record NotificationPreferenceRequest (
    NotificationEventCode eventCode,
    NotificationChannel channel,
    boolean enabled)
{}
