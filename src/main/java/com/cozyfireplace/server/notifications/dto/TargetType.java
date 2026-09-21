package com.cozyfireplace.server.notifications.dto;

import lombok.Getter;

@Getter
public enum TargetType {
    LORE("lore"),
    CHARACTER("character"),
    SESSION("session"),
    QUESTION("question");

    private final String pathPrefix;

    TargetType(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

}
