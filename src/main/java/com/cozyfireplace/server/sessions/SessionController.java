package com.cozyfireplace.server.sessions;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.current.CurrentAccount;
import com.cozyfireplace.server.sessions.dto.SessionListResponse;
import com.cozyfireplace.server.sessions.dto.SessionRequest;
import com.cozyfireplace.server.sessions.dto.SessionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Tag(name = "Session", description = "API для управления сессиями")
public class SessionController {

    private final SessionService sessionService;

    @Operation(
            summary = "Создать сессию",
            description = "Создаёт сессию в комнате. Только создатель комнаты может создавать сессии.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Создано"),
                    @ApiResponse(responseCode = "403", description = "Нет прав", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Комната или аккаунты не найдены", content = @Content)
            }
    )
    @PostMapping("/{roomId}")
    public ResponseEntity<SessionResponse> create(
            @Parameter(description = "ID комнаты", required = true) @PathVariable Long roomId,
            @Valid @RequestBody SessionRequest request,
            @CurrentAccount Account account
    ) {
        SessionResponse session = sessionService.createSession(roomId, account, request);
        return ResponseEntity.ok(session);
    }

    @Operation(
            summary = "Обновить сессию",
            description = "Обновляет сессию. Только создатель комнаты может обновлять сессии.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Обновлено"),
                    @ApiResponse(responseCode = "403", description = "Нет прав", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Сессия не найдена", content = @Content)
            }
    )
    @PutMapping("/{sessionId}")
    public ResponseEntity<SessionResponse> update(
            @Parameter(description = "ID сессии", required = true) @PathVariable Long sessionId,
            @Valid @RequestBody SessionRequest request
    ) {
        return ResponseEntity.ok(sessionService.updateSession(sessionId, request));
    }

    @Operation(
            summary = "Удалить сессию",
            description = "Удаляет сессию. Только создатель комнаты может удалять сессии.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Удалено"),
                    @ApiResponse(responseCode = "403", description = "Нет прав", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Сессия не найдена", content = @Content)
            }
    )
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID сессии", required = true) @PathVariable Long sessionId
    ) {
        sessionService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Получить сессию по ID",
            description = "Возвращает полную информацию о сессии с участниками.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ок"),
                    @ApiResponse(responseCode = "404", description = "Сессия не найдена", content = @Content)
            }
    )
    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionResponse> getSession(
            @Parameter(description = "ID сессии", required = true) @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(sessionService.getSession(sessionId));
    }

    @Operation(
            summary = "Получить сессии за период",
            description = "Возвращает список сессий комнаты за указанный период.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ок")
            }
    )
    @GetMapping("/{roomId}/range")
    public ResponseEntity<List<SessionResponse>> getSessionsByPeriod(
            @Parameter(description = "ID комнаты", required = true) @PathVariable Long roomId,
            @Parameter(description = "Начало периода", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @Parameter(description = "Конец периода", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end
    ) {
        return ResponseEntity.ok(sessionService.getSessionsByPeriod(roomId, start, end));
    }

    @Operation(
            summary = "Получить сессии текущего месяца",
            description = "Возвращает список сессий комнаты за текущий месяц.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ок")
            }
    )
    @GetMapping("/{roomId}/current-month")
    public ResponseEntity<List<SessionResponse>> getSessionsCurrentMonth(
            @Parameter(description = "ID комнаты", required = true) @PathVariable Long roomId,
            @CurrentAccount Account account
    ) {
        return ResponseEntity.ok(sessionService.getSessionsCurrentMonth(roomId));
    }

    @Operation(
            summary = "Получить будущие сессии с участием аккаунта",
            description = "Возвращает список будущих сессий (до месяца вперёд) где в приглашенных есть автор запроса.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ок")
            }
    )
    @GetMapping("/{roomId}/upcoming")
    public ResponseEntity<List<SessionResponse>> getUpcomingSessions(
            @Parameter(description = "ID комнаты", required = true) @PathVariable Long roomId,
            @CurrentAccount Account account
    ) {
        return ResponseEntity.ok(sessionService.getUpcomingSessionsWithParticipant(roomId, account));
    }

    @Operation(
            summary = "Принять или отклонить приглашение на сессию",
            description = "Принимает (accepted=true) или отклоняет (accepted=false) приглашение на сессию.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Обновлено"),
                    @ApiResponse(responseCode = "404", description = "Сессия не найдена", content = @Content),
                    @ApiResponse(responseCode = "403", description = "Нет прав", content = @Content)
            }
    )
    @PostMapping("/{roomId}/{sessionId}/acknowledge")
    public ResponseEntity<Void> acknowledge(
            @Parameter(description = "ID комнаты", required = true) @PathVariable Long roomId,
            @Parameter(description = "ID сессии", required = true) @PathVariable Long sessionId,
            @RequestParam boolean accepted,
            @CurrentAccount Account account
    ) {
        sessionService.acknowledgeSession(sessionId, account, accepted);
        return ResponseEntity.noContent().build();
    }
}
