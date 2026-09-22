package com.cozyfireplace.server.sessions;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.sessions.dto.SessionRequest;
import com.cozyfireplace.server.sessions.dto.SessionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SessionController}.
 * <p>
 * The controller is a thin HTTP facade over {@link SessionService}; each test locks the
 * status code, the body passthrough and which path variables reach the service. Note the
 * acknowledge / current-month endpoints accept a room id but ignore it (the source marks
 * them {@code @SuppressWarnings}), and the range endpoint forwards the parsed
 * instants unchanged.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SessionController")
class SessionControllerTest {

    private static final Long ROOM_ID = 1L;
    private static final Long SESSION_ID = 3L;

    @Mock
    private SessionService sessionService;

    @InjectMocks
    private SessionController controller;

    @Nested
    @DisplayName("POST /{roomId}")
    class Create {

        @Test
        @DisplayName("returns 200 with the created session")
        void returnsBody() {
            Account account = mock(Account.class);
            SessionRequest request = mock(SessionRequest.class);
            SessionResponse body = mock(SessionResponse.class);
            when(sessionService.createSession(ROOM_ID, account, request)).thenReturn(body);

            ResponseEntity<SessionResponse> response = controller.create(ROOM_ID, request, account);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
        }
    }

    @Nested
    @DisplayName("PUT /{sessionId}")
    class Update {

        @Test
        @DisplayName("returns 200 with the updated session")
        void returnsBody() {
            SessionRequest request = mock(SessionRequest.class);
            SessionResponse body = mock(SessionResponse.class);
            when(sessionService.updateSession(SESSION_ID, request)).thenReturn(body);

            ResponseEntity<SessionResponse> response = controller.update(SESSION_ID, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
        }
    }

    @Nested
    @DisplayName("DELETE /{sessionId}")
    class Delete {

        @Test
        @DisplayName("returns 204 and deletes by id")
        void returnsNoContent() {
            ResponseEntity<Void> response = controller.delete(SESSION_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(sessionService).deleteSession(SESSION_ID);
        }
    }

    @Nested
    @DisplayName("GET /{sessionId}")
    class GetSession {

        @Test
        @DisplayName("returns 200 with the session")
        void returnsBody() {
            SessionResponse body = mock(SessionResponse.class);
            when(sessionService.getSession(SESSION_ID)).thenReturn(body);

            ResponseEntity<SessionResponse> response = controller.getSession(SESSION_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
        }
    }

    @Nested
    @DisplayName("GET /{roomId}/range")
    class GetByPeriod {

        @Test
        @DisplayName("returns 200 and forwards the room and instants")
        void returnsList() {
            Instant start = Instant.parse("2026-09-01T00:00:00Z");
            Instant end = Instant.parse("2026-09-30T23:59:59Z");
            List<SessionResponse> body = List.of(mock(SessionResponse.class));
            when(sessionService.getSessionsByPeriod(ROOM_ID, start, end)).thenReturn(body);

            ResponseEntity<List<SessionResponse>> response = controller.getSessionsByPeriod(ROOM_ID, start, end);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
        }
    }

    @Nested
    @DisplayName("GET /{roomId}/current-month")
    class CurrentMonth {

        @Test
        @DisplayName("returns 200 and resolves the current month by room (account ignored)")
        void returnsList() {
            Account account = mock(Account.class);
            List<SessionResponse> body = List.of(mock(SessionResponse.class));
            when(sessionService.getSessionsCurrentMonth(ROOM_ID)).thenReturn(body);

            ResponseEntity<List<SessionResponse>> response = controller.getSessionsCurrentMonth(ROOM_ID, account);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
            verifyNoMoreInteractions(sessionService);
        }
    }

    @Nested
    @DisplayName("GET /{roomId}/upcoming")
    class Upcoming {

        @Test
        @DisplayName("returns 200 and forwards both room and account")
        void returnsList() {
            Account account = mock(Account.class);
            List<SessionResponse> body = List.of(mock(SessionResponse.class));
            when(sessionService.getUpcomingSessionsWithParticipant(ROOM_ID, account)).thenReturn(body);

            ResponseEntity<List<SessionResponse>> response = controller.getUpcomingSessions(ROOM_ID, account);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
        }
    }

    @Nested
    @DisplayName("POST /{roomId}/{sessionId}/acknowledge")
    class Acknowledge {

        @Test
        @DisplayName("returns 204 and acknowledges by session id (room id ignored)")
        void returnsNoContent() {
            Account account = mock(Account.class);

            ResponseEntity<Void> response = controller.acknowledge(ROOM_ID, SESSION_ID, true, account);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(sessionService).acknowledgeSession(SESSION_ID, account, true);
            verifyNoMoreInteractions(sessionService);
        }
    }
}
