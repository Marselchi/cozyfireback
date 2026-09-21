package com.cozyfireplace.server.lore.dto;


import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.lore.Lore;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LoreMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "tags", ignore = true)
    Lore toEntity(LoreRequest loreCreateRequest, Account account);


    @Mapping(target = "account", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(LoreRequest loreCreateRequest, @MappingTarget Lore lore);

    // Для редактирования: всё + roles + tags
    @Mapping(target = "title", source = "s.title")
    @Mapping(target = "description", source = "s.description")
    @Mapping(target = "date", source = "s.date")
    @Mapping(target = "content", source = "expandedContent")
    @Mapping(target = "accountName", source = "s.accountName")
    @Mapping(target = "createdByRoomCreator", source = "s.createdByRoomCreator")
    @Mapping(target = "roles", source = "roles")
    @Mapping(target = "tags", source = "tags")
    LoreResponse toEditResponse(LoreSummaryRow s, String expandedContent, List<IdName> roles, List<IdName> tags);

    // Для списка: без content/roles, только tags

    @Mapping(target = "id", source = "s.id")
    @Mapping(target = "title", source = "s.title")
    @Mapping(target = "description", source = "s.description")
    @Mapping(target = "date", source = "s.date")
    @Mapping(target = "accountName", source = "s.accountName")
    @Mapping(target = "createdByRoomCreator", source = "s.createdByRoomCreator")
    @Mapping(target = "tags", source = "tags")
    @Mapping(target = "nonPublic", source = "canSeeAll")
    LoreListResponse toListResponse(LoreSummaryRow s, boolean canSeeAll, List<IdName> tags);

    // Для просмотра пользователем: content + isAuthor + tags
    @Mapping(target = "title", source = "s.title")
    @Mapping(target = "description", source = "s.description")
    @Mapping(target = "date", source = "s.date")
    @Mapping(target = "content", source = "s.content")
    @Mapping(target = "isAuthor", source = "isAuthor")
    @Mapping(target = "accountName", source = "s.accountName")
    @Mapping(target = "createdByRoomCreator", source = "s.createdByRoomCreator")
    @Mapping(target = "tags", source = "tags")
    @Mapping(target = "nonPublic", source = "canSeeAll")
    LoreUserResponse toUserResponse(LoreSummaryRow s, boolean isAuthor, boolean canSeeAll, List<IdName> tags);

    @Mapping(target = "content", source = "content")
    @Mapping(target = "isAuthor", source = "isAuthor")
    @Mapping(target = "nonPublic", source = "nonPublic")
    @Mapping(target = "tags", source = "tags")
    LoreUserResponse toUserResponse(LoreSummaryRow s,
                                    String content,
                                    boolean isAuthor,
                                    boolean nonPublic,
                                    List<IdName> tags);

    @Mapping(target = "id", source = "s.id")
    @Mapping(target = "title", source = "s.title")
    @Mapping(target = "description", source = "s.description")
    @Mapping(target = "date", source = "s.date")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "isAuthor", source = "isAuthor")
    @Mapping(target = "accountName", source = "s.accountName")
    @Mapping(target = "createdByRoomCreator", source = "s.createdByRoomCreator")
    @Mapping(target = "nonPublic", source = "nonPublic")
    @Mapping(target = "tags", source = "tags")
    @Mapping(target = "excerpts", source = "excerpts")
    @Mapping(target = "questionCount", source = "questionCount")
    LoreUserInlineResponse toUserInlineResponse(LoreSummaryRow s,
                                                String content,
                                                boolean isAuthor,
                                                boolean nonPublic,
                                                List<IdName> tags,
                                                List<ExcerptResponse> excerpts,
                                                long questionCount);


}
