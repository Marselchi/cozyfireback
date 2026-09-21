package com.cozyfireplace.server.questions.dto;

import com.cozyfireplace.server.accounts.dto.AccountQuestionDataResponse;
import com.cozyfireplace.server.answers.dto.AnswerResponse;
import com.cozyfireplace.server.lore.dto.IdName;

import java.io.Serializable;
import java.time.Instant;

public record QuestionListResponse(
        Long id,
        String title,
        String body,
        AccountQuestionDataResponse author,
        Instant createdAt,
        Instant updatedAt,
        Boolean isAnswered,
        Integer category,
        IdName lore,
        IdName character
) implements Serializable {
}