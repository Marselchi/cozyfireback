package com.cozyfireplace.server.questions.dto;

import com.cozyfireplace.server.accounts.dto.AccountMapper;
import com.cozyfireplace.server.answers.dto.AnswerMapper;
import com.cozyfireplace.server.characters.Character;
import com.cozyfireplace.server.lore.Lore;
import com.cozyfireplace.server.lore.dto.IdName;
import com.cozyfireplace.server.questions.Question;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;


@Mapper(componentModel = "spring", uses = {AccountMapper.class, AnswerMapper.class})
public interface QuestionMapper {

    @Mapping(target = "author", source = "author")
    @Mapping(target = "lore", source = "lore", qualifiedByName = "loreToIdName")
    @Mapping(target = "character", source = "character", qualifiedByName = "characterToIdName")
    @Mapping(target = "answers", source = "answers")
    QuestionResponse toQuestionResponse(Question question);

    @Mapping(target = "author", source = "author")
    @Mapping(target = "lore", source = "lore", qualifiedByName = "loreToIdName")
    @Mapping(target = "character", source = "character", qualifiedByName = "characterToIdName")
    QuestionListResponse toQuestionListResponse(Question question);

    @Named("loreToIdName")
    default IdName loreToIdName(Lore lore) {
        if (lore == null) return null;
        return new IdName(lore.getId(), lore.getTitle());
    }

    @Named("characterToIdName")
    default IdName characterToIdName(Character character) {
        if (character == null) return null;
        return new IdName(character.getId(), character.getName());
    }

    @Mapping(target = "title", source = "request.title")
    @Mapping(target = "body", source = "request.body")
    @Mapping(target = "category", source = "request.category")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isAnswered", ignore = true)
    @Mapping(target = "lore", ignore = true)
    @Mapping(target = "character", ignore = true)
    @Mapping(target = "answers", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateQuestionFromDto(QuestionUpdateRequest request, @MappingTarget Question question);

}
