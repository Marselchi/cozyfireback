package com.cozyfireplace.server.characters.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.io.Serializable;
import java.util.Set;

/**
 * DTO for {@link com.cozyfireplace.server.characters.Character}
 */
public record CharacterRequest(
        @NotNull(message = "Name cannot be null")
        @NotEmpty(message = "Name cannot be empty")
        @Length(message = "Max length of name is 100", max = 100)
        String name,
        String description,
        String status,
        String content,
        Set<Long> roleIds
) implements Serializable { }
