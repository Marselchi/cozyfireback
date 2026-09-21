package com.cozyfireplace.server.accounts.stats;


import com.cozyfireplace.server.accounts.AccountChar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AccountCharRepository extends JpaRepository<AccountChar, Long> {
    @Query("SELECT c FROM AccountChar c LEFT JOIN FETCH c.stats LEFT JOIN FETCH c.skills WHERE c.id = :id")
    Optional<AccountChar> findByIdWithStatsAndSkills(Long id);
}