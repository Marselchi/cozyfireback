package com.cozyfireplace.server.lore.blocks;

import com.cozyfireplace.server.lore.blocks.dto.AccessRecordResponse;
import com.cozyfireplace.server.lore.blocks.dto.AccessRecordWithAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccessRecordRepository extends JpaRepository<AccessRecord, Long> {
    Page<AccessRecord> findByBlockId(Long blockId, Pageable pageable);

    @Query("SELECT ar AS record, acc.id AS userId, acc.name AS userName " +
            "FROM AccessRecord ar " +
            "LEFT JOIN Account acc ON acc.accountChar.id = ar.accountChar.id " +
            "WHERE ar.id = :id")
    Optional<AccessRecordWithAccount> findByIdWithAccount(@Param("id") Long id);

    Optional<AccessRecord> findByBlockIdAndAccountCharId(Long blockId, Long accountCharId);

    @Query("""
                SELECT ar FROM AccessRecord ar
                JOIN FETCH ar.block
                WHERE ar.block.id IN :blockIds AND ar.accountChar.id = :accountCharId
            """)
    List<AccessRecord> findAllByBlockIdsAndAccountCharId(@Param("blockIds") List<Long> blockIds,
                                                         @Param("accountCharId") Long accountCharId);

    @Query("""
            select new com.cozyfireplace.server.lore.blocks.dto.AccessRecordResponse(
                ar.id,
                b.id,
                a.id,
                a.name,
                ar.status,
                ar.rollValue,
                case
                    when ar.status = com.cozyfireplace.server.lore.blocks.AccessStatus.PASSED
                      or ar.status = com.cozyfireplace.server.lore.blocks.AccessStatus.GRANTED
                    then true
                    else false
                end
            )
            from AccessRecord ar
            join ar.block b,
                 Account a
            where b.id = :blockId
              and a.room.id = :roomId
              and a.accountChar.id = ar.accountChar.id
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
    List<AccessRecordResponse> findChanceAccess(
            @Param("roomId") Long roomId,
            @Param("blockId") Long blockId
    );
}
