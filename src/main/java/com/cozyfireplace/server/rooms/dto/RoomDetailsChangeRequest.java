package com.cozyfireplace.server.rooms.dto;

import lombok.Value;

@Value
public class RoomDetailsChangeRequest {
    String situation;
    String date;
    String lastSession;
}
