package com.cozyfireplace.server.rooms.dto;

import jakarta.validation.constraints.Size;
import lombok.Value;


@Value
public class RoomUpdateRequest {

    @Size(max = 50, message = "Room name must not exceed 50 characters")
    String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description;

}
