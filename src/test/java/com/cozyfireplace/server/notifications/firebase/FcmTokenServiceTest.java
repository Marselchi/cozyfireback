package com.cozyfireplace.server.notifications.firebase;

import com.cozyfireplace.server.accounts.AccountRepository;
import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.auth.profile.ProfileService;
import com.cozyfireplace.server.util.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FcmTokenService}.
 * <p>
 * The FID repository, {@link ProfileService} and {@link AccountRepository} are mocked. Covers
 * registering a brand-new Firebase Installation ID (persisted) versus re-activating an existing
 * one (mutated in place, not re-saved), deactivation, and the account-to-profile FID lookup that
 * scopes push delivery.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FcmTokenService")
class FcmTokenServiceTest {

    private static final String FID = "firebase-installation-id";
    private static final Long ACCOUNT_ID = 7L;
    private static final Long PROFILE_ID = 42L;

    @Mock
    private FcmTokenRepository repository;
    @Mock
    private ProfileService profileService;
    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private FcmTokenService service;

    @Nested
    @DisplayName("registerFid")
    class Register {

        @Test
        @DisplayName("persists a new active fid bound to the current profile")
        void createsNew() {
            Profile profile = Profile.builder().username("user").build();
            when(repository.findByFid(FID)).thenReturn(Optional.empty());
            when(profileService.getCurrentProfile()).thenReturn(profile);

            service.registerFid(FID);

            ArgumentCaptor<FcmToken> captor = ArgumentCaptor.forClass(FcmToken.class);
            verify(repository).save(captor.capture());
            FcmToken saved = captor.getValue();
            assertThat(saved.getFid()).isEqualTo(FID);
            assertThat(saved.getProfile()).isSameAs(profile);
            assertThat(saved.isActive()).isTrue();
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("re-activates an existing fid in place without saving")
        void reactivatesExisting() {
            Profile profile = Profile.builder().username("user").build();
            FcmToken existing = FcmToken.builder().fid(FID).active(false).build();
            when(repository.findByFid(FID)).thenReturn(Optional.of(existing));
            when(profileService.getCurrentProfile()).thenReturn(profile);

            service.registerFid(FID);

            assertThat(existing.getProfile()).isSameAs(profile);
            assertThat(existing.isActive()).isTrue();
            assertThat(existing.getLastUsedAt()).isNotNull();
            verify(repository, never()).save(any());
        }
    }

    @Test
    @DisplayName("deactivateFid flips a known fid inactive")
    void deactivateKnown() {
        FcmToken existing = FcmToken.builder().fid(FID).active(true).build();
        when(repository.findByFid(FID)).thenReturn(Optional.of(existing));

        service.deactivateFid(FID);

        assertThat(existing.isActive()).isFalse();
    }

    @Test
    @DisplayName("deactivateFid is a no-op for an unknown fid")
    void deactivateUnknown() {
        when(repository.findByFid(FID)).thenReturn(Optional.empty());

        service.deactivateFid(FID);

        verify(repository, never()).save(any());
    }

    @Nested
    @DisplayName("getActiveFids")
    class GetActive {

        @Test
        @DisplayName("resolves the account's profile then returns its active fids")
        void returns() {
            when(accountRepository.findProfileIdByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(PROFILE_ID));
            when(repository.findFidsByProfileIdAndActiveTrue(PROFILE_ID)).thenReturn(List.of("a", "b"));

            assertThat(service.getActiveFids(ACCOUNT_ID)).containsExactly("a", "b");
        }

        @Test
        @DisplayName("throws when the account has no backing profile")
        void accountMissing() {
            when(accountRepository.findProfileIdByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getActiveFids(ACCOUNT_ID))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
