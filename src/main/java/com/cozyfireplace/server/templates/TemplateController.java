package com.cozyfireplace.server.templates;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.current.CurrentAccount;
import com.cozyfireplace.server.templates.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
@Tag(name = "Templates", description = "Reusable lore/character templates")
public class TemplateController {

    private final TemplateService templateService;

    @Operation(
            summary = "List templates",
            description = "Returns a paginated list of templates with optional name search (without content).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of templates",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = TemplateResponse.class))))
            }
    )
    @GetMapping
    public ResponseEntity<Page<TemplateResponse>> getAll(
            @Parameter(description = "Search by name", example = "npc")
            @RequestParam(required = false) String search,
            @Parameter(hidden = true) @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(templateService.getAllTemplates(search, pageable));
    }

    @Operation(
            summary = "Get a template by ID",
            description = "Returns the full template including its content.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = TemplateDetailResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Template not found", content = @Content)
            }
    )
    @GetMapping("/{templateId}")
    public ResponseEntity<TemplateDetailResponse> getById(
            @Parameter(description = "ID of the template", required = true, example = "2")
            @PathVariable Long templateId
    ) {
        return ResponseEntity.ok(templateService.getTemplateById(templateId));
    }

    @Operation(
            summary = "Create a template",
            description = "Creates a new template authored by the authenticated account. The room in the path scopes which account is used as the author.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Created"),
                    @ApiResponse(responseCode = "403", description = "Forbidden: the account does not belong to this room", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(name = "TemplateCreate", value = """
                    {
                      "name": "NPC - Shopkeeper",
                      "content": "Name:\\nAge:\\nPersonality:"
                    }
                    """))
    )
    @PostMapping("/{roomId}")
    @SuppressWarnings("unused")
    public ResponseEntity<Void> create(
            @Parameter(description = "ID of the room", required = true, example = "1")
            @PathVariable Long roomId,
            @Parameter(hidden = true) @CurrentAccount Account account,
            @Valid @RequestBody TemplateCreateRequest request
    ) {
        templateService.createTemplate(request, account);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Update a template name",
            description = "Updates the name of a template. Only the template's author or the room creator may do it.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Updated"),
                    @ApiResponse(responseCode = "403", description = "Forbidden: neither the author nor the room creator", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Template not found in this room", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(name = "TemplateUpdate", value = """
                    { "name": "NPC - Friendly Shopkeeper" }
                    """))
    )
    @PutMapping("/{roomId}/{templateId}")
    public ResponseEntity<Void> update(
            @Parameter(description = "ID of the room", required = true, example = "1")
            @PathVariable Long roomId,
            @Parameter(description = "ID of the template", required = true, example = "2")
            @PathVariable Long templateId,
            @Parameter(hidden = true) @CurrentAccount Account account,
            @Valid @RequestBody TemplateUpdateRequest request
    ) {
        templateService.updateTemplateName(roomId, templateId, request, account);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Delete a template",
            description = "Deletes a template. Only the template's author or the room creator may do it.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Deleted"),
                    @ApiResponse(responseCode = "403", description = "Forbidden: neither the author nor the room creator", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Template not found in this room", content = @Content)
            }
    )
    @DeleteMapping("/{roomId}/{templateId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID of the room", required = true, example = "1")
            @PathVariable Long roomId,
            @Parameter(description = "ID of the template", required = true, example = "2")
            @PathVariable Long templateId,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        templateService.deleteTemplate(roomId, templateId, account);
        return ResponseEntity.noContent().build();
    }
}
