package com.cozyfireplace.server.tags;

import com.cozyfireplace.server.rooms.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag,Long> {

    interface IdNameRow {
        Long getId();
        String getName();
    }

    @Query("""
        select t.id as id, t.name as name
        from Tag t
        where t.room.id = :roomId
          and t.name in :names
    """)
    List<IdNameRow> findIdNameByRoomIdAndNameIn(@Param("roomId") Long roomId,
                                                @Param("names") Collection<String> names);

    boolean existsByNameAndRoom(String name, Room room);

    List<Tag> findByRoomId(Long roomId);

    // Тег ищется строго внутри комнаты: чужой tagId отвечает пустым результатом (404),
    // а не изменением/удалением тега из другой комнаты.
    Optional<Tag> findByIdAndRoomId(Long id, Long roomId);

    List<Tag> findByIdInAndRoomId(Collection<Long> ids, Long roomId);
}
