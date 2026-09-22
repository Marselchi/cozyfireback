package com.cozyfireplace.server.answers.dto;

import com.cozyfireplace.server.accounts.dto.AccountMapper;
import com.cozyfireplace.server.answers.Answer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {AccountMapper.class})
public interface AnswerMapper {

    @Mapping(target = "author", source = "answer.author")
    @Mapping(target = "replyCount", source = "replyCount")
    AnswerResponse toAnswerResponse(Answer answer, long replyCount, boolean isAdmin);

    @Mapping(target = "isAdmin", ignore = true)
    @Mapping(target = "replyCount", ignore = true)
    AnswerResponse toAnswerResponse(Answer answer);

    @Mapping(target = "isAdmin", source = "isAdmin")
    @Mapping(target = "updatedAt", source = "answer.updatedAt")
    @Mapping(target = "id", source = "answer.id")
    @Mapping(target = "createdAt", source = "answer.createdAt")
    @Mapping(target = "content", source = "answer.content")
    @Mapping(target = "author", source = "answer.author")
    @Mapping(target = "replyCount", source = "answerWithReplyCount.replyCount")
    AnswerResponse toAnswerResponse(AnswerWithReplyCount answerWithReplyCount);

    @Mapping(target = "isAdmin", source = "isAdmin")
    @Mapping(target = "updatedAt", source = "answer.updatedAt")
    @Mapping(target = "id", source = "answer.id")
    @Mapping(target = "createdAt", source = "answer.createdAt")
    @Mapping(target = "content", source = "answer.content")
    @Mapping(target = "author", source = "answer.author")
    @Mapping(target = "replyCount", source = "answerWithReplyCount.replyCount")
    @Mapping(target = "replies", ignore = true)
    AnswerWithRepliesResponse toAnswerWithRepliesResponse(AnswerWithReplyCount answerWithReplyCount);

    List<AnswerResponse> toAnswerResponseList(List<AnswerWithReplyCount> answerWithReplyCounts);

    List<AnswerResponse> toAnswerResponseListWithReplyCount(List<AnswerWithReplyCount> answerWithReplyCounts);
}
