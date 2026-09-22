package com.cozyfireplace.server.accounts;

import com.cozyfireplace.server.accounts.dto.AccountMapper;
import com.cozyfireplace.server.accounts.dto.AccountUpdateRoleRequest;
import com.cozyfireplace.server.accounts.dto.AccountUpdateSelfRequest;
import com.cozyfireplace.server.accounts.dto.RoomAccountResponse;
import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.roles.Role;
import com.cozyfireplace.server.roles.RoleRepository;
import com.cozyfireplace.server.roles.RoleService;
import com.cozyfireplace.server.rooms.Room;
import com.cozyfireplace.server.rooms.RoomRepository;
import com.cozyfireplace.server.rooms.RoomService;
import com.cozyfireplace.server.util.exception.AlreadyExistsException;
import com.cozyfireplace.server.util.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AccountService}.
 * <p>
 * Repositories, {@link RoomService}, {@link RoleService} and {@link AccountMapper} are mocked.
 * Role collections are backed by real mutable sets so the add/remove/replace logic is genuinely
 * exercised. Covers room listing projection, account creation wiring, the role add/remove/replace
 * flows and their not-found / already-exists guards.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService")
class AccountServiceTest {

    private static final Long ROOM_ID = 1L;
    private static final Long ACCOUNT_ID = 42L;

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RoomService roomService;
    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountService service;

    @Nested
    @DisplayName("getAllAccountsInRoom")
    class GetAll {

        @Test
        @DisplayName("excludes the caller and projects each account")
        void projects() {
            Account caller = mock(Account.class);
            when(caller.getId()).thenReturn(ACCOUNT_ID);
            Account other = mock(Account.class);
            when(accountRepository.findAllByRoomIdAndNotId(ROOM_ID, ACCOUNT_ID)).thenReturn(List.of(other));
            RoomAccountResponse projected = mock(RoomAccountResponse.class);
            when(accountMapper.toRoomAccountResponse(other)).thenReturn(projected);

            assertThat(service.getAllAccountsInRoom(ROOM_ID, caller)).containsExactly(projected);
        }
    }

    @Nested
    @DisplayName("createAccount")
    class Create {

        @Test
        @DisplayName("builds a member account bound to the room and current profile")
        void savesMemberAccount() {
            Profile profile = Profile.builder().username("user").build();
            when(roomService.getCurrentAuthenticatedProfile()).thenReturn(profile);
            Room room = Room.builder().id(ROOM_ID).build();
            when(roomRepository.getReferenceById(ROOM_ID)).thenReturn(room);

            service.createAccount(ROOM_ID);

            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(captor.capture());
            assertThat(captor.getValue().getProfile()).isSameAs(profile);
            assertThat(captor.getValue().getRoom()).isSameAs(room);
            assertThat(captor.getValue().getName()).isEqualTo("Лучше поменять");
        }
    }

    @Nested
    @DisplayName("deleteAccountRole")
    class DeleteRole {

