package com.cozyfireplace.server.rooms.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class RoomContextResponse {
    Long id;
    String name;
    String description;
    String url;

    // Информация о создателе (только для отображения!)
    CreatorInfo creator;

    // Флаги для UI
    boolean isCurrentUserCreator;

    // Статистика
    long memberCount;
    long questionCount;

    // Вложенный класс для информации о создателе
    @Value
    @Builder
    public static class CreatorInfo {
        Long id;
        String username;
    }
}
