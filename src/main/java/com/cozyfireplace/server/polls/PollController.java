package com.cozyfireplace.server.polls;


import com.cozyfireplace.server.polls.dto.PollCreateRequest;
import com.cozyfireplace.server.polls.dto.PollResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/polls")
@RequiredArgsConstructor
@Tag(name = "Polls", description = "Submit and read polls / feedback")
public class PollController {

    private final PollService pollService;

    @Operation(
            summary = "Create a poll entry",
            description = "Submits poll/feedback content for a given source.",
            responses = @ApiResponse(responseCode = "204", description = "Submitted")
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(name = "PollCreate", value = """
                    { "source": "landing", "content": "Love the new lore editor!" }
                    """))
    )
    @PostMapping("")
    public ResponseEntity<Void> create(
            @Valid @RequestBody PollCreateRequest request
    ) {
        pollService.createPoll(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get polls by source",
            description = "Returns all poll/feedback entries recorded for the given source.",
            responses = @ApiResponse(responseCode = "200", description = "List of polls returned")
    )
    @GetMapping("/source")
    public ResponseEntity<List<PollResponse>> getPolls(
            @Parameter(description = "Source identifier to filter by", required = true, example = "landing")
            @RequestParam String source
    ) {
        return ResponseEntity.ok(pollService.getPolls(source));
    }
}
