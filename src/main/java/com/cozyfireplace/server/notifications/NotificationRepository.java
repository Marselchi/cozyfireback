package com.cozyfireplace.server.notifications;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByAccountIdOrderByCreatedAtDesc(Long accountId);

    long countByAccountIdAndReadAtNull(Long accountId);

    Optional<Notification> findByIdAndAccountId(Long id, Long accountId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.account.id = :accountId AND n.readAt IS NULL")
    void markAllAsRead(@Param("accountId") Long accountId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.account.id = :accountId")
    void deleteAllByAccountId(@Param("accountId") Long accountId);

}
