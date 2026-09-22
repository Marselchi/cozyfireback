package com.cozyfireplace.server.roles;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.AccountRepository;
import com.cozyfireplace.server.characters.Character;
import com.cozyfireplace.server.lore.Lore;
import com.cozyfireplace.server.roles.dto.RoleCreateRequest;
import com.cozyfireplace.server.roles.dto.RoleEditResponse;
import com.cozyfireplace.server.roles.dto.RoleMapper;
import com.cozyfireplace.server.roles.dto.RoleResponse;
import com.cozyfireplace.server.roles.dto.RoleUpdateRequest;
import com.cozyfireplace.server.rooms.Room;
import com.cozyfireplace.server.rooms.RoomRepository;
import com.cozyfireplace.server.util.exception.AlreadyExistsException;
import com.cozyfireplace.server.util.exception.DatabaseConstaintException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pure Mockito unit tests for {@link RoleService}.
 * <p>
 * All collaborators (repositories and the MapStruct mapper) are mocked, so the
 * tests exercise only the service's own branching: room/role existence checks,
 * duplicate detection, cross-room assignment validation, id-collection
 * validation (missing-role detection) and the collection-clear-and-add semantics
 * of the {@code updateRolesXxx} helpers. JPA/persistence behaviour is out of
 * scope here and covered by integration tests.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleService")
class RoleServiceTest {

    private static final Long ROOM_ID = 10L;
    private static final Long ROLE_ID = 10L;
    private static final Long ACCOUNT_ID = 42L;

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private RoleMapper roleMapper;

    @InjectMocks
    private RoleService roleService;

    // ================= helpers =================

    private Room room(long id) {
        return Room.builder().id(id).name("Room " + id).build();
    }

    private Role role(long id, Room room) {
        return Role.builder().id(id).name("Role " + id).room(room).build();
    }

    private Account account(Room room, Role... existing) {
        return Account.builder()
                .id(ACCOUNT_ID)
                .room(room)
                .roles(new HashSet<>(java.util.Arrays.asList(existing)))
                .build();
    }

    // ================= createRole =================

    @Nested
    @DisplayName("createRole")
    class CreateRole {

        @Test
        @DisplayName("throws NotFoundException when the room does not exist")
        void roomNotFound() {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> roleService.createRole(ROOM_ID, new RoleCreateRequest("Admin")));

            verify(roleRepository, never()).existsByNameAndRoom(anyString(), any());
            verify(roleRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws AlreadyExistsException when a same-named role exists in the room")
        void duplicateName() {
            Room room = room(ROOM_ID);
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
            when(roleRepository.existsByNameAndRoom("Admin", room)).thenReturn(true);

            assertThrows(AlreadyExistsException.class,
                    () -> roleService.createRole(ROOM_ID, new RoleCreateRequest("Admin")));

            verify(roleRepository, never()).save(any());
        }

        @Test
        @DisplayName("persists a new role bound to the room and the requested name")
        void validSaves() {
            Room room = room(ROOM_ID);
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
            when(roleRepository.existsByNameAndRoom("Admin", room)).thenReturn(false);

            roleService.createRole(ROOM_ID, new RoleCreateRequest("Admin"));

            ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
            verify(roleRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Admin");
            assertThat(captor.getValue().getRoom()).isSameAs(room);
        }
    }

    // ================= updateRole =================

    @Nested
    @DisplayName("updateRole")
    class UpdateRole {

        @Test
        @DisplayName("throws NotFoundException when the role does not exist")
        void roleNotFound() {
            when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> roleService.updateRole(ROLE_ID, new RoleUpdateRequest("Mod")));

            verify(roleRepository, never()).save(any());
        }

        @Test
        @DisplayName("applies the request via the mapper and saves the role")
        void validUpdatesAndSaves() {
            Role role = role(ROLE_ID, room(ROOM_ID));
            RoleUpdateRequest request = new RoleUpdateRequest("Mod");
            when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role));

            roleService.updateRole(ROLE_ID, request);

