package com.cozyfireplace.server.templates;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.current.CurrentAccount;
import com.cozyfireplace.server.templates.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
@Tag(name = "Templates", description = "Шаблоны")
public class TemplateController {

    private final TemplateService templateService;

    @Operation(
            summary = "Получить список шаблонов",
            description = "Возвращает пагинированный список шаблонов с возможностью поиска по имени (без контента)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Список шаблонов",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = TemplateResponse.class))))
            }
    )
    @GetMapping
    public ResponseEntity<Page<TemplateResponse>> getAll(
            @Parameter(description = "Поиск по имени")
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(templateService.getAllTemplates(search, pageable));
    }

    @Operation(
            summary = "Получить шаблон по ID",
            description = "Возвращает полную информацию о шаблоне включая контент",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ок",
                            content = @Content(schema = @Schema(implementation = TemplateDetailResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Шаблон не найден", content = @Content)
            }
    )
    @GetMapping("/{templateId}")
    public ResponseEntity<TemplateDetailResponse> getById(
            @Parameter(description = "ID шаблона", required = true)
            @PathVariable Long templateId
    ) {
        return ResponseEntity.ok(templateService.getTemplateById(templateId));
    }

    @Operation(
            summary = "Создать шаблон",
            description = "Создаёт новый шаблон",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Создано"),
                    @ApiResponse(responseCode = "404", description = "Автор не найден", content = @Content)
            }
    )
    @PostMapping("/{roomId}")
    public ResponseEntity<Void> create(
            @Parameter(description = "ID автора", required = true)
            @PathVariable Long roomId,
            @CurrentAccount Account account,
            @Valid @RequestBody TemplateCreateRequest request
    ) {
        templateService.createTemplate(request, account.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Обновить название шаблона",
            description = "Обновляет название шаблона",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Обновлено"),
                    @ApiResponse(responseCode = "404", description = "Шаблон не найден", content = @Content)
            }
    )
    @PutMapping("/{templateId}")
    public ResponseEntity<Void> update(
            @Parameter(description = "ID шаблона", required = true)
            @PathVariable Long templateId,
            @Valid @RequestBody TemplateUpdateRequest request
    ) {
        templateService.updateTemplateName(templateId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Удалить шаблон",
            description = "Удаляет шаблон по ID",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Удалено"),
                    @ApiResponse(responseCode = "404", description = "Шаблон не найден", content = @Content)
            }
    )
    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID шаблона", required = true)
            @PathVariable Long templateId
    ) {
        templateService.deleteTemplate(templateId);
        return ResponseEntity.noContent().build();
    }
}
