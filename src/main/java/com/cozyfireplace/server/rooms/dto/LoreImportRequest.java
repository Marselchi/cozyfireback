package com.cozyfireplace.server.rooms.dto;

public record LoreImportRequest(
        String filetype,
        boolean autolink,
        boolean roomReset,
        boolean replaceOnConflict
) {
}
