package com.cozyfireplace.server.rooms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;
import lombok.experimental.Accessors;

@Value
public class RoomCreateRequest {

    @NotBlank(message = "Room name is required")
    @Size(min = 3, max = 50, message = "Room name must be between 3 and 50 characters")
    String name;

    String description;

    String url;
}