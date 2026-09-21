package com.cozyfireplace.server.sessions;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {

    /**
     * Найти все сессии комнаты за период
     */
    @Query("SELECT s FROM Session s WHERE s.creator.room.id = :roomId AND s.time BETWEEN :start AND :end ORDER BY s.time ASC")
    List<Session> findByRoomIdAndTimeBetween(@Param("roomId") Long roomId, @Param("start") Instant start, @Param("end") Instant end);

    /**
     * Найти все сессии комнаты текущего месяца
     */
    @Query("SELECT s FROM Session s WHERE s.creator.room.id = :roomId AND s.time >= :startOfMonth AND s.time < :startOfNextMonth ORDER BY s.time ASC")
    List<Session> findByRoomIdAndCurrentMonth(@Param("roomId") Long roomId, @Param("startOfMonth") Instant startOfMonth, @Param("startOfNextMonth") Instant startOfNextMonth);

    /**
     * Найти будущие сессии (от текущей даты до месяца вперёд) где в приглашенных есть автор запроса
     */
    @Query("SELECT s FROM Session s JOIN Participation p ON s.id = p.id.sessionId WHERE s.creator.room.id = :roomId AND s.time >= :now AND s.time <= :oneMonthLater AND p.id.accountId = :accountId ORDER BY s.time ASC")
    List<Session> findUpcomingWithParticipant(@Param("roomId") Long roomId, @Param("now") Instant now, @Param("oneMonthLater") Instant oneMonthLater, @Param("accountId") Long accountId);

    /**
     * Найти все будущие сессии комнаты до месяца вперёд
     */
    @Query("SELECT s FROM Session s WHERE s.creator.room.id = :roomId AND s.time >= :now AND s.time <= :oneMonthLater ORDER BY s.time ASC")
    List<Session> findUpcomingByRoom(@Param("roomId") Long roomId, @Param("now") Instant now, @Param("oneMonthLater") Instant oneMonthLater);
}
