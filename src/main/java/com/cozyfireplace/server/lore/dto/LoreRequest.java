package com.cozyfireplace.server.lore.dto;

import com.cozyfireplace.server.lore.Lore;
import com.cozyfireplace.server.lore.blocks.dto.BlockChangesRequest;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.io.Serializable;
import java.util.Set;

/**
 * DTO for {@link Lore}
 */
public record LoreRequest(
        @NotNull(message = "Title cannot be null")
        @NotEmpty(message = "Title cannot be empty")
        @Length(message = "Max length of title is 100", max = 100)
        String title,
        String description,
        String date,
        @NotNull(message = "Content cannot be null")
        @NotEmpty(message = "Content cannot be empty")
        String content,
        BlockChangesRequest blockChanges,
        Set<Long> roleIds,
        Set<Long> tagIds)
        implements Serializable {
}