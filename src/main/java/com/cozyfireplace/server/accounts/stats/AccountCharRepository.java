package com.cozyfireplace.server.accounts.stats;


import com.cozyfireplace.server.accounts.AccountChar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountCharRepository extends JpaRepository<AccountChar, Long> {
    @Query("SELECT c FROM AccountChar c LEFT JOIN FETCH c.stats LEFT JOIN FETCH c.skills WHERE c.id = :id")
    Optional<AccountChar> findByIdWithStatsAndSkills(Long id);

    // Комната выводится через аккаунт-владельца: чужой characterId даёт пустой результат
    // (404), а не правку персонажа из другой комнаты.
    @Query("""
        SELECT c FROM AccountChar c
        LEFT JOIN FETCH c.stats
        LEFT JOIN FETCH c.skills
        JOIN Account a ON a.accountChar.id = c.id
        WHERE c.id = :id AND a.room.id = :roomId
    """)
    Optional<AccountChar> findByIdWithStatsAndSkillsInRoom(@Param("id") Long id,
                                                           @Param("roomId") Long roomId);
}