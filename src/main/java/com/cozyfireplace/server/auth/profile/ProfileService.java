package com.cozyfireplace.server.auth.profile;


import com.cozyfireplace.server.auth.UserDetailsImpl;
import com.cozyfireplace.server.util.exception.ForbiddenException;
import com.cozyfireplace.server.util.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;

    public Profile getCurrentProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ForbiddenException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserDetailsImpl userDetails)) {
            // На всякий случай: если Principal вдруг не наш тип (защита от ошибок конфигурации)
            throw new NotFoundException("Profile not found in security context");
        }

        return userDetails.getProfile();
    }
}
