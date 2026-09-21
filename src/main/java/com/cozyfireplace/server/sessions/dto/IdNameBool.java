package com.cozyfireplace.server.sessions.dto;

import java.io.Serializable;

/**
 * DTO для передачи id, name и статуса accepted участника
 */
public record IdNameBool(
        Long id,
        String name,
        Boolean accepted
) implements Serializable { }
