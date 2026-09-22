package com.cozyfireplace.server.sessions;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.AccountRepository;
import com.cozyfireplace.server.rooms.Room;
import com.cozyfireplace.server.rooms.RoomSecurityService;
import com.cozyfireplace.server.sessions.dto.*;
import com.cozyfireplace.server.sessions.participations.Participation;
import com.cozyfireplace.server.sessions.participations.ParticipationId;
import com.cozyfireplace.server.sessions.participations.ParticipationService;
import com.cozyfireplace.server.util.exception.ForbiddenException;
import com.cozyfireplace.server.util.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionService tests")
class SessionServiceTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private SessionMapper sessionMapper;
    @Mock private ParticipationService participationService;
    @Mock private AccountRepository accountRepository;
    @Mock private RoomSecurityService roomSecurityService;

    @InjectMocks
    private SessionService sessionService;

    // ====== HELPERS (lenient stubs) ======
    private Account testAccount(long id, String name, Room room) {
        Account acc = mock(Account.class);
        lenient().when(acc.getId()).thenReturn(id);
        lenient().when(acc.getName()).thenReturn(name);
        lenient().when(acc.getRoom()).thenReturn(room);
        return acc;
    }

    private Room testRoom(long id) {
        Room room = mock(Room.class);
        lenient().when(room.getId()).thenReturn(id);
        return room;
    }

    private Session testSession(long id, Account creator, Instant time) {
        Session session = mock(Session.class);
        lenient().when(session.getId()).thenReturn(id);
        lenient().when(session.getCreator()).thenReturn(creator);
        lenient().when(session.getTime()).thenReturn(time);
        return session;
    }

    private Participation testParticipation(long sessionId, long accountId, Boolean accepted) {
        ParticipationId pid = mock(ParticipationId.class);
        lenient().when(pid.getSessionId()).thenReturn(sessionId);
        lenient().when(pid.getAccountId()).thenReturn(accountId);

        Participation p = mock(Participation.class);
        lenient().when(p.getId()).thenReturn(pid);
        lenient().when(p.getAccepted()).thenReturn(accepted);
        return p;
    }

    // ====== CREATE SESSION ======
    @Nested
    @DisplayName("createSession")
    class CreateSessionTests {

        @Test
        @DisplayName("should create session successfully for room creator")
        void createSession_success() {
            long roomId = 1L;
            long sessionId = 100L, creatorId = 1L;
            Room room = testRoom(roomId);
            Account creator = testAccount(creatorId, "DM", room);
            SessionRequest request = mock(SessionRequest.class);
            Session session = mock(Session.class);
            SessionResponse response = mock(SessionResponse.class);

            when(roomSecurityService.isCreator(roomId)).thenReturn(true);
            when(request.accountIds()).thenReturn(null);
            when(sessionMapper.toEntity(request, creator)).thenReturn(session);
            when(sessionRepository.save(session)).thenAnswer(inv -> inv.getArgument(0));
            when(session.getId()).thenReturn(sessionId);
            when(sessionMapper.toResponse(eq(session), eq(List.of()))).thenReturn(response);

            SessionResponse result = sessionService.createSession(roomId, creator, request);

            assertEquals(response, result);
            verify(participationService, never()).createParticipations(anyLong(), anyList());
        }

        @Test
        @DisplayName("should create participations when accountIds provided")
        void createSession_withParticipants() {
            Long roomId = 1L, sessionId = 100L;
            Room room = testRoom(roomId);
            Account creator = testAccount(1L, "DM", room);
            Account invited = testAccount(2L, "Player", room);
            SessionRequest request = mock(SessionRequest.class);
            Session session = mock(Session.class);

            Set<Long> invitedIds = Set.of(2L);

            when(roomSecurityService.isCreator(roomId)).thenReturn(true);
            when(request.accountIds()).thenReturn(invitedIds);

            // ВАЖНО: any(Iterable.class), а не anyList() — findAllById принимает Iterable
            when(accountRepository.findAllById(any()))
                    .thenReturn(List.of(invited));

            when(sessionMapper.toEntity(request, creator)).thenReturn(session);
            when(sessionRepository.save(session)).thenAnswer(inv -> inv.getArgument(0));
            when(session.getId()).thenReturn(sessionId);
            when(sessionMapper.toResponse(eq(session), anyList())).thenReturn(mock(SessionResponse.class));

            sessionService.createSession(roomId, creator, request);

            // Верификация: проверяем содержимое, а не ссылку
            verify(participationService).createParticipations(
                    eq(sessionId),
                    argThat(ids -> ids != null && ids.contains(2L) && ids.size() == 1)
            );
        }

        @Test
        @DisplayName("should throw ForbiddenException if not room creator")
        void createSession_notCreator() {
            when(roomSecurityService.isCreator(1L)).thenReturn(false);

            assertThrows(ForbiddenException.class,
                    () -> sessionService.createSession(1L, mock(Account.class), mock(SessionRequest.class)));
        }

    }

    // ====== UPDATE SESSION ======
    @Nested
    @DisplayName("updateSession")
    class UpdateSessionTests {

        @Test
        @DisplayName("should update session successfully")
        void updateSession_success() {
            long sessionId = 100L;
            long roomId = 1L;
            Room room = testRoom(roomId);
            Account creator = testAccount(1L, "DM", room);
            Session session = testSession(sessionId, creator, Instant.now());
            SessionRequest request = mock(SessionRequest.class);
            Instant newTime = Instant.now().plusSeconds(3600);

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(roomSecurityService.isCreator(roomId)).thenReturn(true);
            when(request.time()).thenReturn(newTime);
            when(request.description()).thenReturn("Updated");
            when(sessionRepository.save(session)).thenAnswer(inv -> inv.getArgument(0));
            when(sessionMapper.toResponse(eq(session), anyList())).thenReturn(mock(SessionResponse.class));

            SessionResponse result = sessionService.updateSession(sessionId, request);

            assertNotNull(result);
            verify(session).setTime(newTime);
            verify(session).setDescription("Updated");
        }

        @Test
        @DisplayName("should throw ForbiddenException if not room creator")
        void updateSession_notCreator() {
            long sessionId = 100L;
            long roomId = 1L;
            Room room = testRoom(roomId);
            Account creator = testAccount(1L, "DM", room);
            Session session = testSession(sessionId, creator, Instant.now());

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(roomSecurityService.isCreator(roomId)).thenReturn(false);

            assertThrows(ForbiddenException.class,
                    () -> sessionService.updateSession(sessionId, mock(SessionRequest.class)));
        }

        @Test
        @DisplayName("should throw NotFoundException if session not found")
        void updateSession_notFound() {
            when(sessionRepository.findById(100L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> sessionService.updateSession(100L, mock(SessionRequest.class)));
        }
    }

    // ====== DELETE SESSION ======
    @Nested
    @DisplayName("deleteSession")
    class DeleteSessionTests {

        @Test
        @DisplayName("should delete session successfully")
        void deleteSession_success() {
            long sessionId = 100L;
            long roomId = 1L;
            Room room = testRoom(roomId);
            Account creator = testAccount(1L, "DM", room);
            Session session = testSession(sessionId, creator, Instant.now());

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(roomSecurityService.isCreator(roomId)).thenReturn(true);

            sessionService.deleteSession(sessionId);

            verify(sessionRepository).delete(session);
        }

        @Test
        @DisplayName("should throw ForbiddenException if not room creator")
        void deleteSession_notCreator() {
            long sessionId = 100L;
            long roomId = 1L;
            Room room = testRoom(roomId);
            Account creator = testAccount(1L, "DM", room);
            Session session = testSession(sessionId, creator, Instant.now());

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(roomSecurityService.isCreator(roomId)).thenReturn(false);

            assertThrows(ForbiddenException.class,
                    () -> sessionService.deleteSession(sessionId));
        }
    }

    // ====== GET SESSION ======
    @Nested
    @DisplayName("getSession")
    class GetSessionTests {

        @Test
        @DisplayName("should return session with participants")
        void getSession_success() {
            long sessionId = 100L;
            Room room = testRoom(1L);
            Account creator = testAccount(1L, "DM", room);
            Session session = testSession(sessionId, creator, Instant.now());
            Participation p1 = testParticipation(sessionId, 1L, true);
            Participation p2 = testParticipation(sessionId, 2L, null);
            Account player = testAccount(2L, "Player", room);
            List<IdNameBool> participants = List.of(
                    new IdNameBool(1L, "DM", true),
                    new IdNameBool(2L, "Player", null)
            );

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(participationService.findBySessionId(sessionId)).thenReturn(List.of(p1, p2));
            when(accountRepository.findById(1L)).thenReturn(Optional.of(creator));
            when(accountRepository.findById(2L)).thenReturn(Optional.of(player));
            when(sessionMapper.toResponse(eq(session), eq(participants))).thenReturn(mock(SessionResponse.class));

            SessionResponse result = sessionService.getSession(sessionId);

            assertNotNull(result);
            verify(sessionMapper).toResponse(eq(session), eq(participants));
        }

        @Test
        @DisplayName("should throw NotFoundException if session not found")
        void getSession_notFound() {
            when(sessionRepository.findById(100L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> sessionService.getSession(100L));
        }
    }

    // ====== GET SESSIONS BY PERIOD ======
    @Nested
    @DisplayName("getSessionsByPeriod")
    class GetSessionsByPeriodTests {

        @Test
        @DisplayName("should return sessions within date range")
        void getSessionsByPeriod_success() {
            Long roomId = 1L;
            Instant start = Instant.parse("2024-01-01T00:00:00Z");
            Instant end = Instant.parse("2024-01-31T23:59:59Z");
            Room room = testRoom(roomId);
            Account creator = testAccount(1L, "DM", room);
            Session s1 = testSession(100L, creator, Instant.parse("2024-01-15T18:00:00Z"));
            Session s2 = testSession(101L, creator, Instant.parse("2024-01-20T19:00:00Z"));

            when(sessionRepository.findByRoomIdAndTimeBetween(roomId, start, end))
                    .thenReturn(List.of(s1, s2));
            when(sessionMapper.toResponse(eq(s1), anyList())).thenReturn(mock(SessionResponse.class));
            when(sessionMapper.toResponse(eq(s2), anyList())).thenReturn(mock(SessionResponse.class));
            when(participationService.findBySessionId(anyLong())).thenReturn(List.of());

            List<SessionResponse> result = sessionService.getSessionsByPeriod(roomId, start, end);

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("should return empty list when no sessions in range")
        void getSessionsByPeriod_empty() {
            when(sessionRepository.findByRoomIdAndTimeBetween(anyLong(), any(), any()))
                    .thenReturn(List.of());

            List<SessionResponse> result = sessionService.getSessionsByPeriod(1L, Instant.now(), Instant.now().plusSeconds(86400));

            assertTrue(result.isEmpty());
        }
    }

    // ====== GET SESSIONS CURRENT MONTH ======
    @Nested
    @DisplayName("getSessionsCurrentMonth")
    class GetSessionsCurrentMonthTests {

        @Test
        @DisplayName("should return sessions for current month")
        void getSessionsCurrentMonth_success() {
            Long roomId = 1L;
            YearMonth current = YearMonth.now();
            Instant start = current.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant end = current.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

            Room room = testRoom(roomId);
            Account creator = testAccount(1L, "DM", room);
            Session session = testSession(100L, creator, Instant.now());

            when(sessionRepository.findByRoomIdAndCurrentMonth(eq(roomId), eq(start), eq(end)))
                    .thenReturn(List.of(session));
            when(sessionMapper.toResponse(eq(session), anyList())).thenReturn(mock(SessionResponse.class));
            when(participationService.findBySessionId(anyLong())).thenReturn(List.of());

            List<SessionResponse> result = sessionService.getSessionsCurrentMonth(roomId);

            assertEquals(1, result.size());
        }
    }

    // ====== GET UPCOMING SESSIONS ======
    @Nested
    @DisplayName("getUpcomingSessionsWithParticipant")
    class GetUpcomingSessionsTests {

        @Test
        @DisplayName("should return all upcoming sessions for room creator")
        void getUpcomingSessions_creator() {
            Long roomId = 1L;
            Account creator = mock(Account.class);
            Session session = testSession(100L, creator, Instant.now().plusSeconds(86400));

            when(roomSecurityService.isCreator(creator)).thenReturn(true);
            when(sessionRepository.findUpcomingByRoom(eq(roomId), any(), any()))
                    .thenReturn(List.of(session));
            when(sessionMapper.toResponse(eq(session), anyList())).thenReturn(mock(SessionResponse.class));
            when(participationService.findBySessionId(anyLong())).thenReturn(List.of());

            List<SessionResponse> result = sessionService.getUpcomingSessionsWithParticipant(roomId, creator);

            assertEquals(1, result.size());
            verify(sessionRepository).findUpcomingByRoom(eq(roomId), any(), any());
            verify(sessionRepository, never()).findUpcomingWithParticipant(any(), any(), any(), anyLong());
        }

        @Test
        @DisplayName("should return only invited sessions for regular participant")
        void getUpcomingSessions_participant() {
            Long roomId = 1L, accountId = 2L;
            Account participant = testAccount(accountId, "Player", testRoom(roomId));
            Session session = testSession(100L, mock(Account.class), Instant.now().plusSeconds(86400));

            when(roomSecurityService.isCreator(participant)).thenReturn(false);
            when(sessionRepository.findUpcomingWithParticipant(eq(roomId), any(), any(), eq(accountId)))
                    .thenReturn(List.of(session));
            when(sessionMapper.toResponse(eq(session), anyList())).thenReturn(mock(SessionResponse.class));
            when(participationService.findBySessionId(anyLong())).thenReturn(List.of());

            List<SessionResponse> result = sessionService.getUpcomingSessionsWithParticipant(roomId, participant);

            assertEquals(1, result.size());
            verify(sessionRepository).findUpcomingWithParticipant(eq(roomId), any(), any(), eq(accountId));
        }
    }

    // ====== ACKNOWLEDGE SESSION ======
    @Nested
    @DisplayName("acknowledgeSession")
    class AcknowledgeSessionTests {

        @Test
        @DisplayName("should accept invitation successfully")
        void acknowledgeSession_accept() {
            long sessionId = 100L;
            long accountId = 2L;
            Account participant = testAccount(accountId, "Player", testRoom(1L));
            Participation participation = testParticipation(sessionId, accountId, null);

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(mock(Session.class)));
            when(participationService.findBySessionId(sessionId)).thenReturn(List.of(participation));

            sessionService.acknowledgeSession(sessionId, participant, true);

            verify(participationService).acknowledgeParticipation(eq(sessionId), eq(participant), eq(true));
        }

        @Test
        @DisplayName("should throw ForbiddenException if user not invited")
        void acknowledgeSession_notInvited() {
            long sessionId = 100L;
            long accountId = 999L;
            Account stranger = testAccount(accountId, "Stranger", testRoom(1L));
            Participation other = testParticipation(sessionId, 1L, null);

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(mock(Session.class)));
            when(participationService.findBySessionId(sessionId)).thenReturn(List.of(other));

            assertThrows(ForbiddenException.class,
                    () -> sessionService.acknowledgeSession(sessionId, stranger, true));
        }

        @Test
        @DisplayName("should throw NotFoundException if session not found")
        void acknowledgeSession_notFound() {
            when(sessionRepository.findById(100L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> sessionService.acknowledgeSession(100L, mock(Account.class), true));
        }
    }

    // ====== EDGE CASES ======
    @Nested
    @DisplayName("Edge cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("should handle empty accountIds list in createSession")
        void createSession_emptyAccountIds() {
            long roomId = 1L, sessionId = 100L;
            Room room = testRoom(roomId);
            Account creator = testAccount(1L, "DM", room);
            SessionRequest request = mock(SessionRequest.class);
            Session session = mock(Session.class);

            when(roomSecurityService.isCreator(roomId)).thenReturn(true);
            when(request.accountIds()).thenReturn(Set.of());
            when(sessionMapper.toEntity(request, creator)).thenReturn(session);
            when(sessionRepository.save(session)).thenAnswer(inv -> inv.getArgument(0));
            when(session.getId()).thenReturn(sessionId);
            when(sessionMapper.toResponse(eq(session), eq(List.of()))).thenReturn(mock(SessionResponse.class));

            SessionResponse result = sessionService.createSession(roomId, creator, request);

            assertNotNull(result);
            verify(participationService, never()).createParticipations(anyLong(), anyList());
        }

        @Test
        @DisplayName("should handle null time in updateSession")
        void updateSession_nullTime() {
            long sessionId = 100L;
            long roomId = 1L;
            Room room = testRoom(roomId);
            Account creator = testAccount(1L, "DM", room);
            Session session = testSession(sessionId, creator, Instant.now());
            SessionRequest request = mock(SessionRequest.class);

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(roomSecurityService.isCreator(roomId)).thenReturn(true);
            when(request.time()).thenReturn(null);
            when(request.description()).thenReturn("Only description");
            when(sessionRepository.save(session)).thenAnswer(inv -> inv.getArgument(0));
            when(sessionMapper.toResponse(eq(session), anyList())).thenReturn(mock(SessionResponse.class));

            SessionResponse result = sessionService.updateSession(sessionId, request);

            assertNotNull(result);
            verify(session).setDescription("Only description");
        }
    }
}
