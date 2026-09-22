package com.cozyfireplace.server.accounts.stats;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.current.CurrentAccount;
import com.cozyfireplace.server.accounts.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/account_chars")
@RequiredArgsConstructor
@Tag(name = "Account Characters", description = "Playbook characters with stats and skills attached to an account")
public class AccountCharController {

    private final AccountCharService accountCharService;

    @Operation(
            summary = "Get a character by ID",
            description = "Returns the character with its stat block and skills.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Character returned"),
                    @ApiResponse(responseCode = "404", description = "Character not found", content = @Content)
            }
    )
    @GetMapping("/{characterId}")
    public ResponseEntity<CharacterResponse> getCharacter(
            @Parameter(description = "ID of the character", required = true, example = "10") @PathVariable Long characterId) {
        return ResponseEntity.ok(accountCharService.getCharacter(characterId));
    }

    @Operation(
            summary = "Get the current account's character",
            description = "Returns the character bound to the authenticated account within the given room.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Character returned"),
                    @ApiResponse(responseCode = "404", description = "Character not found", content = @Content)
            }
    )
    @GetMapping("/self/{roomId}")
    @SuppressWarnings("unused")
    public ResponseEntity<CharacterResponse> getCharacterSelf(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(hidden = true) @CurrentAccount Account account) {
        return ResponseEntity.ok(accountCharService.getCharacterSelf(account));
    }

    @Operation(
            summary = "Create a character",
            description = "Creates a new character for the authenticated account in the room and returns it with its location header.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Character created"),
                    @ApiResponse(responseCode = "404", description = "Room not found", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(
                    name = "CharacterCreate",
                    value = """
                            {
                              "name": "Thrall",
                              "level": 3,
                              "characterClass": "Fighter",
                              "race": "Half-Elf",
                              "system": "DND_5E",
                              "origin": "City of Brass",
                              "stats": [
                                { "key": "str", "label": "Strength", "score": 16, "modifier": 3 }
                              ],
                              "skills": [
                                { "key": "athletics", "label": "Athletics", "statKey": "str", "proficiency": 2, "modifier": 5 }
                              ]
                            }
                            """
            ))
    )
    @PostMapping("/{roomId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CharacterResponse> createCharacter(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable String roomId,
            @RequestBody CharacterCreateRequest request,
            @Parameter(hidden = true) @CurrentAccount Account account) {
        CharacterResponse response = accountCharService.createCharacter(request, account);
        URI location = URI.create("/rooms/" + roomId + "/account_chars/" + response.getId());
        return ResponseEntity.created(location).body(response);
    }

    @Operation(
            summary = "Update a character",
            description = "Applies a partial (PATCH) update to the character's details, stats and skills.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Character updated"),
                    @ApiResponse(responseCode = "404", description = "Character not found", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(
                    name = "CharacterUpdate",
                    value = """
                            {
                              "name": "Thrall the Bold",
                              "level": 4,
                              "version": 1,
                              "stats": [
                                { "key": "str", "label": "Strength", "score": 18, "modifier": 4 }
                              ]
                            }
                            """
            ))
    )
    @PatchMapping("/{roomId}/{characterId}")
    @SuppressWarnings("unused")
    public ResponseEntity<CharacterResponse> updateCharacter(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable String roomId,
            @Parameter(description = "ID of the character", required = true, example = "10") @PathVariable Long characterId,
            @RequestBody CharacterUpdateRequest request,
            @Parameter(hidden = true) @CurrentAccount Account account) {
        return ResponseEntity.ok(accountCharService.updateCharacter(characterId, request, account));
    }
}
