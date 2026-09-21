package com.cozyfireplace.server.answers.dto;

import com.cozyfireplace.server.answers.Answer;

public record AnswerWithAdminFlag(Answer answer, boolean isAdmin) {}
