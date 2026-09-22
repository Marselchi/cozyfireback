package com.cozyfireplace.server.characters;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.characters.dto.CharacterListFilter;
import com.cozyfireplace.server.characters.dto.CharacterListResponse;
import com.cozyfireplace.server.characters.dto.CharacterRequest;
import com.cozyfireplace.server.characters.dto.CharacterResponse;
import com.cozyfireplace.server.characters.dto.CharacterUserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CharacterController}.
 * <p>
 * The controller is a thin HTTP facade over {@link CharacterService}; each test pins
 * the returned status code, the body passthrough and the exact argument order forwarded
 * to the service (note the room id is threaded into every room-scoped call here, unlike
 * the role/tag facades that ignore it). Persistence, security and the {@code @CurrentAccount}
 * resolution are out of scope — the service is fully mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CharacterController")
class CharacterControllerTest {

    private static final Long ROOM_ID = 1L;
    private static final Long CHARACTER_ID = 15L;

    @Mock
    private CharacterService characterService;

    @InjectMocks
    private CharacterController controller;

    @Nested
    @DisplayName("POST /{roomId}")
    class Create {

        @Test
        @DisplayName("returns 201 with the new id and forwards room, account and body")
        void returnsCreated() {
            Account account = org.mockito.Mockito.mock(Account.class);
            CharacterRequest request = org.mockito.Mockito.mock(CharacterRequest.class);
            when(characterService.createCharacter(ROOM_ID, account, request)).thenReturn(CHARACTER_ID);

            ResponseEntity<Long> response = controller.create(ROOM_ID, request, account);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isEqualTo(CHARACTER_ID);
        }
    }

    @Nested
    @DisplayName("PATCH /{roomId}/{characterId}")
    class Update {

        @Test
        @DisplayName("returns 204 and forwards character id, body, account and room id")
        void returnsNoContent() {
            Account account = org.mockito.Mockito.mock(Account.class);
            CharacterRequest request = org.mockito.Mockito.mock(CharacterRequest.class);

            ResponseEntity<Void> response = controller.update(ROOM_ID, CHARACTER_ID, request, account);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(characterService).updateCharacter(CHARACTER_ID, request, account, ROOM_ID);
            verifyNoMoreInteractions(characterService);
        }
    }

    @Nested
    @DisplayName("DELETE /{characterId}")
    class Delete {

        @Test
        @DisplayName("returns 204 and deletes by character id")
        void returnsNoContent() {
            ResponseEntity<Void> response = controller.delete(CHARACTER_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(characterService).deleteCharacter(CHARACTER_ID);
        }
    }

    @Nested
    @DisplayName("GET /{roomId}/all")
    class List {

        @Test
        @DisplayName("returns 200 with the service page")
        @SuppressWarnings("unchecked")
        void returnsPage() {
            Account account = org.mockito.Mockito.mock(Account.class);
            CharacterListFilter filter = org.mockito.Mockito.mock(CharacterListFilter.class);
            Pageable pageable = org.mockito.Mockito.mock(Pageable.class);
            Page<CharacterListResponse> body = org.mockito.Mockito.mock(Page.class);
            when(characterService.getCharacterList(ROOM_ID, account, filter, pageable)).thenReturn(body);

            ResponseEntity<Page<CharacterListResponse>> response =
                    controller.list(ROOM_ID, filter, pageable, account);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
        }
    }

    @Nested
    @DisplayName("GET /{roomId}/{characterId}/edit")
    class GetForEdit {

        @Test
        @DisplayName("returns 200 with the editable character")
        void returnsBody() {
            Account account = org.mockito.Mockito.mock(Account.class);
            CharacterResponse body = org.mockito.Mockito.mock(CharacterResponse.class);
            when(characterService.getCharacterForEdit(ROOM_ID, CHARACTER_ID, account)).thenReturn(body);

            ResponseEntity<CharacterResponse> response = controller.getForEdit(ROOM_ID, CHARACTER_ID, account);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
        }
    }

    @Nested
    @DisplayName("GET /{roomId}/{characterId}/viewInline")
    class GetForUserInline {

        @Test
        @DisplayName("returns 200 with the inline user view")
        void returnsBody() {
            Account account = org.mockito.Mockito.mock(Account.class);
            CharacterUserResponse body = org.mockito.Mockito.mock(CharacterUserResponse.class);
            when(characterService.getCharacterForUserInline(ROOM_ID, CHARACTER_ID, account)).thenReturn(body);

            ResponseEntity<CharacterUserResponse> response =
                    controller.getForUserInline(ROOM_ID, CHARACTER_ID, account);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
        }
    }
}
