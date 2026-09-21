package com.cozyfireplace.server.auth.refreshToken;

import com.cozyfireplace.server.auth.profile.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByTokenAndActiveTrue(String token);
    void deleteByProfile(Profile profile);

    /**
     * Удаляет все истёкшие или неактивные refresh-токены.
     * Вызывается по крону из RefreshTokenCleanupScheduler.
     * @return количество удалённых записей
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < CURRENT_TIMESTAMP OR rt.active = false")
    int deleteExpiredAndInactiveTokens();
}
