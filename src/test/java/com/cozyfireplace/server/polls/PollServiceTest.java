package com.cozyfireplace.server.polls;

import com.cozyfireplace.server.auth.UserDetailsImpl;
import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.polls.dto.PollCreateRequest;
import com.cozyfireplace.server.polls.dto.PollMapper;
import com.cozyfireplace.server.polls.dto.PollResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure Mockito unit tests for {@link PollService}.
 * <p>
 * Collaborators are mocked and the security context is manipulated directly, since the
 * service reads the authenticated principal from {@link SecurityContextHolder} rather than
 * receiving it as an argument. Tests cover the anonymous short-circuit, the profile-bound
 * persistence and the source-scoped read mapping.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PollService")
class PollServiceTest {

    @Mock
    private PollRepository pollRepository;
    @Mock
    private PollMapper pollMapper;

    @InjectMocks
    private PollService service;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Profile profile) {
        var auth = new UsernamePasswordAuthenticationToken(
                UserDetailsImpl.build(profile), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("createPoll")
    class Create {

        @Test
        @DisplayName("does nothing when the caller is anonymous")
        void anonymousIgnored() {
            service.createPoll(new PollCreateRequest("landing", "text"));

            verifyNoInteractions(pollRepository);
        }

        @Test
        @DisplayName("persists a poll bound to the current profile")
        void persistsForAuthenticated() {
            Profile profile = new Profile();
            authenticate(profile);

            service.createPoll(new PollCreateRequest("landing", "text"));

            ArgumentCaptor<Poll> captor = ArgumentCaptor.forClass(Poll.class);
            verify(pollRepository).save(captor.capture());
            Poll saved = captor.getValue();
            assertThat(saved.getSource()).isEqualTo("landing");
            assertThat(saved.getContent()).isEqualTo("text");
            assertThat(saved.getProfile()).isSameAs(profile);
        }
    }

    @Nested
    @DisplayName("getPolls")
    class Get {

        @Test
        @DisplayName("maps every poll for the source")
        void maps() {
            Poll poll = Poll.builder().source("landing").build();
            when(pollRepository.getPollsBySource("landing")).thenReturn(List.of(poll));
            PollResponse response = mock(PollResponse.class);
            when(pollMapper.toPollResponse(poll)).thenReturn(response);

            assertThat(service.getPolls("landing")).containsExactly(response);
        }

        @Test
        @DisplayName("empty source returns an empty list without mapping")
        void empty() {
            when(pollRepository.getPollsBySource("x")).thenReturn(List.of());

            assertThat(service.getPolls("x")).isEmpty();
            verify(pollMapper, never()).toPollResponse(any());
        }
    }
}
