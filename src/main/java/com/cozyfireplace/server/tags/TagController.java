package com.cozyfireplace.server.tags;

import com.cozyfireplace.server.tags.dto.TagCreateRequest;
import com.cozyfireplace.server.tags.dto.TagResponse;
import com.cozyfireplace.server.tags.dto.TagUpdateRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
@Tag(name = "Tags", description = "Теги лора")
public class TagController {

    private final TagService tagService;

    @Operation(
            summary = "Создать тег в комнате",
            description = "Создаёт тег в комнате roomId",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Создано"),
                    @ApiResponse(responseCode = "404", description = "Комната не найдена", content = @Content),
                    @ApiResponse(responseCode = "409", description = "Тег уже существует", content = @Content)
            }
    )
    @PostMapping("/{roomId}")
    public ResponseEntity<Void> create(
            @Parameter(description = "ID комнаты", required = true)
            @PathVariable Long roomId,
            @Valid @RequestBody TagCreateRequest request
    ) {
        tagService.createTag(roomId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Обновить тег в комнате",
            description = "Обновляет тег",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Обновлено"),
                    @ApiResponse(responseCode = "404", description = "Тег не найден", content = @Content)
            }
    )
    @PutMapping("/{roomId}/{tagId}")
    public ResponseEntity<Void> update(
            @Parameter(description = "ID комнаты", required = true)
            @PathVariable Long roomId,
            @Parameter(description = "ID тега", required = true)
            @PathVariable Long tagId,
            @Valid @RequestBody TagUpdateRequest request
    ) {
        tagService.updateTag(tagId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Удалить тег в комнате",
            description = "Удаляет тег",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Удалено"),
                    @ApiResponse(responseCode = "404", description = "Тег не найден", content = @Content)
            }
    )
    @DeleteMapping("/{roomId}/{tagId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID комнаты", required = true)
            @PathVariable Long roomId,
            @Parameter(description = "ID тега", required = true)
            @PathVariable Long tagId
    ) {
        tagService.deleteTag(tagId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Получить тег",
            description = "Возвращает TagResponse.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ок",
                            content = @Content(schema = @Schema(implementation = TagResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Тег не найден", content = @Content)
            }
    )
    @GetMapping("/{roomId}/{tagId}")
    public ResponseEntity<TagResponse> getTag(
            @Parameter(description = "ID комнаты", required = true)
            @PathVariable Long roomId,
            @Parameter(description = "ID тега", required = true)
            @PathVariable Long tagId
    ) {
        return ResponseEntity.ok(tagService.getTag(tagId));
    }

    @Operation(
            summary = "Получить все теги комнаты",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Список тегов",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = TagResponse.class))))
            }
    )
    @GetMapping("/{roomId}/all")
    public ResponseEntity<List<TagResponse>> getAll(
            @Parameter(description = "ID комнаты", required = true)
            @PathVariable Long roomId
    ) {
        return ResponseEntity.ok(tagService.getAllRoomTags(roomId));
    }

}
