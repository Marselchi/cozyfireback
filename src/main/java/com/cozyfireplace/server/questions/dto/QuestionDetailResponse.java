package com.cozyfireplace.server.questions.dto;

import com.cozyfireplace.server.answers.dto.AnswerWithRepliesResponse;

import java.util.List;

public record QuestionDetailResponse(
        QuestionResponse question,
        List<AnswerWithRepliesResponse> answers,
        List<Long> pathToAnswer
) {}
