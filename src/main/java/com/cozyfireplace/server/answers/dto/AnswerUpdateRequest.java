package com.cozyfireplace.server.answers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnswerUpdateRequest(
        @NotBlank(message = "Content cannot be blank")
        @Size(max = 2000, message = "Content must not exceed 2000 characters")
        String content
) {
}
