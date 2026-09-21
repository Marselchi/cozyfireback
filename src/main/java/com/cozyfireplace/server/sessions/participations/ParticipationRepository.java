package com.cozyfireplace.server.sessions.participations;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ParticipationRepository extends JpaRepository<Participation, ParticipationId> {

    /**
     * Найти все участия по sessionId
     */
    @Query("SELECT p FROM Participation p WHERE p.id.sessionId = :sessionId")
    List<Participation> findAllByIdSessionId(@Param("sessionId") Long sessionId);
}
