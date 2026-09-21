package com.cozyfireplace.server.characters;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.current.CurrentAccount;
import com.cozyfireplace.server.characters.dto.CharacterListFilter;
import com.cozyfireplace.server.characters.dto.CharacterListResponse;
import com.cozyfireplace.server.characters.dto.CharacterRequest;
import com.cozyfireplace.server.characters.dto.CharacterResponse;
import com.cozyfireplace.server.characters.dto.CharacterUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/characters")
@RequiredArgsConstructor
@Tag(name = "Character", description = "API для управления персонажами")
public class CharacterController {

    private final CharacterService characterService;

    @Operation(
            summary = "Создать персонажа",
            description = "Создаёт персонажа в комнате. Роли передаются id-шниками в теле запроса.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Создано"),
                    @ApiResponse(responseCode = "404", description = "Комната/роль не найдены", content = @Content)
            }
    )
    @PostMapping("/{roomId}")
    public ResponseEntity<Long> create(
            @Parameter(description = "ID комнаты", required = true) @PathVariable Long roomId,
            @Valid @RequestBody CharacterRequest request,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        Long characterId = characterService.createCharacter(roomId, account, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(characterId);
    }

    @Operation(
            summary = "Обновить персонажа",
            description = "Обновляет персонажа. Поддерживается PATCH для частичного обновления.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Обновлено"),
                    @ApiResponse(responseCode = "404", description = "Персонаж/роль не найдены", content = @Content)
            }
    )
    @PatchMapping("/{roomId}/{characterId}")
    public ResponseEntity<Void> update(
            @Parameter(description = "ID комнаты", required = true) @PathVariable Long roomId,
            @Parameter(description = "ID персонажа", required = true) @PathVariable Long characterId,
            @Valid @RequestBody CharacterRequest request,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        characterService.updateCharacter(characterId, request, account, roomId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Удалить персонажа",
            description = "Удаляет персонажа.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Удалено"),
                    @ApiResponse(responseCode = "404", description = "Персонаж не найден", content = @Content)
            }
    )
    @DeleteMapping("/{characterId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID персонажа", required = true) @PathVariable Long characterId
    ) {
        characterService.deleteCharacter(characterId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Список персонажей (пагинация + фильтры)",
            description = "Возвращает страницу CharacterListResponse. Фильтры: name contains, roles ALL, createdByRoomCreator.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ок"),
                    @ApiResponse(responseCode = "404", description = "Комната не найдена", content = @Content)
            }
    )
    @GetMapping("/{roomId}/all")
    public ResponseEntity<Page<CharacterListResponse>> list(
            @Parameter(description = "ID комнаты", required = true) @PathVariable Long roomId,
            @Parameter(description = "Фильтры списка") @ParameterObject CharacterListFilter filter,
            @ParameterObject Pageable pageable,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        return ResponseEntity.ok(characterService.getCharacterList(roomId, account, filter, pageable));
    }

    @Operation(
            summary = "Получить персонажа для редактирования",
            description = "Возвращает CharacterResponse (включая roles).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ок"),
                    @ApiResponse(responseCode = "404", description = "Персонаж не найден или нет доступа", content = @Content)
            }
    )
    @GetMapping("/{roomId}/{characterId}/edit")
    public ResponseEntity<CharacterResponse> getForEdit(
            @Parameter(description = "ID комнаты", required = true) @PathVariable Long roomId,
            @Parameter(description = "ID персонажа", required = true) @PathVariable Long characterId,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        return ResponseEntity.ok(characterService.getCharacterForEdit(roomId, characterId, account));
    }

    @Operation(
            summary = "Получить персонажа для просмотра пользователем (inline)",
            description = "Возвращает CharacterUserResponse с excerpts из lore, на которые есть ссылки в content персонажа.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ок"),
                    @ApiResponse(responseCode = "404", description = "Персонаж не найден или нет доступа", content = @Content)
            }
    )
    @GetMapping("/{roomId}/{characterId}/viewInline")
    public ResponseEntity<CharacterUserResponse> getForUserInline(
            @Parameter(description = "ID комнаты", required = true) @PathVariable Long roomId,
            @Parameter(description = "ID персонажа", required = true) @PathVariable Long characterId,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        CharacterUserResponse response = characterService.getCharacterForUserInline(roomId, characterId, account);
        return ResponseEntity.ok(response);
    }
}
