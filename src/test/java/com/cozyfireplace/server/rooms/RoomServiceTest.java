package com.cozyfireplace.server.rooms;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.AccountRepository;
import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.auth.profile.ProfileRepository;
import com.cozyfireplace.server.questions.QuestionRepository;
import com.cozyfireplace.server.rooms.dto.RoomContextResponse;
import com.cozyfireplace.server.rooms.dto.RoomCreateRequest;
import com.cozyfireplace.server.rooms.dto.RoomMapper;
import com.cozyfireplace.server.rooms.dto.RoomUpdateRequest;
import com.cozyfireplace.server.rooms.roomDetails.RoomDetails;
import com.cozyfireplace.server.rooms.roomDetails.RoomDetailsRepository;
import com.cozyfireplace.server.util.exception.NotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RoomService}.
 * <p>
 * All repositories and the {@link RoomMapper} are mocked; the security context is populated
 * directly so the authenticated-profile lookup is exercised for real. Covers room creation
 * (creator account + details wiring), the profile-resolution guard clauses, the user-room
 * listing projection, url/id lookups and the room rename flow.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoomService")
class RoomServiceTest {

    private static final Long ROOM_ID = 1L;

    @Mock
    private RoomRepository roomRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private RoomDetailsRepository roomDetailsRepository;
    @Mock
    private RoomMapper roomMapper;
    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private RoomService service;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticateAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                username, "pw", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @Nested
    @DisplayName("getCurrentAuthenticatedProfile")
    class CurrentProfile {

        @Test
        @DisplayName("throws AccessDenied when there is no authentication")
        void noAuthentication() {
            assertThatThrownBy(() -> service.getCurrentAuthenticatedProfile())
                    .isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(profileRepository);
        }

        @Test
        @DisplayName("throws AccessDenied for an unauthenticated token")
        void unauthenticated() {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("user", "pw"));

            assertThatThrownBy(() -> service.getCurrentAuthenticatedProfile())
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("throws IllegalStateException when the profile is unknown")
        void profileMissing() {
            authenticateAs("ghost");
            when(profileRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCurrentAuthenticatedProfile())
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("returns the stored profile for the authenticated username")
        void returnsProfile() {
            Profile profile = Profile.builder().username("user").build();
            authenticateAs("user");
            when(profileRepository.findByUsername("user")).thenReturn(Optional.of(profile));

            assertThat(service.getCurrentAuthenticatedProfile()).isSameAs(profile);
        }
    }

    @Nested
    @DisplayName("createRoom")
    class Create {

        @Test
        @DisplayName("wires the creator account and room details")
        void createsRoomAndDependencies() {
            Profile profile = Profile.builder().username("user").build();
            authenticateAs("user");
            when(profileRepository.findByUsername("user")).thenReturn(Optional.of(profile));
            Account savedCreator = Account.builder().id(55L).build();
            when(accountRepository.save(any(Account.class))).thenReturn(savedCreator);

            RoomCreateRequest request = mock(RoomCreateRequest.class);
            when(request.getName()).thenReturn("Hall");
            when(request.getUrl()).thenReturn("hall");
            when(request.getDescription()).thenReturn("desc");

            service.createRoom(request);

            ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(accountCaptor.capture());
            assertThat(accountCaptor.getValue().getName()).isEqualTo("user");
            assertThat(accountCaptor.getValue().getProfile()).isSameAs(profile);
            assertThat(accountCaptor.getValue().getRoom()).isNotNull();

            ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
            verify(roomRepository).save(roomCaptor.capture());
            assertThat(roomCaptor.getValue().getName()).isEqualTo("Hall");
            assertThat(roomCaptor.getValue().getCreator()).isSameAs(savedCreator);

            verify(roomDetailsRepository).save(any(RoomDetails.class));
        }
    }

    @Nested
    @DisplayName("getUserRooms")
    class GetUserRooms {

        @Test
        @DisplayName("projects every membership with creator info and member count")
        void projectsRooms() {
            Profile currentUser = Profile.builder().username("user").build();
            authenticateAs("user");
            when(profileRepository.findByUsername("user")).thenReturn(Optional.of(currentUser));

            Profile creatorProfile = Profile.builder().username("creator").build();
            Account creator = Account.builder().id(100L).profile(creatorProfile).build();
            Room room = Room.builder().id(7L).name("Hall").description("d").url("hall").creator(creator).build();
            Account member = Account.builder().id(200L).room(room).build();
            when(accountRepository.findByProfile(currentUser)).thenReturn(List.of(member));
            when(accountRepository.countByRoom(room)).thenReturn(3L);

            List<RoomContextResponse> result = service.getUserRooms();

            assertThat(result).hasSize(1);
            RoomContextResponse response = result.getFirst();
            assertThat(response.getId()).isEqualTo(7L);
            assertThat(response.getName()).isEqualTo("Hall");
            assertThat(response.getUrl()).isEqualTo("hall");
            assertThat(response.isCurrentUserCreator()).isFalse();
            assertThat(response.getMemberCount()).isEqualTo(3L);
            assertThat(response.getCreator().getId()).isEqualTo(100L);
            assertThat(response.getCreator().getUsername()).isEqualTo("creator");
        }
    }

    @Nested
    @DisplayName("getRoomByUrl")
    class GetByUrl {

        @Test
        @DisplayName("returns the id for a known url")
        void known() {
            Room room = Room.builder().id(7L).build();
            when(roomRepository.findByUrl("hall")).thenReturn(Optional.of(room));

            assertThat(service.getRoomByUrl("hall")).isEqualTo(7L);
        }

        @Test
        @DisplayName("throws NotFound for an unknown url")
        void unknown() {
            when(roomRepository.findByUrl("nope")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getRoomByUrl("nope"))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getRoomById")
    class GetById {

        @Test
        @DisplayName("projects the room flagging the caller as creator")
        void asCreator() {
            Profile creatorProfile = Profile.builder().username("creator").build();
            Account creator = Account.builder().id(100L).profile(creatorProfile).build();
            Room room = Room.builder().id(7L).name("Hall").description("d").url("hall").creator(creator).build();
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
            when(accountRepository.countByRoom(room)).thenReturn(4L);
            when(questionRepository.countUnansweredByRoomId(ROOM_ID)).thenReturn(2L);
            Account currentAccount = Account.builder().id(100L).build();

            RoomContextResponse response = service.getRoomById(ROOM_ID, currentAccount);

            assertThat(response.getId()).isEqualTo(7L);
            assertThat(response.isCurrentUserCreator()).isTrue();
            assertThat(response.getMemberCount()).isEqualTo(4L);
            assertThat(response.getQuestionCount()).isEqualTo(2L);
        }

        @Test
        @DisplayName("throws NotFound for an unknown room id")
        void unknown() {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getRoomById(ROOM_ID, mock(Account.class)))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateRoom")
    class Update {

        @Test
        @DisplayName("maps onto the stored room and saves")
        void mapsAndSaves() {
            Room room = Room.builder().id(ROOM_ID).build();
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
            RoomUpdateRequest request = mock(RoomUpdateRequest.class);

            service.updateRoom(ROOM_ID, request);

            verify(roomMapper).updateRoomFromRequest(request, room);
            verify(roomRepository).save(room);
        }

        @Test
        @DisplayName("throws NotFound for an unknown room without saving")
        void unknown() {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateRoom(ROOM_ID, mock(RoomUpdateRequest.class)))
                    .isInstanceOf(NotFoundException.class);
            verifyNoInteractions(roomMapper);
        }
    }
}
