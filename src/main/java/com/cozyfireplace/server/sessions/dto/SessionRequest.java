package com.cozyfireplace.server.sessions.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.Instant;
import java.util.Set;

/**
 * DTO для создания/редактирования сессии
 */
public record SessionRequest(
        @NotNull(message = "Time cannot be null")
        Instant time,

        String description,

        Set<Long> accountIds
) implements Serializable { }
