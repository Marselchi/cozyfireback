package com.cozyfireplace.server.answers.dto;

import com.cozyfireplace.server.accounts.dto.AccountQuestionDataResponse;
import lombok.Builder;

import java.io.Serializable;
import java.time.Instant;

@Builder
public record AnswerResponse(
        Long id,
        String content,
        AccountQuestionDataResponse author,
        Instant createdAt,
        Instant updatedAt,
        boolean isAdmin,
        int replyCount
) implements Serializable {
}