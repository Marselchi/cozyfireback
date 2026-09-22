package com.cozyfireplace.server.accounts;

import com.cozyfireplace.server.accounts.dto.AccountDataResponse;
import com.cozyfireplace.server.accounts.dto.AccountMapper;
import com.cozyfireplace.server.accounts.dto.AccountUpdateRoleRequest;
import com.cozyfireplace.server.accounts.dto.AccountUpdateSelfRequest;
import com.cozyfireplace.server.accounts.dto.RoomAccountResponse;
import com.cozyfireplace.server.rooms.RoomSecurityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AccountController}.
 * <p>
 * Thin facade over {@link AccountService}, {@link RoomSecurityService} and {@link AccountMapper}.
 * Pins status codes and argument forwarding; the {@code roomId} path variable is largely unused
 * (the resolved account already scopes the room), and {@code create} is a no-op stub that must not
 * touch any collaborator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountController")
class AccountControllerTest {

    private static final Long ROOM_ID = 1L;
    private static final Long ACCOUNT_ID = 42L;

    @Mock
    private AccountService accountService;
    @Mock
    private RoomSecurityService roomSecurityService;
    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountController controller;

    @Test
    @DisplayName("POST /create is an unused stub returning 200 without side effects")
    void createAccount() {
        ResponseEntity<Void> response = controller.createAccount(mock(Account.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verifyNoInteractions(accountService, roomSecurityService, accountMapper);
    }

    @Test
    @DisplayName("GET /{roomId} projects the account with the creator flag")
    void getAccount() {
        Account account = mock(Account.class);
        when(roomSecurityService.isCreator(account)).thenReturn(true);
        AccountDataResponse body = mock(AccountDataResponse.class);
        when(accountMapper.toAccountDataResponse(account, true)).thenReturn(body);

        ResponseEntity<AccountDataResponse> response = controller.getAccount(ROOM_ID, account);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(body);
    }

    @Test
    @DisplayName("GET /{roomId}/all lists room accounts")
    void getAllAccountsRoom() {
        Account account = mock(Account.class);
        List<RoomAccountResponse> body = List.of(mock(RoomAccountResponse.class));
        when(accountService.getAllAccountsInRoom(ROOM_ID, account)).thenReturn(body);

        ResponseEntity<List<RoomAccountResponse>> response = controller.getAllAccountsRoom(ROOM_ID, account);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(body);
    }

    @Test
    @DisplayName("DELETE role forwards account and role ids, ignoring the room id")
    void deleteAccountRole() {
        ResponseEntity<Void> response = controller.deleteAccountRole(ROOM_ID, ACCOUNT_ID, 5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(accountService).deleteAccountRole(ACCOUNT_ID, 5L);
    }

    @Test
    @DisplayName("POST role forwards account and role ids, ignoring the room id")
    void addAccountRole() {
        ResponseEntity<Void> response = controller.addAccountRole(ROOM_ID, ACCOUNT_ID, 5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(accountService).addAccountRole(ACCOUNT_ID, 5L);
    }

    @Test
    @DisplayName("PUT role forwards the swap request for the target account")
    void updateAccountRole() {
        AccountUpdateRoleRequest request = new AccountUpdateRoleRequest(5L, 7L);

        ResponseEntity<Void> response = controller.updateAccountRole(ROOM_ID, ACCOUNT_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(accountService).updateAccountRole(ACCOUNT_ID, request);
    }

    @Test
    @DisplayName("PUT /update forwards the current account and self-update request")
    void updateSelf() {
        Account account = mock(Account.class);
        AccountUpdateSelfRequest request = new AccountUpdateSelfRequest("Nyra");

        ResponseEntity<Void> response = controller.updateSelf(ROOM_ID, account, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(accountService).updateSelf(account, request);
    }
}
