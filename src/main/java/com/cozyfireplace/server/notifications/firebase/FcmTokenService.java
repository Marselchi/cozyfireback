package com.cozyfireplace.server.notifications.firebase;

import com.cozyfireplace.server.accounts.AccountRepository;
import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.auth.profile.ProfileRepository;
import com.cozyfireplace.server.auth.profile.ProfileService;
import com.cozyfireplace.server.util.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final FcmTokenRepository repository;
    private final ProfileService profileService;
    private final AccountRepository accountRepository;

    @Transactional
    public void registerFid(String fid) {
        var existing = repository.findByFid(fid);
        Profile profile = profileService.getCurrentProfile();
        if (existing.isPresent()) {
            FcmToken t = existing.get();
            t.setProfile(profile);
            t.setActive(true);
            t.setLastUsedAt(Instant.now());
            return;
        }

        repository.save(FcmToken.builder()
                .profile(profile)
                .fid(fid)
                .active(true)
                .createdAt(Instant.now())
                .lastUsedAt(Instant.now())
                .build());
    }

    @Transactional
    public void deactivateFid(String fid) {
        repository.findByFid(fid).ifPresent(t -> {
            t.setActive(false);
        });
    }

    public List<String> getActiveFids(Long accountId) {
        Long profileId = accountRepository.findProfileIdByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found"));

        return repository.findFidsByProfileIdAndActiveTrue(profileId);
    }
}
