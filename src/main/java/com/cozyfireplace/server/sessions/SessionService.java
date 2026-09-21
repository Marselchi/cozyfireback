package com.cozyfireplace.server.sessions;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.AccountRepository;
import com.cozyfireplace.server.rooms.RoomSecurityService;
import com.cozyfireplace.server.sessions.dto.*;
import com.cozyfireplace.server.sessions.participations.Participation;
import com.cozyfireplace.server.sessions.participations.ParticipationService;
import com.cozyfireplace.server.util.exception.ForbiddenException;
import com.cozyfireplace.server.util.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final SessionMapper sessionMapper;
    private final ParticipationService participationService;
    private final AccountRepository accountRepository;
    private final RoomSecurityService roomSecurityService;

    /**
     * Создать сессию
     */
    @Transactional
    public SessionResponse createSession(Long roomId, Account account, SessionRequest request) {
        // Проверка: только создатель комнаты может создавать сессии
        if (!roomSecurityService.isCreator(roomId)) {
            throw new ForbiddenException("Создание сессии", "только создатель комнаты может создавать сессии");
        }

        // Валидация: аккаунты должны быть из той же комнаты
        if (request.accountIds() != null && !request.accountIds().isEmpty()) {
            List<Account> invitedAccounts = accountRepository.findAllById(request.accountIds());
            for (Account invitedAccount : invitedAccounts) {
                if (!invitedAccount.getRoom().getId().equals(roomId)) {
                    throw new NotFoundException("Аккаунт", "id", invitedAccount.getId());
                }
            }
        }

        Session session = sessionMapper.toEntity(request, account);
        Session savedSession = sessionRepository.save(session);

        // Создание записей участия для приглашенных аккаунтов
        if (request.accountIds() != null && !request.accountIds().isEmpty()) {
            participationService.createParticipations(savedSession.getId(), request.accountIds().stream().toList());
        }

        return toSessionResponse(savedSession);
    }

    /**
     * Обновить сессию
     */
    @Transactional
    public SessionResponse updateSession(Long sessionId, SessionRequest request) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Сессия", "id", sessionId));

        // Проверка: только создатель комнаты может обновлять сессии
        if (!roomSecurityService.isCreator(session.getCreator().getRoom().getId())) {
            throw new ForbiddenException("Обновление сессии", "только создатель комнаты может обновлять сессии");
        }

        session.setTime(request.time());
        session.setDescription(request.description());

        /*
        // При обновлении времени сессии сбрасываем у всех участников статус на null
        if (request.time() != null) {
            participationService.resetParticipationStatuses(sessionId);
        }
        */

        Session updatedSession = sessionRepository.save(session);
        return toSessionResponse(updatedSession);
    }

    /**
     * Удалить сессию
     */
    @Transactional
    public void deleteSession(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Сессия", "id", sessionId));

        // Проверка: только создатель комнаты может удалять сессии
        if (!roomSecurityService.isCreator(session.getCreator().getRoom().getId())) {
            throw new ForbiddenException("Удаление сессии", "только создатель комнаты может удалять сессии");
        }

        sessionRepository.delete(session);
    }

    /**
     * Получить сессию по ID с участниками
     */
    @Transactional(readOnly = true)
    public SessionResponse getSession(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Сессия", "id", sessionId));

        List<IdNameBool> participants = getParticipantsWithStatus(sessionId);

        return sessionMapper.toResponse(session, participants);
    }

    /**
     * Получить сессии комнаты за период
     */
    @Transactional(readOnly = true)
    public List<SessionResponse> getSessionsByPeriod(Long roomId, Instant start, Instant end) {
        List<Session> sessions = sessionRepository.findByRoomIdAndTimeBetween(roomId, start, end);
        return sessions.stream()
                .map(this::toSessionResponse)
                .toList();
    }

    /**
     * Получить сессии комнаты текущего месяца
     */
    @Transactional(readOnly = true)
    public List<SessionResponse> getSessionsCurrentMonth(Long roomId) {
        YearMonth currentMonth = YearMonth.now();
        Instant startOfMonth = currentMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant startOfNextMonth = currentMonth.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<Session> sessions = sessionRepository.findByRoomIdAndCurrentMonth(roomId, startOfMonth, startOfNextMonth);
        return sessions.stream()
                .map(this::toSessionResponse)
                .toList();
    }

    /**
     * Получить будущие сессии (до месяца вперёд) где в приглашенных есть автор запроса.
     * Если пользователь является создателем комнаты, возвращаются все сессии комнаты.
     */
    @Transactional(readOnly = true)
    public List<SessionResponse> getUpcomingSessionsWithParticipant(Long roomId, Account account) {
        Instant now = Instant.now();
        Instant oneMonthLater = now.plusSeconds(30L * 24 * 60 * 60); // 30 дней

        // Если пользователь - создатель комнаты, возвращаем все сессии
        if (roomSecurityService.isCreator(account)) {
            List<Session> sessions = sessionRepository.findUpcomingByRoom(roomId, now, oneMonthLater);
            return sessions.stream()
                    .map(this::toSessionResponse)
                    .toList();
        }

        // Иначе только сессии, где пользователь приглашен
        List<Session> sessions = sessionRepository.findUpcomingWithParticipant(roomId, now, oneMonthLater, account.getId());
        return sessions.stream()
                .map(this::toSessionResponse)
                .toList();
    }

    /**
     * Принять или отклонить приглашение
     */
    @Transactional
    public void acknowledgeSession(Long sessionId, Account account, boolean accepted) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Сессия", "id", sessionId));

        // Проверка: только приглашенный может принять/отклонить приглашение
        Participation participation = participationService.findBySessionId(sessionId).stream()
                .filter(p -> p.getId().getAccountId().equals(account.getId()))
                .findFirst()
                .orElseThrow(() -> new ForbiddenException("Вы не приглашены на эту сессию"));

        participationService.acknowledgeParticipation(sessionId, account, accepted);
    }

    /**
     * Получить список участников сессии с их статусами
     */
    private List<IdNameBool> getParticipantsWithStatus(Long sessionId) {
        return participationService.findBySessionId(sessionId).stream()
                .map(participation -> {
                    Account account = accountRepository.findById(participation.getId().getAccountId())
                            .orElseThrow(() -> new NotFoundException("Аккаунт", "id", participation.getId().getAccountId()));
                    return new IdNameBool(account.getId(), account.getName(), participation.getAccepted());
                })
                .toList();
    }

    /**
     * Преобразовать Session в SessionResponse с участниками
     */
    private SessionResponse toSessionResponse(Session session) {
        List<IdNameBool> participants = getParticipantsWithStatus(session.getId());
        return sessionMapper.toResponse(session, participants);
    }
}
