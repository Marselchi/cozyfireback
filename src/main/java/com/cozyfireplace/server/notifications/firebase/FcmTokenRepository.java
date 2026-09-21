package com.cozyfireplace.server.notifications.firebase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    @Query("SELECT t.token FROM FcmToken t WHERE t.profile.id = :profileId AND t.active = true")
    List<String> findTokensByProfileIdAndActiveTrue(@Param("profileId") Long profileId);

    Optional<FcmToken> findByToken(String token);
}
