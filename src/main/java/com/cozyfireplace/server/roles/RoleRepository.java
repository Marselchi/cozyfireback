package com.cozyfireplace.server.roles;

import com.cozyfireplace.server.rooms.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    boolean existsByNameAndRoom(String name, Room room);

    Optional<Role> findByNameAndRoom(String name, Room room);

    List<Role> findByRoomId(Long roomId);

    // Роль ищется строго внутри комнаты: чужой roleId отвечает пустым результатом (404),
    // а не изменением/удалением роли из другой комнаты.
    Optional<Role> findByIdAndRoomId(Long id, Long roomId);

    List<Role> findByIdInAndRoomId(Collection<Long> ids, Long roomId);
}
