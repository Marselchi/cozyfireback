package com.cozyfireplace.server.lore;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.current.CurrentAccount;
import com.cozyfireplace.server.lore.dto.*;

import com.cozyfireplace.server.lore.search.LoreSearchPageResponse;
import com.cozyfireplace.server.lore.search.LoreSearchRequest;
import com.cozyfireplace.server.lore.search.LoreSearchService;
import com.cozyfireplace.server.loreViews.LoreViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lore")
@RequiredArgsConstructor
@Tag(name = "Lore", description = "лень")
public class LoreController {

    private final LoreService loreService;
    private final LoreSearchService loreSearchService;
    private final LoreViewService loreViewService;

    @Operation(
            summary = "Создать лор",
            description = "Создаёт лор в комнате. Роли/теги передаются id-шниками в теле запроса.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Создано"),
                    @ApiResponse(responseCode = "404", description = "Комната/роль/тег не найдены", content = @Content)
            }
    )
    @PostMapping("/{roomId}")
    public ResponseEntity<Long> create(
            @Parameter(description = "ID комнаты", required = true) @PathVariable Long roomId,
            @Valid @RequestBody LoreRequest request,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        Long loreId = loreService.createLore(roomId, account, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(loreId);
    }

    @Operation(
            summary = "Обновить лор",
            description = "Обновляет лор. Идентификатор лора передаётся в query параметре loreId (без /{loreId} в path).",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Обновлено"),
                    @ApiResponse(responseCode = "404", description = "Лор/роль/тег не найдены", content = @Content)
            }
    )
    //todo: проверочки
    @PutMapping("/{roomId}/{loreId}")
    public ResponseEntity<Void> update(
            @Parameter(description = "ID комнаты", required = true) @PathVariable Long roomId,
            @Parameter(description = "ID лора", required = true) @PathVariable Long loreId,
            @Valid @RequestBody LoreRequest request,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        loreService.updateLore(loreId, request, account, roomId);
        loreViewService.loreUpdated(loreId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Удалить лор",
            description = "Удаляет лор. Идентификатор лора передаётся в query параметре loreId (без /{loreId} в path).",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Удалено"),
                    @ApiResponse(responseCode = "404", description = "Лор не найден", content = @Content)
            }
    )
    //todo: maybe check rights but to lazy rn
    @DeleteMapping("/{loreId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID лора", required = true) @PathVariable Long loreId
    ) {
        loreService.deleteLore(loreId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Список лора (пагинация + фильтры)",
            description = "Возвращает страницу LoreListResponse. Фильтры: title contains, tags ALL, createdByRoomCreator.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ок"),
                    @ApiResponse(responseCode = "404", description = "Комната не найдена", content = @Content)
            }
    )
    @GetMapping("/{roomId}/all")
    public ResponseEntity<Page<LoreListResponse>> list(
            @Parameter(description = "ID комнаты", required = true) @PathVariable Long roomId,
            @Parameter(description = "Фильтры списка") @ParameterObject LoreListFilter filter,
            @ParameterObject Pageable pageable,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        return ResponseEntity.ok(loreService.getLoreList(roomId, account, filter, pageable));
    }

    @Operation(
            summary = "Получить лор для редактирования",
            description = "Возвращает LoreResponse (включая roles+tags). loreId передаётся query-параметром.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ок"),
                    @ApiResponse(responseCode = "404", description = "Лор не найден или нет доступа", content = @Content)
            }
    )
    @GetMapping("/{roomId}/{loreId}/edit")
    public ResponseEntity<LoreResponse> getForEdit(
            @Parameter(description = "ID комнаты", required = true) @PathVariable Long roomId,
            @Parameter(description = "ID лора", required = true) @PathVariable Long loreId,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        return ResponseEntity.ok(loreService.getLoreForEdit(roomId, loreId, account));
    }

    @Operation(
            summary = "Получить лор для просмотра пользователем",
            description = "Возвращает LoreUserResponse (без roles, но с tags и флагом isAuthor). loreId передаётся query-параметром.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ок"),
                    @ApiResponse(responseCode = "404", description = "Лор не найден или нет доступа", content = @Content)
            }
    )
    @GetMapping("/{roomId}/{loreId}/view")
    public ResponseEntity<LoreUserResponse> getForUser(
            @Parameter(description = "ID комнаты", required = true) @PathVariable Long roomId,
            @Parameter(description = "ID лора", required = true) @PathVariable Long loreId,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        LoreUserResponse response = loreService.getLoreForUser(roomId, loreId, account);
        loreViewService.setViewed(account.getId(), loreId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{roomId}/full-search")
    public LoreSearchPageResponse search(
            @RequestParam String query,
            @PathVariable Long roomId,
            @RequestParam int size,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Boolean byDm,
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam(required = false) String status,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        LoreSearchRequest request = LoreSearchRequest.builder()
                .query(query)
                .roomId(roomId)
                .size(size)
                .offset(offset)
                .title(title)
                .createdByCreator(byDm)
                .tagIds(tagIds)
                .status(status)
                .build();

        return loreSearchService.search(request, account);
    }

}
