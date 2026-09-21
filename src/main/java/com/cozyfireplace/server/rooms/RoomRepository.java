package com.cozyfireplace.server.rooms;

import com.cozyfireplace.server.rooms.dto.RoomTextResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByUrl(String url);

    @Query("SELECT r.url as roomUrl, r.name as roomName FROM Room r WHERE r.id = :id")
    Optional<RoomTextResponse> findRoomUrlById(@Param("id") Long id);
}
