package com.cozyfireplace.server.lore;

import com.cozyfireplace.server.lore.dto.ContentTitleRow;
import com.cozyfireplace.server.lore.dto.LoreSummaryRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LoreRepository extends JpaRepository<Lore, Long> {

    // ====== 1) IDS page with ALL filters (native, PostgreSQL) ======
    @Query(
            value = """
            select x.lore_id
            from (
                select l.id as lore_id
                from lore l
                join accounts a on a.id = l.account_id
                join rooms r on r.id = a.room_id

                left join lore_tags lt on lt.lore_id = l.id
                left join lore_roles lr on lr.lore_id = l.id
                left join lore_view lv on lv.lore_id = l.id and lv.account_id = :viewerId

                where r.id = :roomId

                  and (:title is null or lower(l.title) like concat('%', lower(:title), '%'))

                  and (
                        :createdByCreator is null
                     or (:createdByCreator = true  and a.id = r.creator_account_id)
                     or (:createdByCreator = false and a.id <> r.creator_account_id)
                  )

                  and (
                        :canSeeAll = true
                     or not exists (select 1 from lore_roles lr0 where lr0.lore_id = l.id)
                     or (:viewerHasRoles = true and lr.role_id = any(:viewerRoleIds))
                  )

                  and (
                        :tagCount = 0
                     or lt.tag_id = any(:tagIds)
                  )

                  and (
                        :status is null
                     or (:status = 'viewed' and lv.viewed = true)
                     or (:status = 'unviewed' and lv.viewed is null)
                     or (:status = 'updated' and lv.viewed = false)
                  )

                group by l.id
                having (
                      :tagCount = 0
                   or count(distinct case when lt.tag_id = any(:tagIds) then lt.tag_id end) = :tagCount
                )
            ) x
            """,
            countQuery = """
            select count(*)
            from (
                select l.id
                from lore l
                join accounts a on a.id = l.account_id
                join rooms r on r.id = a.room_id

                left join lore_tags lt on lt.lore_id = l.id
                left join lore_roles lr on lr.lore_id = l.id
                left join lore_view lv on lv.lore_id = l.id and lv.account_id = :viewerId

                where r.id = :roomId
                  and (:title is null or lower(l.title) like concat('%', lower(:title), '%'))
                  and (
                        :createdByCreator is null
                     or (:createdByCreator = true  and a.id = r.creator_account_id)
                     or (:createdByCreator = false and a.id <> r.creator_account_id)
                  )
                  and (
                        :canSeeAll = true
                     or not exists (select 1 from lore_roles lr0 where lr0.lore_id = l.id)
                     or (:viewerHasRoles = true and lr.role_id = any(:viewerRoleIds))
                  )
                  and (
                        :tagCount = 0
                     or lt.tag_id = any(:tagIds)
                  )
                  and (
                        :status is null
                     or (:status = 'viewed' and lv.viewed = true)
                     or (:status = 'unviewed' and lv.viewed is null)
                     or (:status = 'updated' and lv.viewed = false)
                  )
                group by l.id
                having (
                      :tagCount = 0
                   or count(distinct case when lt.tag_id = any(:tagIds) then lt.tag_id end) = :tagCount
                )
            ) x
            """,
            nativeQuery = true
    )
    Page<Long> findLoreIdsFiltered(@Param("roomId") Long roomId,
                                   @Param("canSeeAll") boolean canSeeAll,
                                   @Param("viewerHasRoles") boolean viewerHasRoles,
                                   @Param("viewerRoleIds") long[] viewerRoleIds,
                                   @Param("createdByCreator") Boolean createdByCreator,
                                   @Param("title") String title,
                                   @Param("tagCount") int tagCount,
                                   @Param("tagIds") long[] tagIds,
                                   @Param("status") String status,
                                   @Param("viewerId") Long viewerId,
                                   Pageable pageable);

    // ====== 2) Summary rows by ids (JPQL projection) ======

    @Query("""
        select
          l.id as id,
          l.title as title,
          l.description as description,
          l.date as date,
          l.content as content,
          a.id as accountId,
          a.name as accountName,
          case when a.id = r.creator.id then true else false end as createdByRoomCreator,
          case when exists (
                select 1 from Lore l2 join l2.roles rr where l2.id = l.id
            ) then true else false end as nonPublic,
          lv.viewed as viewed
        from Lore l
        join l.account a
        join a.room r
        left join LoreView lv on lv.id.loreId = l.id and lv.id.accountId = :viewerId
        where l.id in :ids
    """)
    List<LoreSummaryRow> findSummariesByIds(@Param("ids") List<Long> ids, @Param("viewerId") Long viewerId);

    // Для одного lore (если доступ уже проверен снаружи — просто в рамках комнаты)
    @Query("""
        select
          l.id as id,
          l.title as title,
          l.description as description,
          l.date as date,
          l.content as content,
          a.id as accountId,
          a.name as accountName,
          case when a.id = r.creator.id then true else false end as createdByRoomCreator,
          case when exists (
                select 1 from Lore l2 join l2.roles rr where l2.id = l.id
            ) then true else false end as nonPublic,
          (select count(lv) from LoreView lv where lv.id.loreId = l.id) as viewCount
        from Lore l
        join l.account a
        join a.room r
        where r.id = :roomId and l.id = :loreId
    """)
    Optional<LoreSummaryRow> findSummaryByIdAndRoomId(@Param("roomId") Long roomId,
                                                      @Param("loreId") Long loreId);

    // ====== 3) Batch rows for roles/tags ======

    interface LoreRoleRow {
        Long getLoreId();
        Long getRoleId();
        String getRoleName();
    }

    interface LoreTagRow {
        Long getLoreId();
        Long getTagId();
        String getTagName();
    }

    @Query("""
        select l.id as loreId, r.id as roleId, r.name as roleName
        from Lore l
        join l.roles r
        where l.id in :loreIds
    """)
    List<LoreRoleRow> findRolesByLoreIds(@Param("loreIds") List<Long> loreIds);

    @Query("""
        select l.id as loreId, t.id as tagId, t.name as tagName
        from Lore l
        join l.tags t
        where l.id in :loreIds
    """)
    List<LoreTagRow> findTagsByLoreIds(@Param("loreIds") List<Long> loreIds);

    @Query(value = """
    select exists (
        select 1
        from lore l
        join accounts a on a.id = l.account_id
        join rooms r on r.id = a.room_id
        left join lore_roles lr on lr.lore_id = l.id
        where r.id = :roomId
          and l.id = :loreId
          and (
                :canSeeAll = true
             or not exists (select 1 from lore_roles lr0 where lr0.lore_id = l.id)
             or (:viewerHasRoles = true and lr.role_id = any(:viewerRoleIds))
          )
    )
    """, nativeQuery = true)
    boolean existsVisibleLore(@Param("roomId") Long roomId,
                              @Param("loreId") Long loreId,
                              @Param("canSeeAll") boolean canSeeAll,
                              @Param("viewerHasRoles") boolean viewerHasRoles,
                              @Param("viewerRoleIds") long[] viewerRoleIds);

    @Query("""
        select l from Lore l
        join l.account a
        join a.room r
        where r.id = :roomId and lower(l.title) = lower(:title)
    """)
    Optional<Lore> findByTitleAndRoomId(@Param("title") String title, @Param("roomId") Long roomId);

    @Query("""
        select l from Lore l
        join l.account a
        join a.room r
        where r.id = :roomId
    """)
    List<Lore> findAllByRoomId(@Param("roomId") Long roomId);

    @Query(value = """
    select
      l.id as id,
      l.title as title,
      l.content as content
    from lore l
    join accounts a on a.id = l.account_id
    join rooms r on r.id = a.room_id
    left join lore_roles lr on lr.lore_id = l.id
    
    where r.id = :roomId
      and l.id = any(:loreIds)
      and (
            :canSeeAll = true
         or not exists (select 1 from lore_roles lr0 where lr0.lore_id = l.id)
         or (:viewerHasRoles = true and lr.role_id = any(:viewerRoleIds))
      )
    """, nativeQuery = true)
    List<ContentTitleRow> findContentAndTitleByIdsAndRoomId(@Param("roomId") Long roomId,
                                                            @Param("loreIds") long[] loreIds,
                                                            @Param("canSeeAll") boolean canSeeAll,
                                                            @Param("viewerHasRoles") boolean viewerHasRoles,
                                                            @Param("viewerRoleIds") long[] viewerRoleIds);

}
