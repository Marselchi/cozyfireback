package com.cozyfireplace.server.rooms.dto;

public record LoreExportRequest(
        boolean saveTags,
        boolean dmOnly,
        boolean saveSpoilers
) {
}
