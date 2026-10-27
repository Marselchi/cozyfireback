package com.cozyfireplace.server.invitations;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;


public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    List<Invitation> findAllByRoomId(Long roomId);

    Optional<Invitation> findByCode(String code);

    // Инвайт ищется строго внутри комнаты: чужой invitationId отвечает 404,
    // а не отзывом кода из другой комнаты.
    Optional<Invitation> findByRoomIdAndId(Long roomId, Long id);
}
