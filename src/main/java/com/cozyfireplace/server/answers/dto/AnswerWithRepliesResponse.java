package com.cozyfireplace.server.answers.dto;

import com.cozyfireplace.server.accounts.dto.AccountQuestionDataResponse;
import lombok.Builder;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Builder
public record AnswerWithRepliesResponse(
        Long id, String content, AccountQuestionDataResponse author,
        Instant createdAt, Instant updatedAt, boolean isAdmin, int replyCount,
        List<AnswerWithRepliesResponse> replies
) implements Serializable {}

