package com.cozyfireplace.server.lore.blocks;

import com.cozyfireplace.server.lore.blocks.dto.BlockAccessMeta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, Long> {
    Page<Block> findByLoreId(Long loreId, Pageable pageable);

    List<Block> findByLoreId(Long loreId);

    @EntityGraph(attributePaths = {"roles", "chanceConfig", "chanceConfig.skill"})
    List<Block> findAllByIdIn(Collection<Long> ids);

    @Query("""
            select coalesce(la.room.id, ca.room.id) as roomId,
                   case when b.chanceConfig is not null then true else false end as hasChance
            from Block b
            left join b.lore l
            left join l.account la
            left join b.character c
            left join c.account ca
            where b.id = :blockId
            """)
    Optional<BlockAccessMeta> findAccessMetaByBlockId(@Param("blockId") Long blockId);


    @Query("SELECT b FROM Block b LEFT JOIN FETCH b.chanceConfig LEFT JOIN FETCH b.roles WHERE b.id = :id")
    Optional<Block> findByIdWithChanceAndRoles(Long id);

    // Комната блока выводится из его владельца (lore или character). Чужой blockId даёт
    // пустой результат (404), а не бросок/правку блока из другой комнаты.
    @Query("""
        SELECT b FROM Block b
        LEFT JOIN b.lore l
        LEFT JOIN l.account la
        LEFT JOIN b.character c
        LEFT JOIN c.account ca
        LEFT JOIN FETCH b.chanceConfig
        LEFT JOIN FETCH b.roles
        WHERE b.id = :id
          AND coalesce(la.room.id, ca.room.id) = :roomId
    """)
    Optional<Block> findByIdWithChanceAndRolesInRoom(@Param("id") Long id,
                                                    @Param("roomId") Long roomId);

    @Query("""
                SELECT b FROM Block b
                LEFT JOIN FETCH b.chanceConfig
                LEFT JOIN FETCH b.roles
                WHERE b.id IN :ids
            """)
    List<Block> findAllWithChanceAndRolesByIds(@Param("ids") List<Long> ids);

    @Query(
            value = """
                    select b.id
                    from Block b
                    left join b.lore l
                    left join l.account la
                    left join b.character c
                    left join c.account ca
                    where coalesce(la.room.id, ca.room.id) = :roomId
                      and (:loreId is null or l.id = :loreId)
                      and (
                          :search is null
                          or lower(coalesce(l.title, c.name)) like concat('%', lower(cast(:search as string)), '%')
                      )
                      and (
                          :hasChance is null
                          or (:hasChance = true and b.chanceConfig is not null)
                          or (:hasChance = false and b.chanceConfig is null)
                      )
                    order by b.id desc
                    """,
            countQuery = """
                    select count(b.id)
                    from Block b
                    left join b.lore l
                    left join l.account la
                    left join b.character c
                    left join c.account ca
                    where coalesce(la.room.id, ca.room.id) = :roomId
                      and (:loreId is null or l.id = :loreId)
                      and (
                          :search is null
                          or lower(coalesce(l.title, c.name)) like concat('%', lower(cast(:search as string)), '%')
                      )
                      and (
                          :hasChance is null
                          or (:hasChance = true and b.chanceConfig is not null)
                          or (:hasChance = false and b.chanceConfig is null)
                      )
                    """
    )
    Page<Long> findBlockIdsByRoomId(
            @Param("roomId") Long roomId,
            @Param("loreId") Long loreId,
            @Param("search") String search,
            @Param("hasChance") Boolean hasChance,
            Pageable pageable
    );

    @Query("""
            select distinct b
            from Block b
            left join fetch b.lore
            left join fetch b.character
            left join fetch b.chanceConfig
            left join fetch b.roles
            where b.id in :ids
            """)
    List<Block> findBlocksForSummaryByIds(@Param("ids") List<Long> ids);
}