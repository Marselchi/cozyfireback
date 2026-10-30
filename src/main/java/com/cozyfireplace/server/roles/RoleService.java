package com.cozyfireplace.server.roles;


import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.AccountRepository;
import com.cozyfireplace.server.characters.Character;
import com.cozyfireplace.server.lore.Lore;
import com.cozyfireplace.server.lore.LoreRepository;
import com.cozyfireplace.server.roles.dto.*;
import com.cozyfireplace.server.rooms.Room;
import com.cozyfireplace.server.rooms.RoomRepository;
import com.cozyfireplace.server.rooms.RoomSecurityService;
import com.cozyfireplace.server.util.exception.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;
    private final RoomRepository roomRepository;
    private final AccountRepository accountRepository;
    private final LoreRepository loreRepository;
    private final RoomSecurityService roomSecurityService;
    private final RoleMapper roleMapper;

    public void createRole(Long roomId, RoleCreateRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room", roomId));

        assertCreator(roomId);

        if (roleRepository.existsByNameAndRoom(request.getName(), room)) {
            throw new AlreadyExistsException(
                    "Role '" + request.getName() + "' already exists in room: " + room.getName()
            );
        }

        roleRepository.save(
                Role.builder()
                        .name(request.getName())
                        .room(room)
                        .build()
        );
    }

    public void updateRole(Long roomId, Long roleId, RoleUpdateRequest request) {
        Role role = findInRoom(roomId, roleId);
        assertCreator(roomId);

        String newName = request.name();
        if (newName != null && !newName.equals(role.getName())
                && roleRepository.existsByNameAndRoom(newName, role.getRoom())) {
            throw new AlreadyExistsException(
                    "Role '" + newName + "' already exists in room: " + role.getRoom().getName()
            );
        }

        roleMapper.updateRoleFromRequest(request, role);
        roleRepository.save(role);
    }

    public void deleteRole(Long roomId, Long roleId) {
        Role role = findInRoom(roomId, roleId);
        assertCreator(roomId);
        roleRepository.delete(role);
    }

    public RoleResponse getRole(Long roomId, Long roleId) {
        return roleMapper.toRoleResponse(findInRoom(roomId, roleId));
    }

    /**
     * Room-scoped загрузку роли: в чужой комнате роли не видно (404 вместо тихой записи в чужую комнату).
     */
    private Role findInRoom(Long roomId, Long roleId) {
        return roleRepository.findByIdAndRoomId(roleId, roomId)
                .orElseThrow(() -> new NotFoundException("Role", roleId));
    }

    private void assertCreator(Long roomId) {
        if (!roomSecurityService.isCreator(roomId)) {
            throw new ForbiddenException("Управление ролями", "только создатель комнаты может управлять ролями");
        }
    }


    //TODO: generally refactor where methods are
    public List<RoleResponse> getAllRoomRoles(Long roomId) {
        List<Role> roles = roleRepository.findByRoomId(roomId);

        return roles.stream().map(roleMapper::toRoleResponse).collect(Collectors.toList());

    }

    public List<RoleEditResponse> getAllRoomRolesEdit(Long roomId) {
        return roleRepository.findByRoomId(roomId).stream()
                .map(role -> roleMapper.toEditResponse(
                        role,
                        accountRepository.findNamesByRoleId(role.getId())
                ))
                .toList();
    }



    public void assignRolesToAccount(Long accountId, List<Long> roleIds) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account", accountId));

        List<Role> roles = requireAllInRoom(roleIds, account.getRoom().getId());

        account.getRoles().addAll(roles);
        accountRepository.save(account);
    }

    /**
     * Разрешает роли для payload'а lore/character строго внутри комнаты:
     * чужой или несуществующий roleId отвечает 404 и не протаскивает роль из другой комнаты.
     */
    public Set<Role> resolveRoles(List<Long> roleIds, Long roomId) {
        if (roleIds == null || roleIds.isEmpty()) return Set.of();
        return new HashSet<>(requireAllInRoom(roleIds, roomId));
    }

    public Lore updateRolesLore(Lore lore, Set<Long> roleIds, Long roomId) {
        List<Role> roles = requireAllInRoom(roleIds, roomId);
        lore.getRoles().clear();
        lore.getRoles().addAll(roles);
        return lore;
    }

    public Character updateRolesCharacter(Character character, Set<Long> roleIds, Long roomId) {
        List<Role> roles = requireAllInRoom(roleIds, roomId);
        character.getRoles().clear();
        character.getRoles().addAll(roles);
        return character;
    }

    private List<Role> requireAllInRoom(Collection<Long> roleIds, Long roomId) {
        if (roleIds == null || roleIds.isEmpty()) return List.of();

        List<Long> requested = List.copyOf(roleIds);
        List<Role> found = roleRepository.findByIdInAndRoomId(requested, roomId);
        if (found.size() != requested.size()) {
            Set<Long> foundIds = found.stream().map(Role::getId).collect(Collectors.toSet());
            requested.stream()
                    .filter(id -> !foundIds.contains(id))
                    .findFirst()
                    .ifPresent(missingId -> {
                        throw new NotFoundException("Role", missingId);
                    });
        }
        return found;
    }

}
