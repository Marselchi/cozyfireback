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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final RoomRepository roomRepository;
    private final RoleRepository roleRepository;
    private final RoomService roomService;
    private final RoleService roleService;
    private final AccountMapper accountMapper;


    public List<RoomAccountResponse> getAllAccountsInRoom(Long roomId, Account account) {
        List<Account> accountList = accountRepository.findAllByRoomIdAndNotId(roomId,account.getId());
        return accountList.stream().map(accountMapper::toRoomAccountResponse).toList();
    }

    public void createAccount(Long roomId) {
        Profile currentProfile = roomService.getCurrentAuthenticatedProfile();
        Room room = roomRepository.getReferenceById(roomId);
        Account account = Account.builder()
                .room(room)
                .name("Лучше поменять")
                .profile(currentProfile)
                .build();
        accountRepository.save(account);
    }

    public void deleteAccountRole(Long accountId, Long roleId) {
        Account account = accountRepository.findById(accountId).orElseThrow(
                () -> new NotFoundException("Account", accountId)
        );

        boolean wasRemoved = account.getRoles().removeIf(role -> role.getId().equals(roleId));
        if (!wasRemoved) {
            throw new NotFoundException("Account", "Role", roleId);
        }
    }

    public void addAccountRole(Long accountId, Long roleId) {
        Account account = accountRepository.findById(accountId).orElseThrow(
                () -> new NotFoundException("Account", accountId)
        );

        Role role =  roleRepository.findById(roleId).orElseThrow(
                () -> new NotFoundException("Role", roleId)
        );
        boolean added = account.getRoles().add(role);
        if (!added) {
            throw new AlreadyExistsException("Role", roleId);
        }
        accountRepository.save(account);
    }

    public void updateSelf(Account account, AccountUpdateSelfRequest request) {
        accountRepository.save(accountMapper.updateSelf(request,account));

    }

    public void updateAccountRole(Long accountId, AccountUpdateRoleRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account", accountId));

        Long oldRoleId = request.oldRoleId();
        Long newRoleId = request.newRoleId();

        boolean hasOldRole = account.getRoles().stream()
                .anyMatch(role -> role.getId().equals(oldRoleId));

        if (!hasOldRole) {
            throw new NotFoundException("Account", "Role", oldRoleId);
        }

        // Проверяем, что новая роль ещё не назначена (опционально)
        boolean alreadyHasNewRole = account.getRoles().stream()
                .anyMatch(role -> role.getId().equals(newRoleId));

        if (alreadyHasNewRole) {
            throw new AlreadyExistsException("Account", "Role", newRoleId);
        }

        account.getRoles().removeIf(role -> role.getId().equals(oldRoleId));

        Role newRole = roleRepository.findById(newRoleId)
                .orElseThrow(() -> new NotFoundException("Role", newRoleId));

        account.getRoles().add(newRole);

        accountRepository.save(account);
    }
}
