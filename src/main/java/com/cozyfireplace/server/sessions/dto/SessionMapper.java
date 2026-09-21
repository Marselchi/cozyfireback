package com.cozyfireplace.server.sessions.dto;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.sessions.Session;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SessionMapper {
    
    /**
     * Маппинг DTO -> Entity для создания сессии
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creator", source = "account")
    Session toEntity(SessionRequest request, Account account);

    /**
     * Маппинг Entity -> Response для полного ответа
     */
    @Mapping(target = "creatorId", source = "session.creator.id")
    @Mapping(target = "creatorName", source = "session.creator.name")
    SessionResponse toResponse(Session session, List<IdNameBool> participants);

    /**
     * Маппинг Entity -> ListResponse для краткого ответа в списке
     */
    @Mapping(target = "creatorId", source = "session.creator.id")
    @Mapping(target = "creatorName", source = "session.creator.name")
    SessionListResponse toListResponse(Session session);
}
