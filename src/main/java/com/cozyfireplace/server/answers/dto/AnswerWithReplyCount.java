package com.cozyfireplace.server.answers.dto;

import com.cozyfireplace.server.answers.Answer;

/**
 * DTO для получения ответа вместе с количеством его дочерних ответов.
 * Используется в запросах репозитория.
 */
public record AnswerWithReplyCount(
        Answer answer,
        long replyCount,
        boolean isAdmin
) {
}
