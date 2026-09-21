package com.cozyfireplace.server.rooms.dto;


import com.cozyfireplace.server.rooms.roomDetails.RoomDetails;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Builder
@Value
public class RoomDetailsResponse {
    Long roomId;
    String currentSituation;
    String currentDate;
    String lastSession;
}