        @Test
        @DisplayName("throws NotFound when the account is unknown")
        void accountMissing() {
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteAccountRole(ACCOUNT_ID, 5L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("removes a present role")
        void removes() {
            Role role = mock(Role.class);
            when(role.getId()).thenReturn(5L);
            Account account = mock(Account.class);
            when(account.getRoles()).thenReturn(new HashSet<>(Set.of(role)));
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            service.deleteAccountRole(ACCOUNT_ID, 5L);

            assertThat(account.getRoles()).isEmpty();
        }

        @Test
        @DisplayName("throws NotFound when the role was not assigned")
        void roleMissing() {
            Account account = mock(Account.class);
            when(account.getRoles()).thenReturn(new HashSet<>());
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            assertThatThrownBy(() -> service.deleteAccountRole(ACCOUNT_ID, 5L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("addAccountRole")
    class AddRole {

        @Test
        @DisplayName("throws NotFound when the account is unknown")
        void accountMissing() {
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addAccountRole(ACCOUNT_ID, 5L))
                    .isInstanceOf(NotFoundException.class);
            verifyNoInteractions(roleRepository);
        }

        @Test
        @DisplayName("throws NotFound when the role is unknown")
        void roleMissing() {
            Account account = mock(Account.class);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(roleRepository.findById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addAccountRole(ACCOUNT_ID, 5L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("throws AlreadyExists when the account already holds the role")
        void alreadyPresent() {
            Role role = mock(Role.class);
            Account account = mock(Account.class);
            when(account.getRoles()).thenReturn(new HashSet<>(Set.of(role)));
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(roleRepository.findById(5L)).thenReturn(Optional.of(role));

            assertThatThrownBy(() -> service.addAccountRole(ACCOUNT_ID, 5L))
                    .isInstanceOf(AlreadyExistsException.class);
        }

        @Test
        @DisplayName("assigns a fresh role and saves")
        void assigns() {
            Role role = mock(Role.class);
            Account account = mock(Account.class);
            when(account.getRoles()).thenReturn(new HashSet<>());
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(roleRepository.findById(5L)).thenReturn(Optional.of(role));

            service.addAccountRole(ACCOUNT_ID, 5L);

            assertThat(account.getRoles()).containsExactly(role);
            verify(accountRepository).save(account);
        }
    }

    @Nested
    @DisplayName("updateSelf")
    class UpdateSelf {

        @Test
        @DisplayName("persists the mapper-updated account")
        void persists() {
            Account account = mock(Account.class);
            AccountUpdateSelfRequest request = new AccountUpdateSelfRequest("Nyra");
            Account updated = mock(Account.class);
            when(accountMapper.updateSelf(request, account)).thenReturn(updated);

            service.updateSelf(account, request);

            verify(accountRepository).save(updated);
        }
    }

    @Nested
    @DisplayName("updateAccountRole")
    class UpdateRole {

        @Test
        @DisplayName("throws NotFound when the account is unknown")
        void accountMissing() {
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateAccountRole(ACCOUNT_ID, new AccountUpdateRoleRequest(5L, 7L)))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("throws NotFound when the account lacks the old role")
        void lacksOldRole() {
            Account account = mock(Account.class);
            when(account.getRoles()).thenReturn(new HashSet<>());
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            assertThatThrownBy(() -> service.updateAccountRole(ACCOUNT_ID, new AccountUpdateRoleRequest(5L, 7L)))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("throws AlreadyExists when the account already holds the new role")
        void alreadyHasNewRole() {
            Role oldRole = mock(Role.class);
            when(oldRole.getId()).thenReturn(5L);
            Role newRole = mock(Role.class);
            when(newRole.getId()).thenReturn(7L);
            Set<Role> roles = new HashSet<>();
            roles.add(oldRole);
            roles.add(newRole);
            Account account = mock(Account.class);
            when(account.getRoles()).thenReturn(roles);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            assertThatThrownBy(() -> service.updateAccountRole(ACCOUNT_ID, new AccountUpdateRoleRequest(5L, 7L)))
                    .isInstanceOf(AlreadyExistsException.class);
        }

        @Test
        @DisplayName("swaps the old role for the new one and saves")
        void swapsRoles() {
            Role oldRole = mock(Role.class);
            when(oldRole.getId()).thenReturn(5L);
            Role newRole = mock(Role.class);
            Set<Role> roles = new HashSet<>();
            roles.add(oldRole);
            Account account = mock(Account.class);
            when(account.getRoles()).thenReturn(roles);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(roleRepository.findById(7L)).thenReturn(Optional.of(newRole));

            service.updateAccountRole(ACCOUNT_ID, new AccountUpdateRoleRequest(5L, 7L));

            assertThat(account.getRoles()).containsExactly(newRole);
            verify(accountRepository).save(account);
        }
    }
}
