package com.cozyfireplace.server.rooms.roomDetails;


import com.cozyfireplace.server.rooms.dto.RoomDetailsChangeRequest;
import com.cozyfireplace.server.rooms.dto.RoomDetailsResponse;
import com.cozyfireplace.server.rooms.dto.RoomMapper;
import com.cozyfireplace.server.util.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@RequiredArgsConstructor
@Service
public class RoomDetailsService {

    private final RoomDetailsRepository roomDetailsRepository;
    private final RoomMapper roomMapper;

    public void updateRoomDetails(RoomDetailsChangeRequest request, Long roomId) {
        RoomDetails details = roomDetailsRepository.findByRoomId(roomId)
                .orElseThrow(() -> new NotFoundException("Room", roomId));
        roomMapper.updateRoomDetailsFromRequest(request, details);
        roomDetailsRepository.save(details);
    }

    public RoomDetailsResponse getRoomDetails(Long roomId) {
        RoomDetails details = roomDetailsRepository.findByRoomId(roomId)
                .orElseThrow(() -> new NotFoundException("Room", roomId));
        return roomMapper.toRoomDetailsResponseFromDetails(details);
    }

}
