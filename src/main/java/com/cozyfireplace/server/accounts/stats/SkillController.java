package com.cozyfireplace.server.accounts.stats;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/skills")
@RequiredArgsConstructor
@Tag(name = "Skills", description = "Skill catalog for a room's game system")
public class SkillController {

    private final SkillService skillService;

    @Operation(
            summary = "Get skills for a room",
            description = "Returns the list of skills available in the specified room.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of skills returned"),
                    @ApiResponse(responseCode = "404", description = "Room not found", content = @Content)
            }
    )
    @GetMapping("/{roomId}")
    public ResponseEntity<List<SkillListResponse>> getRoomSkills(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId) {
        return ResponseEntity.ok(skillService.getSkills(roomId));
    }

}
