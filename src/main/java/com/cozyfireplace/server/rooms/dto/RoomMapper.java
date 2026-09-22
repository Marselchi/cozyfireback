package com.cozyfireplace.server.rooms.dto;

import com.cozyfireplace.server.rooms.Room;
import com.cozyfireplace.server.rooms.roomDetails.RoomDetails;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RoomMapper {
    @Mapping(target = "roomId", source = "roomDetails.id")
    @Mapping(target = "currentSituation", source = "roomDetails.situation")
    @Mapping(target = "currentDate", source = "roomDetails.date")
    RoomDetailsResponse toRoomDetailsResponseFromDetails(RoomDetails roomDetails);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "room", ignore = true)
    @Mapping(target = "lastSession", source = "request.lastSession")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateRoomDetailsFromRequest(RoomDetailsChangeRequest request, @MappingTarget RoomDetails details);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "url", ignore = true)
    @Mapping(target = "gameSystem", ignore = true)
    void updateRoomFromRequest(RoomUpdateRequest request, @MappingTarget Room room);
}
