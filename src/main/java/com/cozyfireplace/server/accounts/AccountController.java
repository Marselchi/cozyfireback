package com.cozyfireplace.server.accounts;


import com.cozyfireplace.server.accounts.current.CurrentAccount;
import com.cozyfireplace.server.accounts.dto.*;
import com.cozyfireplace.server.rooms.RoomSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final RoomSecurityService  roomSecurityService;
    private final AccountMapper accountMapper;


    @PostMapping("/create")
    public ResponseEntity<Void> createAccount(@RequestBody Account account) {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<AccountDataResponse> getAccount(@PathVariable Long roomId, @CurrentAccount Account account) {
        boolean isCreator = roomSecurityService.isCreator(account);
        AccountDataResponse response = accountMapper.toAccountDataResponse(account, isCreator);
        return ResponseEntity.ok(response);
    }

    //todo: checks
    @GetMapping("/{roomId}/all")
    public ResponseEntity<List<RoomAccountResponse>> getAllAccountsRoom(@PathVariable Long roomId, @CurrentAccount Account account) {
        List<RoomAccountResponse> response = accountService.getAllAccountsInRoom(roomId, account);
        return ResponseEntity.ok(response);
    }

    //todo: checks admin and response
    @DeleteMapping("/{roomId}/{accountId}/role")
    public ResponseEntity<Void> deleteAccountRole(@PathVariable Long roomId,
                                                  @PathVariable Long accountId,
                                                  @RequestBody Long roleId) {
        accountService.deleteAccountRole(accountId, roleId);
        return ResponseEntity.ok().build();
    }

    //todo: checks admin and response
    @PostMapping("/{roomId}/{accountId}/role")
    public ResponseEntity<Void> addAccountRole(@PathVariable Long roomId,
                                                  @PathVariable Long accountId,
                                                  @RequestBody Long roleId) {
        accountService.addAccountRole(accountId, roleId);
        return ResponseEntity.ok().build();
    }

    //todo: checks admin and response
    @PutMapping("/{roomId}/{accountId}/role")
    public ResponseEntity<Void> updateAccountRole(@PathVariable Long roomId,
                                                  @PathVariable Long accountId,
                                                  @RequestBody AccountUpdateRoleRequest request) {
        accountService.updateAccountRole(accountId, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{roomId}/update")
    public ResponseEntity<Void> updateSelf(@PathVariable Long roomId,
                                           @CurrentAccount Account account,
                                           @RequestBody AccountUpdateSelfRequest request) {
        accountService.updateSelf(account, request);
        return ResponseEntity.ok().build();
    }
}
