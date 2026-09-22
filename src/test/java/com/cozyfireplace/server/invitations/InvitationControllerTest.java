package com.cozyfireplace.server.invitations;

import com.cozyfireplace.server.invitations.dto.InvitationRequest;
import com.cozyfireplace.server.invitations.dto.InvitationRespone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link InvitationController} — a thin facade over {@link InvitationService}
 * locking the create (200), redeem (204), delete (204) and list (200) contracts.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InvitationController")
class InvitationControllerTest {

    private static final Long ROOM_ID = 1L;
    private static final Long INVITATION_ID = 12L;

    @Mock
    private InvitationService invitationService;

    @InjectMocks
    private InvitationController controller;

    @Nested
    @DisplayName("POST /{roomId}")
    class Create {

        @Test
        @DisplayName("returns 200 with the generated invitation")
        void returnsOk() {
            InvitationRespone body = new InvitationRespone(INVITATION_ID, "ABCDEFGH");
            when(invitationService.createInvitation(ROOM_ID)).thenReturn(body);

            ResponseEntity<InvitationRespone> response = controller.createInvitation(ROOM_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
        }
    }

    @Nested
    @DisplayName("POST /use")
    class Use {

        @Test
        @DisplayName("returns 204 and redeems the code")
        void returnsNoContent() {
            InvitationRequest request = new InvitationRequest("ABCDEFGH");

            ResponseEntity<Void> response = controller.useInvitation(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(invitationService).useInvitation(request);
        }
    }

    @Nested
    @DisplayName("DELETE /{invitationId}")
    class Delete {

        @Test
        @DisplayName("returns 204 and deletes by id")
        void returnsNoContent() {
            ResponseEntity<Void> response = controller.deleteInvitation(INVITATION_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(invitationService).deleteInvitation(INVITATION_ID);
        }
    }

    @Nested
    @DisplayName("GET /{roomId}/all")
    class GetAll {

        @Test
        @DisplayName("returns 200 with the room invitations")
        void returnsList() {
            List<InvitationRespone> body = List.of(new InvitationRespone(1L, "AAA"));
            when(invitationService.getAllInvitations(ROOM_ID)).thenReturn(body);

            ResponseEntity<List<InvitationRespone>> response = controller.getAllAccountsRoom(ROOM_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
        }
    }
}
