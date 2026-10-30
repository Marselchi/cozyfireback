package com.cozyfireplace.server.accounts.stats;

import com.cozyfireplace.server.accounts.Account;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AccountCharController}.
 * <p>
 * Thin facade over {@link AccountCharService}. Pins status codes and body passthrough; the
 * noteworthy behaviours are {@code create} building a location header from the room id (a String
 * path variable here) plus the freshly created character id, and {@code update} forwarding both the
 * room and the character id so the service can resolve the character inside that room.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountCharController")
class AccountCharControllerTest {

    private static final Long CHARACTER_ID = 10L;
    private static final Long ROOM_ID = 1L;

    @Mock
    private AccountCharService accountCharService;

    @InjectMocks
    private AccountCharController controller;

    @Test
    @DisplayName("GET /{characterId} returns the character")
    void getCharacter() {
        CharacterResponse body = mock(CharacterResponse.class);
        when(accountCharService.getCharacter(CHARACTER_ID)).thenReturn(body);

        ResponseEntity<CharacterResponse> response = controller.getCharacter(CHARACTER_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(body);
    }

    @Test
    @DisplayName("GET /self/{roomId} returns the current account's character")
    void getCharacterSelf() {
        Account account = mock(Account.class);
        CharacterResponse body = mock(CharacterResponse.class);
        when(accountCharService.getCharacterSelf(account)).thenReturn(body);

        ResponseEntity<CharacterResponse> response = controller.getCharacterSelf(ROOM_ID, account);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(body);
    }

    @Test
    @DisplayName("POST /{roomId} returns 201 with a location header to the new character")
    void createCharacter() {
        CharacterCreateRequest request = mock(CharacterCreateRequest.class);
        Account account = mock(Account.class);
        CharacterResponse body = CharacterResponse.builder().id(CHARACTER_ID).build();
        when(accountCharService.createCharacter(request, account)).thenReturn(body);

        ResponseEntity<CharacterResponse> response = controller.createCharacter(String.valueOf(ROOM_ID), request, account);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(body);
        assertThat(String.valueOf(response.getHeaders().getLocation()))
                .isEqualTo("/rooms/1/account_chars/10");
    }

    @Test
    @DisplayName("PATCH /{roomId}/{characterId} forwards room and character id")
    void updateCharacter() {
        CharacterUpdateRequest request = mock(CharacterUpdateRequest.class);
        Account account = mock(Account.class);
        CharacterResponse body = mock(CharacterResponse.class);
        when(accountCharService.updateCharacter(ROOM_ID, CHARACTER_ID, request, account)).thenReturn(body);

        ResponseEntity<CharacterResponse> response = controller.updateCharacter(ROOM_ID, CHARACTER_ID, request, account);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(body);
        verify(accountCharService).updateCharacter(ROOM_ID, CHARACTER_ID, request, account);
    }
}
