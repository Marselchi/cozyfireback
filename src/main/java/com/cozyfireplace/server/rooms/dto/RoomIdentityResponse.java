package com.cozyfireplace.server.rooms.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RoomIdentityResponse {
    Long id;
    String url;
    String name;
}
