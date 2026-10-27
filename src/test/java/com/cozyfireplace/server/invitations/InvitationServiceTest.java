package com.cozyfireplace.server.invitations;

import com.cozyfireplace.server.accounts.AccountService;
import com.cozyfireplace.server.invitations.dto.InvitationRequest;
import com.cozyfireplace.server.invitations.dto.InvitationRespone;
import com.cozyfireplace.server.rooms.Room;
import com.cozyfireplace.server.rooms.RoomRepository;
import com.cozyfireplace.server.rooms.RoomSecurityService;
import com.cozyfireplace.server.util.exception.ForbiddenException;
import com.cozyfireplace.server.util.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure Mockito unit tests for {@link InvitationService}.
 * <p>
 * The repositories and {@link AccountService} are mocked. Covers the random code generator
 * (length + alphabet), the create-and-project flow, room-scoped deletion (an invitation from
 * another room answers 404 rather than being silently kept), the creator-only gate on every
 * management operation, the redeem flow (which joins the room behind the code) and the
 * room-scoped listing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InvitationService")
class InvitationServiceTest {

    private static final Long ROOM_ID = 1L;
    private static final Long INVITATION_ID = 12L;

    @Mock
    private InvitationRepository invitationRepository;
    @Mock
    private AccountService accountService;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private RoomSecurityService roomSecurityService;

    @InjectMocks
    private InvitationService service;

    @Nested
    @DisplayName("generateRandomString")
    class Generator {

        @Test
        @DisplayName("produces a string of the requested length drawn from the alphabet")
        void lengthAndAlphabet() {
            String code = InvitationService.generateRandomString(8);

            assertThat(code).hasSize(8);
            assertThat(code).matches("[A-Za-z0-9]{8}");
        }

        @Test
        @DisplayName("zero length yields an empty string")
        void zeroLength() {
            assertThat(InvitationService.generateRandomString(0)).isEmpty();
        }
    }

    @Nested
    @DisplayName("createInvitation")
    class Create {

        @Test
        @DisplayName("throws NotFound for an unknown room instead of an FK failure")
        void roomNotFound() {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createInvitation(ROOM_ID)).isInstanceOf(NotFoundException.class);
            verifyNoInteractions(roomSecurityService);
            verify(invitationRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws Forbidden for a member who is not the room creator")
        void notCreator() {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(Room.builder().id(ROOM_ID).build()));
            when(roomSecurityService.isCreator(ROOM_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.createInvitation(ROOM_ID)).isInstanceOf(ForbiddenException.class);
            verify(invitationRepository, never()).save(any());
        }

        @Test
        @DisplayName("builds an 8-char code for the room and projects the saved entity")
        void buildsAndProjects() {
            Room room = mock(Room.class);
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
            when(roomSecurityService.isCreator(ROOM_ID)).thenReturn(true);
            Invitation saved = Invitation.builder().id(INVITATION_ID).code("ABCDEFGH").room(room).build();
            when(invitationRepository.save(any(Invitation.class))).thenReturn(saved);

            InvitationRespone response = service.createInvitation(ROOM_ID);

            assertThat(response).isEqualTo(new InvitationRespone(INVITATION_ID, "ABCDEFGH"));
            ArgumentCaptor<Invitation> captor = ArgumentCaptor.forClass(Invitation.class);
            verify(invitationRepository).save(captor.capture());
            assertThat(captor.getValue().getCode()).hasSize(8);
            assertThat(captor.getValue().getRoom()).isSameAs(room);
        }
    }

    @Nested
    @DisplayName("deleteInvitation")
    class Delete {

        @Test
        @DisplayName("throws NotFound when the invitation belongs to another room")
        void notFound() {
            when(invitationRepository.findByRoomIdAndId(ROOM_ID, INVITATION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteInvitation(ROOM_ID, INVITATION_ID))
                    .isInstanceOf(NotFoundException.class);
            verifyNoInteractions(roomSecurityService);
            verify(invitationRepository, never()).delete(any());
        }

        @Test
        @DisplayName("throws Forbidden when the caller is not the room creator")
        void notCreator() {
            when(invitationRepository.findByRoomIdAndId(ROOM_ID, INVITATION_ID))
                    .thenReturn(Optional.of(Invitation.builder().id(INVITATION_ID).build()));
            when(roomSecurityService.isCreator(ROOM_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.deleteInvitation(ROOM_ID, INVITATION_ID))
                    .isInstanceOf(ForbiddenException.class);
            verify(invitationRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deletes the loaded invitation (no silent no-op on a foreign id)")
        void deletes() {
            Invitation invitation = Invitation.builder().id(INVITATION_ID).build();
            when(invitationRepository.findByRoomIdAndId(ROOM_ID, INVITATION_ID)).thenReturn(Optional.of(invitation));
            when(roomSecurityService.isCreator(ROOM_ID)).thenReturn(true);

            service.deleteInvitation(ROOM_ID, INVITATION_ID);

            verify(invitationRepository).delete(invitation);
            verify(invitationRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("useInvitation")
    class Use {

        @Test
        @DisplayName("redeems a known code by joining the account to its room")
        void joinsRoom() {
            Room room = Room.builder().id(9L).build();
            Invitation invitation = Invitation.builder().room(room).build();
            when(invitationRepository.findByCode("CODE")).thenReturn(Optional.of(invitation));

            service.useInvitation(new InvitationRequest("CODE"));

            verify(accountService).createAccount(9L);
        }

        @Test
        @DisplayName("throws NotFound for an unknown code without joining")
        void unknownCode() {
            when(invitationRepository.findByCode("nope")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.useInvitation(new InvitationRequest("nope")))
                    .isInstanceOf(NotFoundException.class);
            verifyNoInteractions(accountService);
        }
    }

    @Nested
    @DisplayName("getAllInvitations")
    class GetAll {

        @Test
        @DisplayName("throws Forbidden when the caller is not the room creator")
        void notCreator() {
            when(roomSecurityService.isCreator(ROOM_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.getAllInvitations(ROOM_ID)).isInstanceOf(ForbiddenException.class);
            verifyNoInteractions(invitationRepository);
        }

        @Test
        @DisplayName("projects every stored invitation")
        void projects() {
            Invitation a = Invitation.builder().id(1L).code("AAA").build();
            Invitation b = Invitation.builder().id(2L).code("BBB").build();
            when(roomSecurityService.isCreator(ROOM_ID)).thenReturn(true);
            when(invitationRepository.findAllByRoomId(ROOM_ID)).thenReturn(List.of(a, b));

            assertThat(service.getAllInvitations(ROOM_ID))
                    .containsExactly(new InvitationRespone(1L, "AAA"), new InvitationRespone(2L, "BBB"));
        }
    }
}
