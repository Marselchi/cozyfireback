package com.cozyfireplace.server.tags.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TagResponse {
    Long id;
    String name;
}

