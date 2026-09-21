package com.cozyfireplace.server.accounts.stats;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.current.CurrentAccount;
import com.cozyfireplace.server.accounts.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/account_chars")
@RequiredArgsConstructor
public class AccountCharController {

    private final AccountCharService accountCharService;

    @GetMapping("/{characterId}")
    public ResponseEntity<CharacterResponse> getCharacter(@PathVariable Long characterId) {
        return ResponseEntity.ok(accountCharService.getCharacter(characterId));
    }

    @GetMapping("/self/{roomId}")
    public ResponseEntity<CharacterResponse> getCharacterSelf(@PathVariable Long roomId, @CurrentAccount Account account) {
        return ResponseEntity.ok(accountCharService.getCharacterSelf(account));
    }

    @PostMapping("/{roomId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CharacterResponse> createCharacter(
            @PathVariable String roomId,
            @RequestBody CharacterCreateRequest request,
            @CurrentAccount Account account) {
        CharacterResponse response = accountCharService.createCharacter(request, account);
        URI location = URI.create("/rooms/" + roomId + "/account_chars/" + response.getId());
        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/{roomId}/{characterId}")
    public ResponseEntity<CharacterResponse> updateCharacter(
            @PathVariable String roomId,
            @PathVariable Long characterId,
            @RequestBody CharacterUpdateRequest request,
            @CurrentAccount Account account) {
        return ResponseEntity.ok(accountCharService.updateCharacter(characterId, request, account));
    }
}
