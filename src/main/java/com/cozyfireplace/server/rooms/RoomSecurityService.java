package com.cozyfireplace.server.rooms;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.current.CurrentAccountResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomSecurityService {

    private final CurrentAccountResolver accountResolver;
    private final RoomRepository roomRepository;

    /**
     * Проверяет, является ли текущий пользователь создателем комнаты, извлекая аккаунт.
     * @param roomId ID комнаты
     * @return true, если пользователь - создатель комнаты
     */
    public boolean isCreator(Long roomId) {
        Account currentAccount = accountResolver.resolve(roomId);
        Room room = roomRepository.getReferenceById(roomId);
        return currentAccount.getId().equals(room.getCreator().getId());
    }

    /**
     * Проверяет, является ли указанный аккаунт создателем комнаты.
     * Используйте этот метод, если аккаунт уже был извлечен
     * @param account Аккаунт, права которого проверяются
     * @return true, если аккаунт - создатель комнаты
     */
    public boolean isCreator(Account account) {
        if (account == null || account.getRoom() == null || account.getRoom().getCreator() == null) {
            // Если аккаунт или его комната некорректны, он точно не создатель
            return false;
        }
        // Сравниваем ID аккаунта с ID создателя комнаты, к которой привязан сам аккаунт
        return account.getRoom().getCreator().getId().equals(account.getId());
    }

    // На всякий написал
    public boolean hasRole(Long roomId, String roleName) {
        Account account = accountResolver.resolve(roomId);
        return account.getRoles().stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }
}
