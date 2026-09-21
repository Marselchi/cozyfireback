package com.cozyfireplace.server.notifications.dto;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.notifications.Notification;
import com.cozyfireplace.server.notifications.NotificationPreference;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(source = "account.id", target = "accountId")
    @Mapping(source = "account.room.id", target = "roomId")
    @Mapping(source = "account.room.url", target = "roomName")
    NotificationResponse toDto(Notification entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Notification toEntity(RoomNotificationCommand command, String route);

    NotificationPreferenceRequest toRequestDto(NotificationPreference notificationPreference);
}