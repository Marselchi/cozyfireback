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
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
@Tag(name = "Lore", description = "Manage lore entries in a room: CRUD, listing, viewing and full-text search")
public class LoreController {

    private final LoreService loreService;
    private final LoreSearchService loreSearchService;
    private final LoreViewService loreViewService;

    @Operation(
            summary = "Create lore",
            description = "Creates a lore entry in the room. Roles/tags are passed as sets of IDs in the request body.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created",
                            content = @Content(examples = @ExampleObject(name = "loreId", value = "23"))),
                    @ApiResponse(responseCode = "404", description = "Room/role/tag not found", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(name = "LoreCreate", value = """
                    {
                      "title": "The Broken Compass",
                      "description": "A relic of the northern expeditions.",
                      "date": "Year of the Amber Moon",
                      "content": "<p>Once it pointed true; now it spins.</p>",
                      "roleIds": [3, 8],
                      "tagIds": [1, 4]
                    }
                    """))
    )
    @PostMapping("/{roomId}")
    public ResponseEntity<Long> create(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Valid @RequestBody LoreRequest request,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        Long loreId = loreService.createLore(roomId, account, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(loreId);
    }

    @Operation(
            summary = "Update lore",
            description = "Updates a lore entry. The lore identifier is provided as the `loreId` path variable.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Updated"),
                    @ApiResponse(responseCode = "404", description = "Lore/role/tag not found", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(name = "LoreUpdate", value = """
                    {
                      "title": "The Broken Compass (Recovered)",
                      "description": "Now restored by the guild.",
                      "date": "Year of the Amber Moon",
                      "content": "<p>It points home again.</p>",
                      "roleIds": [3],
                      "tagIds": [1]
                    }
                    """))
    )
    //todo: проверочки
    @PutMapping("/{roomId}/{loreId}")
    public ResponseEntity<Void> update(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(description = "ID of the lore entry", required = true, example = "23") @PathVariable Long loreId,
            @Valid @RequestBody LoreRequest request,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        loreService.updateLore(loreId, request, account, roomId);
        loreViewService.loreUpdated(loreId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Delete lore",
            description = "Deletes a lore entry by its ID.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Deleted"),
                    @ApiResponse(responseCode = "404", description = "Lore not found", content = @Content)
            }
    )
    //todo: maybe check rights but to lazy rn
    @DeleteMapping("/{loreId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID of the lore entry", required = true, example = "23") @PathVariable Long loreId
    ) {
        loreService.deleteLore(loreId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "List lore (pagination + filters)",
            description = "Returns a page of LoreListResponse. Filters: title contains, tags ALL, createdByRoomCreator.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Room not found", content = @Content)
            }
    )
    @GetMapping("/{roomId}/all")
    public ResponseEntity<Page<LoreListResponse>> list(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(description = "List filters") @ParameterObject LoreListFilter filter,
            @ParameterObject Pageable pageable,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        return ResponseEntity.ok(loreService.getLoreList(roomId, account, filter, pageable));
    }

    @Operation(
            summary = "Get lore for editing",
            description = "Returns a LoreResponse (including roles + tags).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Lore not found or no access", content = @Content)
            }
    )
    @GetMapping("/{roomId}/{loreId}/edit")
    public ResponseEntity<LoreResponse> getForEdit(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(description = "ID of the lore entry", required = true, example = "23") @PathVariable Long loreId,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        return ResponseEntity.ok(loreService.getLoreForEdit(roomId, loreId, account));
    }

    @Operation(
            summary = "Get lore for user viewing",
            description = "Returns a LoreUserResponse (no roles, but with tags and an isAuthor flag). Also records the view.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Lore not found or no access", content = @Content)
            }
    )
    @GetMapping("/{roomId}/{loreId}/view")
    public ResponseEntity<LoreUserResponse> getForUser(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(description = "ID of the lore entry", required = true, example = "23") @PathVariable Long loreId,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        LoreUserResponse response = loreService.getLoreForUser(roomId, loreId, account);
        loreViewService.setViewed(account.getId(), loreId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Full-text search lore",
            description = "Searches the room's lore by full-text query with optional title, author, tag and status filters. Returns a paginated result with highlighted matches.",
            responses = @ApiResponse(responseCode = "200", description = "Search results returned")
    )
    @GetMapping("/{roomId}/full-search")
    public LoreSearchPageResponse search(
            @Parameter(description = "Full-text query", required = true, example = "compass") @RequestParam String query,
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(description = "Page size", required = true, example = "20") @RequestParam int size,
            @Parameter(description = "Result offset", example = "0") @RequestParam(defaultValue = "0") int offset,
            @Parameter(description = "Filter by title substring", example = "Broken") @RequestParam(required = false) String title,
            @Parameter(description = "Restrict to entries created by the room DM/creator") @RequestParam(required = false) Boolean byDm,
            @Parameter(description = "Filter by tag IDs") @RequestParam(required = false) List<Long> tagIds,
            @Parameter(description = "Filter by status", example = "published") @RequestParam(required = false) String status,
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
