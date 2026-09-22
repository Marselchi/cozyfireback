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
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
@Tag(name = "Characters", description = "Manage characters belonging to a room")
public class CharacterController {

    private final CharacterService characterService;

    @Operation(
            summary = "Create a character",
            description = "Creates a character in the room. Roles are passed as a set of IDs in the request body.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created",
                            content = @Content(examples = @ExampleObject(name = "characterId", value = "15"))),
                    @ApiResponse(responseCode = "404", description = "Room/role not found", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(name = "CharacterCreate", value = """
                    {
                      "name": "Kaera Dawnstrider",
                      "description": "A wandering cartographer.",
                      "status": "alive",
                      "content": "Linked lore: [[The Broken Compass]]",
                      "roleIds": [3, 8]
                    }
                    """))
    )
    @PostMapping("/{roomId}")
    public ResponseEntity<Long> create(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Valid @RequestBody CharacterRequest request,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        Long characterId = characterService.createCharacter(roomId, account, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(characterId);
    }

    @Operation(
            summary = "Update a character",
            description = "Updates a character. Partial (PATCH) updates are supported.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Updated"),
                    @ApiResponse(responseCode = "404", description = "Character/role not found", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(name = "CharacterUpdate", value = """
                    {
                      "name": "Kaera Dawnstrider",
                      "status": "missing",
                      "roleIds": [3]
                    }
                    """))
    )
    @PatchMapping("/{roomId}/{characterId}")
    public ResponseEntity<Void> update(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(description = "ID of the character", required = true, example = "15") @PathVariable Long characterId,
            @Valid @RequestBody CharacterRequest request,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        characterService.updateCharacter(characterId, request, account, roomId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Delete a character",
            description = "Deletes a character.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Deleted"),
                    @ApiResponse(responseCode = "404", description = "Character not found", content = @Content)
            }
    )
    @DeleteMapping("/{characterId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID of the character", required = true, example = "15") @PathVariable Long characterId
    ) {
        characterService.deleteCharacter(characterId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "List characters (pagination + filters)",
            description = "Returns a page of CharacterListResponse. Filters: name contains, roles ALL, createdByRoomCreator.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Room not found", content = @Content)
            }
    )
    @GetMapping("/{roomId}/all")
    public ResponseEntity<Page<CharacterListResponse>> list(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(description = "List filters") @ParameterObject CharacterListFilter filter,
            @ParameterObject Pageable pageable,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        return ResponseEntity.ok(characterService.getCharacterList(roomId, account, filter, pageable));
    }

    @Operation(
            summary = "Get a character for editing",
            description = "Returns a CharacterResponse (including roles).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Character not found or no access", content = @Content)
            }
    )
    @GetMapping("/{roomId}/{characterId}/edit")
    public ResponseEntity<CharacterResponse> getForEdit(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(description = "ID of the character", required = true, example = "15") @PathVariable Long characterId,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        return ResponseEntity.ok(characterService.getCharacterForEdit(roomId, characterId, account));
    }

    @Operation(
            summary = "Get a character for user viewing (inline)",
            description = "Returns a CharacterUserResponse with lore excerpts referenced from the character's content.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Character not found or no access", content = @Content)
            }
    )
    @GetMapping("/{roomId}/{characterId}/viewInline")
    public ResponseEntity<CharacterUserResponse> getForUserInline(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(description = "ID of the character", required = true, example = "15") @PathVariable Long characterId,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        CharacterUserResponse response = characterService.getCharacterForUserInline(roomId, characterId, account);
        return ResponseEntity.ok(response);
    }
}
