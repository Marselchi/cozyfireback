package com.cozyfireplace.server.lore.dto;

import lombok.Builder;

@Builder
public record IdName(Long id, String name) {
}

