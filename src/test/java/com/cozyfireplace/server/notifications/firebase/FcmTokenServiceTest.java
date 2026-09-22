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
 * The token repository, {@link ProfileService} and {@link AccountRepository} are mocked. Covers
 * registering a brand-new token (persisted) versus re-activating an existing one (mutated in
 * place, not re-saved), deactivation, and the account-to-profile token lookup that scopes push
 * delivery.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FcmTokenService")
class FcmTokenServiceTest {

    private static final String TOKEN = "device-token";
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
    @DisplayName("registerToken")
    class Register {

        @Test
        @DisplayName("persists a new active token bound to the current profile")
        void createsNew() {
            Profile profile = Profile.builder().username("user").build();
            when(repository.findByToken(TOKEN)).thenReturn(Optional.empty());
            when(profileService.getCurrentProfile()).thenReturn(profile);

            service.registerToken(TOKEN);

            ArgumentCaptor<FcmToken> captor = ArgumentCaptor.forClass(FcmToken.class);
            verify(repository).save(captor.capture());
            FcmToken saved = captor.getValue();
            assertThat(saved.getToken()).isEqualTo(TOKEN);
            assertThat(saved.getProfile()).isSameAs(profile);
            assertThat(saved.isActive()).isTrue();
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("re-activates an existing token in place without saving")
        void reactivatesExisting() {
            Profile profile = Profile.builder().username("user").build();
            FcmToken existing = FcmToken.builder().token(TOKEN).active(false).build();
            when(repository.findByToken(TOKEN)).thenReturn(Optional.of(existing));
            when(profileService.getCurrentProfile()).thenReturn(profile);

            service.registerToken(TOKEN);

            assertThat(existing.getProfile()).isSameAs(profile);
            assertThat(existing.isActive()).isTrue();
            assertThat(existing.getLastUsedAt()).isNotNull();
            verify(repository, never()).save(any());
        }
    }

    @Test
    @DisplayName("deactivateToken flips a known token inactive")
    void deactivateKnown() {
        FcmToken existing = FcmToken.builder().token(TOKEN).active(true).build();
        when(repository.findByToken(TOKEN)).thenReturn(Optional.of(existing));

        service.deactivateToken(TOKEN);

        assertThat(existing.isActive()).isFalse();
    }

    @Test
    @DisplayName("deactivateToken is a no-op for an unknown token")
    void deactivateUnknown() {
        when(repository.findByToken(TOKEN)).thenReturn(Optional.empty());

        service.deactivateToken(TOKEN);

        verify(repository, never()).save(any());
    }

    @Nested
    @DisplayName("getActiveTokens")
    class GetActive {

        @Test
        @DisplayName("resolves the account's profile then returns its active tokens")
        void returns() {
            when(accountRepository.findProfileIdByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(PROFILE_ID));
            when(repository.findTokensByProfileIdAndActiveTrue(PROFILE_ID)).thenReturn(List.of("a", "b"));

            assertThat(service.getActiveTokens(ACCOUNT_ID)).containsExactly("a", "b");
        }

        @Test
        @DisplayName("throws when the account has no backing profile")
        void accountMissing() {
            when(accountRepository.findProfileIdByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getActiveTokens(ACCOUNT_ID))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
