package com.cozyfireplace.server.accounts;


import com.cozyfireplace.server.accounts.current.CurrentAccount;
import com.cozyfireplace.server.accounts.dto.*;
import com.cozyfireplace.server.rooms.RoomSecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Account management: profile data, room membership and roles")
public class AccountController {

    private final AccountService accountService;
    private final RoomSecurityService  roomSecurityService;
    private final AccountMapper accountMapper;


    @Operation(
            summary = "Create an account",
            description = "Creates a new account within a room.",
            responses = @ApiResponse(responseCode = "200", description = "Account created")
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(
                    name = "Account",
                    value = """
                            {
                              "name": "Nyra",
                              "roles": []
                            }
                            """
            ))
    )
    @PostMapping("/create")
    @SuppressWarnings("unused")
    public ResponseEntity<Void> createAccount(@RequestBody Account account) {
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Get the current account",
            description = "Returns the profile data of the authenticated account for the given room. Includes extra fields when the account is the room creator.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Account data returned"),
                    @ApiResponse(responseCode = "404", description = "Room or account not found", content = @Content)
            }
    )
    @GetMapping("/{roomId}")
    @SuppressWarnings("unused")
    public ResponseEntity<AccountDataResponse> getAccount(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(hidden = true) @CurrentAccount Account account) {
        boolean isCreator = roomSecurityService.isCreator(account);
        AccountDataResponse response = accountMapper.toAccountDataResponse(account, isCreator);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get all accounts in a room",
            description = "Returns the list of accounts that belong to the specified room.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of room accounts returned"),
                    @ApiResponse(responseCode = "403", description = "Not allowed to list room accounts", content = @Content)
            }
    )
    //todo: checks
    @GetMapping("/{roomId}/all")
    public ResponseEntity<List<RoomAccountResponse>> getAllAccountsRoom(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(hidden = true) @CurrentAccount Account account) {
        List<RoomAccountResponse> response = accountService.getAllAccountsInRoom(roomId, account);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Remove a role from an account",
            description = "Detaches the given role from the target account in the room.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Role removed"),
                    @ApiResponse(responseCode = "403", description = "Admin privileges required", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "ID of the role to remove",
            content = @Content(examples = @ExampleObject(name = "roleId", value = "5"))
    )
    //todo: checks admin and response
    @DeleteMapping("/{roomId}/{accountId}/role")
    @SuppressWarnings("unused")
    public ResponseEntity<Void> deleteAccountRole(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(description = "ID of the target account", required = true, example = "42") @PathVariable Long accountId,
            @RequestBody Long roleId) {
        accountService.deleteAccountRole(accountId, roleId);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Add a role to an account",
            description = "Assigns the given role to the target account in the room.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Role added"),
                    @ApiResponse(responseCode = "403", description = "Admin privileges required", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "ID of the role to add",
            content = @Content(examples = @ExampleObject(name = "roleId", value = "5"))
    )
    //todo: checks admin and response
    @PostMapping("/{roomId}/{accountId}/role")
    @SuppressWarnings("unused")
    public ResponseEntity<Void> addAccountRole(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(description = "ID of the target account", required = true, example = "42") @PathVariable Long accountId,
            @RequestBody Long roleId) {
        accountService.addAccountRole(accountId, roleId);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Replace a role on an account",
            description = "Swaps an existing role on the target account for a new one.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Role updated"),
                    @ApiResponse(responseCode = "403", description = "Admin privileges required", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(
                    name = "RoleSwap",
                    value = """
                            {
                              "oldRoleId": 5,
                              "newRoleId": 7
                            }
                            """
            ))
    )
    //todo: checks admin and response
    @PutMapping("/{roomId}/{accountId}/role")
    @SuppressWarnings("unused")
    public ResponseEntity<Void> updateAccountRole(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(description = "ID of the target account", required = true, example = "42") @PathVariable Long accountId,
            @RequestBody AccountUpdateRoleRequest request) {
        accountService.updateAccountRole(accountId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Update the current account",
            description = "Updates the authenticated account's own data (for example, its display name) within a room.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Account updated"),
                    @ApiResponse(responseCode = "404", description = "Account not found", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(
                    name = "SelfUpdate",
                    value = """
                            {
                              "name": "Nyra the Wanderer"
                            }
                            """
            ))
    )
    @PutMapping("/{roomId}/update")
    @SuppressWarnings("unused")
    public ResponseEntity<Void> updateSelf(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(hidden = true) @CurrentAccount Account account,
            @RequestBody AccountUpdateSelfRequest request) {
        accountService.updateSelf(account, request);
        return ResponseEntity.ok().build();
    }
}
