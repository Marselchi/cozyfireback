package com.cozyfireplace.server.rooms.roomDetails;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomDetailsRepository extends JpaRepository<RoomDetails, Long> {
    Optional<RoomDetails> findByRoomName(String roomName);
    Optional<RoomDetails> findByRoomId(Long roomId);
}
