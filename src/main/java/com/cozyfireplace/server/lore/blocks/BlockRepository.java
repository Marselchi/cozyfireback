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

    @EntityGraph(attributePaths = {"roles", "chanceConfig", "chanceConfig.skill"})
    List<Block> findAllByIdIn(Collection<Long> ids);

    @Query("""
            select b.lore.account.room.id as roomId,
                   case when b.chanceConfig is not null then true else false end as hasChance
            from Block b
            where b.id = :blockId
            """)
    Optional<BlockAccessMeta> findAccessMetaByBlockId(@Param("blockId") Long blockId);


    @Query("SELECT b FROM Block b LEFT JOIN FETCH b.chanceConfig LEFT JOIN FETCH b.roles WHERE b.id = :id")
    Optional<Block> findByIdWithChanceAndRoles(Long id);

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
                    join b.lore l
                    join l.account a
                    join a.room r
                    where r.id = :roomId
                      and (:loreId is null or l.id = :loreId)
                      and (
                          :search is null
                          or lower(l.title) like concat('%', lower(cast(:search as string)), '%')
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
                    join b.lore l
                    join l.account a
                    join a.room r
                    where r.id = :roomId
                      and (:loreId is null or l.id = :loreId)
                      and (
                          :search is null
                          or lower(l.title) like concat('%', lower(cast(:search as string)), '%')
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
            left join fetch b.chanceConfig
            left join fetch b.roles
            where b.id in :ids
            """)
    List<Block> findBlocksForSummaryByIds(@Param("ids") List<Long> ids);
}