package com.cozyfireplace.server.accounts;

import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.lore.blocks.dto.NormalAccessRecordResponse;
import com.cozyfireplace.server.rooms.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByProfileAndRoom(Profile profile, Room room);

    @Query("""
            select distinct new com.cozyfireplace.server.lore.blocks.dto.NormalAccessRecordResponse(
                a.id,
                b.id,
                a.id,
                a.name,
                true
            )
            from Block b, Account a
            where b.id = :blockId
              and a.room.id = :roomId
              and (
                    b.roles is empty
                    or exists (
                         select 1
                         from Account a2
                         join a2.roles r
                         where a2 = a
                           and r member of b.roles
                    )
              )
            """)
    List<NormalAccessRecordResponse> findNormalAccess(
            @Param("roomId") Long roomId,
            @Param("blockId") Long blockId
    );

    List<Account> findAllByRoomId(Long roomId);

    @Query("SELECT a.profile.id FROM Account a WHERE a.id = :accountId")
    Optional<Long> findProfileIdByAccountId(@Param("accountId") Long accountId);

    Optional<Account> findByProfileAndRoomId(Profile profile, Long id);

    @Query("SELECT a FROM Account a " +
            "LEFT JOIN FETCH a.roles " +
            "LEFT JOIN FETCH a.profile " +
            "WHERE a.room.id = :roomId " +
            "AND a.id != :currentAccountId")
    List<Account> findAllByRoomIdAndNotId(
            @Param("roomId") Long roomId,
            @Param("currentAccountId") Long currentAccountId);

    List<Account> findByProfile(Profile currentAuthenticatedProfile);

    long countByRoom(Room room);

    @Query("""
                select a.name
                from Account a
                join a.roles r
                where r.id = :roleId
            """)
    List<String> findNamesByRoleId(@Param("roleId") Long roleId);
}
