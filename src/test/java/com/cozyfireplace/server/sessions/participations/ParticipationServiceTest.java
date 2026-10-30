package com.cozyfireplace.server.sessions.participations;

import com.cozyfireplace.server.accounts.Account;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure Mockito unit tests for {@link ParticipationService}.
 * <p>
 * The {@link ParticipationRepository} is mocked, so only the service's own logic is
 * exercised: composite-key construction, invitation acknowledgement transitions, the
 * status-reset on session reschedule, and delegation on read. JPA persistence semantics
 * are out of scope.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ParticipationService")
class ParticipationServiceTest {

    private static final Long SESSION_ID = 3L;

    @Mock
    private ParticipationRepository participationRepository;

    @InjectMocks
    private ParticipationService service;

    private Account account() {
        Account account = mock(Account.class);
        when(account.getId()).thenReturn(42L);
        return account;
    }

    @Nested
    @DisplayName("createParticipations")
    class CreateParticipations {

        @Test
        @DisplayName("saves one pending (accepted = null) participation per account id")
        void savesEachAccount() {
            service.createParticipations(SESSION_ID, List.of(10L, 20L));

            ArgumentCaptor<Participation> captor = ArgumentCaptor.forClass(Participation.class);
            verify(participationRepository, times(2)).save(captor.capture());

            List<Participation> saved = captor.getAllValues();
            assertThat(saved).extracting(p -> p.getId().getAccountId()).containsExactly(10L, 20L);
            assertThat(saved).allSatisfy(p -> {
                assertThat(p.getId().getSessionId()).isEqualTo(SESSION_ID);
                assertThat(p.getAccepted()).isNull();
            });
        }

        @Test
        @DisplayName("empty id list persists nothing")
        void emptyListNoSaves() {
            service.createParticipations(SESSION_ID, List.of());

            verify(participationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("acknowledgeParticipation")
    class Acknowledge {

        @Test
        @DisplayName("looks up by the (session, account) key and stores the new status")
        void updatesAndSaves() {
            Account account = account();
            Participation existing = Participation.builder()
                    .id(new ParticipationId(SESSION_ID, 42L))
                    .accepted(null)
                    .build();
            when(participationRepository.findById(new ParticipationId(SESSION_ID, 42L)))
                    .thenReturn(Optional.of(existing));

            service.acknowledgeParticipation(SESSION_ID, account, true);

            assertThat(existing.getAccepted()).isTrue();
            verify(participationRepository).save(existing);
        }

        @Test
        @DisplayName("throws NotFound when no invitation exists")
        void throwsWhenMissing() {
            Account account = account();
            when(participationRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.acknowledgeParticipation(SESSION_ID, account, false))
                    .isInstanceOf(NotFoundException.class);
            verify(participationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("resetParticipationStatuses")
    class Reset {

        @Test
        @DisplayName("clears accepted to null for every participant and batch-saves")
        void clearsAll() {
            Participation a = Participation.builder().accepted(true).build();
            Participation b = Participation.builder().accepted(false).build();
            when(participationRepository.findAllByIdSessionId(SESSION_ID)).thenReturn(List.of(a, b));

            service.resetParticipationStatuses(SESSION_ID);

            assertThat(a.getAccepted()).isNull();
            assertThat(b.getAccepted()).isNull();
            verify(participationRepository).saveAll(List.of(a, b));
        }
    }

    @Nested
    @DisplayName("findBySessionId")
    class FindBySession {

        @Test
        @DisplayName("delegates straight to the repository")
        void delegates() {
            List<Participation> rows = List.of(Participation.builder().build());
            when(participationRepository.findAllByIdSessionId(SESSION_ID)).thenReturn(rows);

            assertThat(service.findBySessionId(SESSION_ID)).isSameAs(rows);
        }

        @Test
        @DisplayName("session with no participants returns an empty list")
        void empty() {
            when(participationRepository.findAllByIdSessionId(SESSION_ID)).thenReturn(List.of());

            assertThat(service.findBySessionId(SESSION_ID)).isEmpty();
        }
    }
}
