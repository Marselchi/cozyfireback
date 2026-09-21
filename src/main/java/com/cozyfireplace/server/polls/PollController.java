package com.cozyfireplace.server.polls;


import com.cozyfireplace.server.polls.dto.PollCreateRequest;
import com.cozyfireplace.server.polls.dto.PollResponse;
import com.cozyfireplace.server.tags.TagService;
import com.cozyfireplace.server.tags.dto.TagCreateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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
@Tag(name = "Polls", description = "Опросики и отзывы")
public class PollController {

    private final PollService pollService;

    @Operation()
    @PostMapping("")
    public ResponseEntity<Void> create(
            @Valid @RequestBody PollCreateRequest request
    ) {
        pollService.createPoll(request);
        return ResponseEntity.noContent().build();
    }

    @Operation()
    @GetMapping("/source")
    public ResponseEntity<List<PollResponse>> getPolls(
            @RequestParam String source
    ) {
        return ResponseEntity.ok(pollService.getPolls(source));
    }
}
