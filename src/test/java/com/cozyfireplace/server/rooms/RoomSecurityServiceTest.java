package com.cozyfireplace.server.rooms;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.current.CurrentAccountResolver;
import com.cozyfireplace.server.roles.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RoomSecurityService}.
 * <p>
 * The {@link CurrentAccountResolver} and {@link RoomRepository} are mocked. Covers the two
 * {@code isCreator} overloads (roomId lookup variant and the null-tolerant account variant)
 * and the {@code hasRole} membership check.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoomSecurityService")
class RoomSecurityServiceTest {

    private static final Long ROOM_ID = 1L;

    @Mock
    private CurrentAccountResolver accountResolver;
    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomSecurityService service;

    @Nested
    @DisplayName("isCreator(Long)")
    class IsCreatorById {

        @Test
        @DisplayName("true when the resolved account matches the room creator")
        void matches() {
            Account current = mock(Account.class);
            when(current.getId()).thenReturn(100L);
            when(accountResolver.resolve(ROOM_ID)).thenReturn(current);
            Account creator = mock(Account.class);
            when(creator.getId()).thenReturn(100L);
            Room room = mock(Room.class);
            when(room.getCreator()).thenReturn(creator);
            when(roomRepository.getReferenceById(ROOM_ID)).thenReturn(room);

            assertThat(service.isCreator(ROOM_ID)).isTrue();
        }

        @Test
        @DisplayName("false when the resolved account differs from the creator")
        void differs() {
            Account current = mock(Account.class);
            when(current.getId()).thenReturn(200L);
            when(accountResolver.resolve(ROOM_ID)).thenReturn(current);
            Account creator = mock(Account.class);
            when(creator.getId()).thenReturn(100L);
            Room room = mock(Room.class);
            when(room.getCreator()).thenReturn(creator);
            when(roomRepository.getReferenceById(ROOM_ID)).thenReturn(room);

            assertThat(service.isCreator(ROOM_ID)).isFalse();
        }
    }

    @Nested
    @DisplayName("isCreator(Account)")
    class IsCreatorFromAccount {

        @Test
        @DisplayName("false for a null account")
        void nullAccount() {
            assertThat(service.isCreator((Account) null)).isFalse();
            verifyNoInteractions(roomRepository, accountResolver);
        }

        @Test
        @DisplayName("false when the account has no room")
        void nullRoom() {
            Account account = mock(Account.class);
            when(account.getRoom()).thenReturn(null);

            assertThat(service.isCreator(account)).isFalse();
        }

        @Test
        @DisplayName("false when the room has no creator")
        void nullCreator() {
            Room room = mock(Room.class);
            when(room.getCreator()).thenReturn(null);
            Account account = mock(Account.class);
            when(account.getRoom()).thenReturn(room);

            assertThat(service.isCreator(account)).isFalse();
        }

        @Test
        @DisplayName("true when the account is its own room's creator")
        void matches() {
            Account creator = mock(Account.class);
            when(creator.getId()).thenReturn(100L);
            Room room = mock(Room.class);
            when(room.getCreator()).thenReturn(creator);
            Account account = mock(Account.class);
            when(account.getRoom()).thenReturn(room);
            when(account.getId()).thenReturn(100L);

            assertThat(service.isCreator(account)).isTrue();
        }
    }

    @Nested
    @DisplayName("hasRole")
    class HasRole {

        @Test
        @DisplayName("true when one of the account roles matches by name")
        void present() {
            Role role = mock(Role.class);
            when(role.getName()).thenReturn("GM");
            Account account = mock(Account.class);
            when(account.getRoles()).thenReturn(Set.of(role));
            when(accountResolver.resolve(ROOM_ID)).thenReturn(account);

            assertThat(service.hasRole(ROOM_ID, "GM")).isTrue();
        }

        @Test
        @DisplayName("false when no role matches")
        void absent() {
            Role role = mock(Role.class);
            when(role.getName()).thenReturn("Player");
            Account account = mock(Account.class);
            when(account.getRoles()).thenReturn(Set.of(role));
            when(accountResolver.resolve(ROOM_ID)).thenReturn(account);

            assertThat(service.hasRole(ROOM_ID, "GM")).isFalse();
        }
    }
}
