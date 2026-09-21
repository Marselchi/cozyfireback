package com.cozyfireplace.server.accounts.current;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.AccountRepository;
import com.cozyfireplace.server.auth.UserDetailsImpl;
import com.cozyfireplace.server.auth.profile.Profile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentAccountResolver {
    private final AccountRepository accountRepository;

    public Account resolve(Long roomId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("User not authenticated");
        }

        Profile currentUser = ((UserDetailsImpl) authentication.getPrincipal()).getProfile();
        return accountRepository.findByProfileAndRoomId(currentUser, roomId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Profile not associated with room: " + roomId
                ));
    }

}
