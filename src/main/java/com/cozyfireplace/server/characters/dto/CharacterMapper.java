package com.cozyfireplace.server.characters.dto;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.characters.Character;
import com.cozyfireplace.server.lore.dto.ExcerptResponse;
import com.cozyfireplace.server.lore.dto.IdName;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CharacterMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "name", source = "request.name")
    Character toEntity(CharacterRequest request, Account account);

    @Mapping(target = "account", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "description", source = "request.description")
    @Mapping(target = "status", source = "request.status")
    @Mapping(target = "content", source = "request.content")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(CharacterRequest request, @MappingTarget Character character);

    // Для редактирования: всё + roles
    @Mapping(target = "accountName", source = "s.accountName")
    @Mapping(target = "createdByRoomCreator", source = "s.createdByRoomCreator")
    @Mapping(target = "roles", source = "roles")
    CharacterResponse toEditResponse(CharacterSummaryRow s, List<IdName> roles);

    // Для списка: без content, без roles — только основная информация
    @Mapping(target = "accountName", source = "s.accountName")
    @Mapping(target = "createdByRoomCreator", source = "s.createdByRoomCreator")
    CharacterListResponse toListResponse(CharacterSummaryRow s);

    // Для просмотра пользователем с excerpts
    @Mapping(target = "isAuthor", source = "isAuthor")
    @Mapping(target = "accountName", source = "s.accountName")
    @Mapping(target = "createdByRoomCreator", source = "s.createdByRoomCreator")
    @Mapping(target = "roles", source = "roles")
    @Mapping(target = "excerpts", source = "excerpts")
    @Mapping(target = "questionCount", source = "questionCount")
    CharacterUserResponse toUserResponse(CharacterSummaryRow s,
                                         String content,
                                         boolean isAuthor,
                                         List<IdName> roles,
                                         List<ExcerptResponse> excerpts,
                                         long questionCount);
}