            verify(roleMapper).updateRoleFromRequest(request, role);
            verify(roleRepository).save(role);
        }
    }

    // ================= deleteRole =================

    @Test
    @DisplayName("deleteRole delegates straight to the repository")
    void deleteRole() {
        roleService.deleteRole(ROLE_ID);
        verify(roleRepository).deleteById(ROLE_ID);
    }

    // ================= getRole =================

    @Nested
    @DisplayName("getRole")
    class GetRole {

        @Test
        @DisplayName("throws NotFoundException when the role does not exist")
        void roleNotFound() {
            when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> roleService.getRole(ROLE_ID));
        }

        @Test
        @DisplayName("returns the mapped response for an existing role")
        void returnsMapped() {
            Role role = role(ROLE_ID, room(ROOM_ID));
            RoleResponse mapped = new RoleResponse(ROLE_ID, role.getName());
            when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role));
            when(roleMapper.toRoleResponse(role)).thenReturn(mapped);

            assertThat(roleService.getRole(ROLE_ID)).isSameAs(mapped);
        }
    }

    // ================= getAllRoomRoles / getAllRoomRolesEdit =================

    @Nested
    @DisplayName("listing")
    class Listing {

        @Test
        @DisplayName("getAllRoomRoles maps every room role to a response")
        void getAllRoomRoles() {
            Role a = role(1L, room(ROOM_ID));
            Role b = role(2L, room(ROOM_ID));
            when(roleRepository.findByRoomId(ROOM_ID)).thenReturn(List.of(a, b));
            when(roleMapper.toRoleResponse(a)).thenReturn(new RoleResponse(1L, a.getName()));
            when(roleMapper.toRoleResponse(b)).thenReturn(new RoleResponse(2L, b.getName()));

            List<RoleResponse> result = roleService.getAllRoomRoles(ROOM_ID);

            assertThat(result).extracting(RoleResponse::id).containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("getAllRoomRoles returns an empty list when the room has no roles")
        void getAllRoomRolesEmpty() {
            when(roleRepository.findByRoomId(ROOM_ID)).thenReturn(List.of());

            assertThat(roleService.getAllRoomRoles(ROOM_ID)).isEmpty();
            verifyNoInteractions(roleMapper);
        }

        @Test
        @DisplayName("getAllRoomRolesEdit enriches each role with its assigned user names")
        void getAllRoomRolesEdit() {
            Role a = role(1L, room(ROOM_ID));
            when(roleRepository.findByRoomId(ROOM_ID)).thenReturn(List.of(a));
            when(accountRepository.findNamesByRoleId(1L)).thenReturn(List.of("Ann", "Bob"));
            when(roleMapper.toEditResponse(a, List.of("Ann", "Bob")))
                    .thenReturn(new RoleEditResponse(1L, a.getName(), new String[]{"Ann", "Bob"}));

            List<RoleEditResponse> result = roleService.getAllRoomRolesEdit(ROOM_ID);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().users()).containsExactly("Ann", "Bob");
        }
    }

    // ================= assignRolesToAccount =================

    @Nested
    @DisplayName("assignRolesToAccount")
    class AssignRolesToAccount {

        @Test
        @DisplayName("throws NotFoundException when the account does not exist")
        void accountNotFound() {
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> roleService.assignRolesToAccount(ACCOUNT_ID, List.of(1L)));
        }

        @Test
        @DisplayName("throws NotFoundException when a requested role id is missing")
        void someRoleIdMissing() {
            Room r = room(ROOM_ID);
            Account account = account(r);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            // only role 1 is returned though both 1 and 2 were requested
            when(roleRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(role(1L, r)));

            assertThrows(NotFoundException.class,
                    () -> roleService.assignRolesToAccount(ACCOUNT_ID, List.of(1L, 2L)));
            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws DatabaseConstaintException when a role belongs to another room")
        void roleFromAnotherRoom() {
            Room accountRoom = room(10L);
            Room otherRoom = room(20L);
            Account account = account(accountRoom);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(roleRepository.findAllById(List.of(5L))).thenReturn(List.of(role(5L, otherRoom)));

            assertThrows(DatabaseConstaintException.class,
                    () -> roleService.assignRolesToAccount(ACCOUNT_ID, List.of(5L)));
            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("adds matching-room roles to the account and saves it")
        void allRolesMatchRoom() {
            Room r = room(ROOM_ID);
            Account account = account(r);
            Role newRole = role(5L, r);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(roleRepository.findAllById(List.of(5L))).thenReturn(List.of(newRole));

            roleService.assignRolesToAccount(ACCOUNT_ID, List.of(5L));

            assertThat(account.getRoles()).containsExactly(newRole);
            verify(accountRepository).save(account);
        }

        @Test
        @DisplayName("existing account roles are retained (assignment is additive)")
        void existingRolesRetained() {
            Room r = room(ROOM_ID);
            Role existing = role(1L, r);
            Role added = role(2L, r);
            Account account = account(r, existing);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(roleRepository.findAllById(List.of(2L))).thenReturn(List.of(added));

            roleService.assignRolesToAccount(ACCOUNT_ID, List.of(2L));

            assertThat(account.getRoles()).containsExactlyInAnyOrder(existing, added);
            verify(accountRepository).save(account);
        }
    }

    // ================= resolveRoles =================

    @Nested
    @DisplayName("resolveRoles")
    class ResolveRoles {

        @Test
        @DisplayName("null or empty id list returns an empty set without touching the repository")
        void nullOrEmpty() {
            assertThat(roleService.resolveRoles(null)).isEmpty();
            assertThat(roleService.resolveRoles(List.of())).isEmpty();
            verifyNoInteractions(roleRepository);
        }

        @Test
        @DisplayName("throws NotFoundException when an id cannot be resolved")
        void someRoleIdMissing() {
            when(roleRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(role(1L, room(ROOM_ID))));

            assertThrows(NotFoundException.class, () -> roleService.resolveRoles(List.of(1L, 2L)));
        }

        @Test
        @DisplayName("returns a de-duplicated set of the resolved roles")
        void validIdsReturnSet() {
            Role a = role(1L, room(ROOM_ID));
            Role b = role(2L, room(ROOM_ID));
            when(roleRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(a, b, a));

            Set<Role> result = roleService.resolveRoles(List.of(1L, 2L));

            assertThat(result).containsExactlyInAnyOrder(a, b);
        }
    }

    // ================= updateRolesLore =================

    @Nested
    @DisplayName("updateRolesLore")
    class UpdateRolesLore {

        @Test
        @DisplayName("throws NotFoundException when an id is missing, leaving the lore untouched")
        void someRoleIdMissing() {
            Lore lore = Lore.builder().roles(new HashSet<>()).build();
            when(roleRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(role(1L, room(ROOM_ID))));

            assertThrows(NotFoundException.class,
                    () -> roleService.updateRolesLore(lore, Set.of(1L, 2L)));
        }

        @Test
        @DisplayName("replaces the lore's roles with the resolved set")
        void validIdsReplacesRoles() {
            Role stale = role(99L, room(ROOM_ID));
            Lore lore = Lore.builder().roles(new HashSet<>(Set.of(stale))).build();
            Role a = role(1L, room(ROOM_ID));
            Role b = role(2L, room(ROOM_ID));
            when(roleRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(a, b));

            Lore result = roleService.updateRolesLore(lore, Set.of(1L, 2L));

            assertThat(result).isSameAs(lore);
            assertThat(lore.getRoles()).containsExactlyInAnyOrder(a, b);
        }
    }

    // ================= updateRolesCharacter =================

    @Nested
    @DisplayName("updateRolesCharacter")
    class UpdateRolesCharacter {

        @Test
        @DisplayName("throws NotFoundException when an id is missing")
        void someRoleIdMissing() {
            Character character = Character.builder().roles(new HashSet<>()).build();
            when(roleRepository.findAllById(Set.of(1L))).thenReturn(List.of());

            assertThrows(NotFoundException.class,
                    () -> roleService.updateRolesCharacter(character, Set.of(1L)));
        }

        @Test
        @DisplayName("replaces the character's roles with the resolved set")
        void validIdsReplacesRoles() {
            Role stale = role(99L, room(ROOM_ID));
            Character character = Character.builder().roles(new HashSet<>(Set.of(stale))).build();
            Role a = role(1L, room(ROOM_ID));
            when(roleRepository.findAllById(Set.of(1L))).thenReturn(List.of(a));

            Character result = roleService.updateRolesCharacter(character, Set.of(1L));

            assertThat(result).isSameAs(character);
            assertThat(character.getRoles()).containsExactly(a);
        }
    }
}
