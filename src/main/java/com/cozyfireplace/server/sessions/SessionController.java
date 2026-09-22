package com.cozyfireplace.server.sessions;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.current.CurrentAccount;
import com.cozyfireplace.server.sessions.dto.SessionRequest;
import com.cozyfireplace.server.sessions.dto.SessionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
@Tag(name = "Sessions", description = "Scheduled game sessions and participant invitations")
public class SessionController {

    private final SessionService sessionService;

    @Operation(
            summary = "Create a session",
            description = "Creates a session in the room. Only the room creator may create sessions.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Created"),
                    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Room or accounts not found", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(name = "SessionCreate", value = """
                    {
                      "time": "2026-10-05T18:00:00Z",
                      "description": "Session 12 - the gate assault",
                      "accountIds": [42, 55, 61]
                    }
                    """))
    )
    @PostMapping("/{roomId}")
    public ResponseEntity<SessionResponse> create(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Valid @RequestBody SessionRequest request,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        SessionResponse session = sessionService.createSession(roomId, account, request);
        return ResponseEntity.ok(session);
    }

    @Operation(
            summary = "Update a session",
            description = "Updates a session. Only the room creator may update sessions.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Updated"),
                    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Session not found", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(name = "SessionUpdate", value = """
                    {
                      "time": "2026-10-06T18:00:00Z",
                      "description": "Moved by one day",
                      "accountIds": [42, 55]
                    }
                    """))
    )
    @PutMapping("/{sessionId}")
    public ResponseEntity<SessionResponse> update(
            @Parameter(description = "ID of the session", required = true, example = "3") @PathVariable Long sessionId,
            @Valid @RequestBody SessionRequest request
    ) {
        return ResponseEntity.ok(sessionService.updateSession(sessionId, request));
    }

    @Operation(
            summary = "Delete a session",
            description = "Deletes a session. Only the room creator may delete sessions.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Deleted"),
                    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Session not found", content = @Content)
            }
    )
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID of the session", required = true, example = "3") @PathVariable Long sessionId
    ) {
        sessionService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get a session by ID",
            description = "Returns the full session details including participants.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Session not found", content = @Content)
            }
    )
    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionResponse> getSession(
            @Parameter(description = "ID of the session", required = true, example = "3") @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(sessionService.getSession(sessionId));
    }

    @Operation(
            summary = "Get sessions within a period",
            description = "Returns the room's sessions falling within the given time range.",
            responses = @ApiResponse(responseCode = "200", description = "OK")
    )
    @GetMapping("/{roomId}/range")
    public ResponseEntity<List<SessionResponse>> getSessionsByPeriod(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(description = "Period start (ISO-8601)", required = true, example = "2026-09-01T00:00:00Z") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @Parameter(description = "Period end (ISO-8601)", required = true, example = "2026-09-30T23:59:59Z") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end
    ) {
        return ResponseEntity.ok(sessionService.getSessionsByPeriod(roomId, start, end));
    }

    @Operation(
            summary = "Get this month's sessions",
            description = "Returns the room's sessions for the current month.",
            responses = @ApiResponse(responseCode = "200", description = "OK")
    )
    @GetMapping("/{roomId}/current-month")
    @SuppressWarnings("unused")
    public ResponseEntity<List<SessionResponse>> getSessionsCurrentMonth(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        return ResponseEntity.ok(sessionService.getSessionsCurrentMonth(roomId));
    }

    @Operation(
            summary = "Get upcoming sessions the account participates in",
            description = "Returns upcoming sessions (up to a month ahead) where the requesting account is invited.",
            responses = @ApiResponse(responseCode = "200", description = "OK")
    )
    @GetMapping("/{roomId}/upcoming")
    public ResponseEntity<List<SessionResponse>> getUpcomingSessions(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        return ResponseEntity.ok(sessionService.getUpcomingSessionsWithParticipant(roomId, account));
    }

    @Operation(
            summary = "Accept or decline a session invitation",
            description = "Accepts (accepted=true) or declines (accepted=false) an invitation to a session.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Updated"),
                    @ApiResponse(responseCode = "404", description = "Session not found", content = @Content),
                    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
            }
    )
    @PostMapping("/{roomId}/{sessionId}/acknowledge")
    @SuppressWarnings("unused")
    public ResponseEntity<Void> acknowledge(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(description = "ID of the session", required = true, example = "3") @PathVariable Long sessionId,
            @Parameter(description = "Whether the invitation is accepted", required = true, example = "true") @RequestParam boolean accepted,
            @Parameter(hidden = true) @CurrentAccount Account account
    ) {
        sessionService.acknowledgeSession(sessionId, account, accepted);
        return ResponseEntity.noContent().build();
    }
}
