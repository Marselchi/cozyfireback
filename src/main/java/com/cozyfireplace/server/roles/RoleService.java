package com.cozyfireplace.server.roles;


import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.AccountRepository;
import com.cozyfireplace.server.characters.Character;
import com.cozyfireplace.server.lore.Lore;
import com.cozyfireplace.server.lore.LoreRepository;
import com.cozyfireplace.server.roles.dto.*;
import com.cozyfireplace.server.rooms.Room;
import com.cozyfireplace.server.rooms.RoomRepository;
import com.cozyfireplace.server.util.exception.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    private final RoleMapper roleMapper;

    public void createRole(Long roomId, RoleCreateRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room", roomId));

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

    public void updateRole(Long roleId, RoleUpdateRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role", roleId));
        roleMapper.updateRoleFromRequest(request, role);
        roleRepository.save(role);
    }

    public void deleteRole(Long roleId) {

        //TODO: some checks mb and deleting of role in lore

        roleRepository.deleteById(roleId);
    }

    public RoleResponse getRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role", roleId));

        return roleMapper.toRoleResponse(role);

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

        List<Role> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            Set<Long> foundIds = roles.stream().map(Role::getId).collect(Collectors.toSet());
            roleIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .findFirst()
                    .ifPresent(missingId -> {
                        throw new NotFoundException("Role", missingId);
                    });
        }

        Long accountRoomId = account.getRoom().getId();
        for (Role role : roles) {
            if (!accountRoomId.equals(role.getRoom().getId())) {
                throw new DatabaseConstaintException(
                        "Role " + role.getId() + " belongs to room " + role.getRoom().getId() +
                                ", but account belongs to room " + accountRoomId
                );
            }
        }

        account.getRoles().addAll(roles);
        accountRepository.save(account);
    }

    public Set<Role> resolveRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return Set.of();

        List<Role> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            Set<Long> foundIds = roles.stream().map(Role::getId).collect(Collectors.toSet());
            roleIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .findFirst()
                    .ifPresent(missingId -> { throw new NotFoundException("Role", missingId); });
        }
        return new HashSet<>(roles);
    }

    public Lore updateRolesLore(Lore lore, Set<Long> roleIds) {
        List<Role> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            Set<Long> foundIds = roles.stream().map(Role::getId).collect(Collectors.toSet());
            roleIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .findFirst()
                    .ifPresent(missingId -> {
                        throw new NotFoundException("Role", missingId);
                    });
        }
        lore.getRoles().clear();
        lore.getRoles().addAll(roles);
        return lore;
    }

    public Character updateRolesCharacter(Character character, Set<Long> roleIds) {
        List<Role> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            Set<Long> foundIds = roles.stream().map(Role::getId).collect(Collectors.toSet());
            roleIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .findFirst()
                    .ifPresent(missingId -> {
                        throw new NotFoundException("Role", missingId);
                    });
        }
        character.getRoles().clear();
        character.getRoles().addAll(roles);
        return character;
    }

}