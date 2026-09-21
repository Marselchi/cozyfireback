package com.cozyfireplace.server.questions.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuestionUpdateRequest(
        @NotBlank(message = "Title cannot be blank")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,
        
        @NotBlank(message = "Body cannot be blank")
        @Size(max = 2000, message = "Body must not exceed 2000 characters")
        String body,
        
        @Min(value = 0, message = "Category must be between 0 and 6")
        @Max(value = 6, message = "Category must be between 0 and 6")
        Long category
) {
}
