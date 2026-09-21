package com.cozyfireplace.server.characters;

import com.cozyfireplace.server.characters.dto.CharacterSummaryRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CharacterRepository extends JpaRepository<Character, Long> {

    // ====== 1) IDS page with ALL filters (native, PostgreSQL) ======
    @Query(
            value = """
            select x.character_id
            from (
                select c.id as character_id
                from characters c
                join accounts a on a.id = c.account_id
                join rooms r on r.id = a.room_id

                left join character_roles cr on cr.character_id = c.id

                where r.id = :roomId

                  and (:name is null or lower(c.name) like concat('%', lower(:name), '%'))

                  and (
                        :createdByCreator is null
                     or (:createdByCreator = true  and a.id = r.creator_account_id)
                     or (:createdByCreator = false and a.id <> r.creator_account_id)
                  )

                  and (
                        :canSeeAll = true
                     or not exists (select 1 from character_roles cr0 where cr0.character_id = c.id)
                     or (:viewerHasRoles = true and cr.role_id = any(:viewerRoleIds))
                  )

                  and (
                        :roleCount = 0
                     or cr.role_id = any(:roleIds)
                  )

                group by c.id
                having (
                      :roleCount = 0
                   or count(distinct case when cr.role_id = any(:roleIds) then cr.role_id end) = :roleCount
                )
            ) x
            """,
            countQuery = """
            select count(*)
            from (
                select c.id
                from characters c
                join accounts a on a.id = c.account_id
                join rooms r on r.id = a.room_id

                left join character_roles cr on cr.character_id = c.id

                where r.id = :roomId
                  and (:name is null or lower(c.name) like concat('%', lower(:name), '%'))
                  and (
                        :createdByCreator is null
                     or (:createdByCreator = true  and a.id = r.creator_account_id)
                     or (:createdByCreator = false and a.id <> r.creator_account_id)
                  )
                  and (
                        :canSeeAll = true
                     or not exists (select 1 from character_roles cr0 where cr0.character_id = c.id)
                     or (:viewerHasRoles = true and cr.role_id = any(:viewerRoleIds))
                  )
                  and (
                        :roleCount = 0
                     or cr.role_id = any(:roleIds)
                  )
                group by c.id
                having (
                      :roleCount = 0
                   or count(distinct case when cr.role_id = any(:roleIds) then cr.role_id end) = :roleCount
                )
            ) x
            """,
            nativeQuery = true
    )
    Page<Long> findCharacterIdsFiltered(@Param("roomId") Long roomId,
                                        @Param("canSeeAll") boolean canSeeAll,
                                        @Param("viewerHasRoles") boolean viewerHasRoles,
                                        @Param("viewerRoleIds") long[] viewerRoleIds,
                                        @Param("createdByCreator") Boolean createdByCreator,
                                        @Param("name") String name,
                                        @Param("roleCount") int roleCount,
                                        @Param("roleIds") long[] roleIds,
                                        Pageable pageable);

    // ====== 2) Summary rows by ids (JPQL projection) ======
    @Query("""
        select
          c.id as id,
          c.name as name,
          c.description as description,
          c.status as status,
          c.content as content,
          a.id as accountId,
          a.name as accountName,
          case when a.id = r.creator.id then true else false end as createdByRoomCreator
        from Character c
        join c.account a
        join a.room r
        where c.id in :ids
    """)
    List<CharacterSummaryRow> findSummariesByIds(@Param("ids") List<Long> ids);

    // Для одного character (если доступ уже проверен снаружи)
    @Query("""
        select
          c.id as id,
          c.name as name,
          c.description as description,
          c.status as status,
          c.content as content,
          a.id as accountId,
          a.name as accountName,
          case when a.id = r.creator.id then true else false end as createdByRoomCreator
        from Character c
        join c.account a
        join a.room r
        where r.id = :roomId and c.id = :characterId
    """)
    Optional<CharacterSummaryRow> findSummaryByIdAndRoomId(@Param("roomId") Long roomId,
                                                           @Param("characterId") Long characterId);

    // ====== 3) Batch rows for roles ======
    interface CharacterRoleRow {
        Long getCharacterId();
        Long getRoleId();
        String getRoleName();
    }

    @Query("""
        select c.id as characterId, r.id as roleId, r.name as roleName
        from Character c
        join c.roles r
        where c.id in :characterIds
    """)
    List<CharacterRoleRow> findRolesByCharacterIds(@Param("characterIds") List<Long> characterIds);

    @Query(value = """
    select exists (
        select 1
        from characters c
        join accounts a on a.id = c.account_id
        join rooms r on r.id = a.room_id
        left join character_roles cr on cr.character_id = c.id
        where r.id = :roomId
          and c.id = :characterId
          and (
                :canSeeAll = true
             or not exists (select 1 from character_roles cr0 where cr0.character_id = c.id)
             or (:viewerHasRoles = true and cr.role_id = any(:viewerRoleIds))
          )
    )
    """, nativeQuery = true)
    boolean existsVisibleCharacter(@Param("roomId") Long roomId,
                                   @Param("characterId") Long characterId,
                                   @Param("canSeeAll") boolean canSeeAll,
                                   @Param("viewerHasRoles") boolean viewerHasRoles,
                                   @Param("viewerRoleIds") long[] viewerRoleIds);

}
