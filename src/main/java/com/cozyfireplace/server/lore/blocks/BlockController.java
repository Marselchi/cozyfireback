package com.cozyfireplace.server.lore.blocks;


import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.current.CurrentAccount;
import com.cozyfireplace.server.lore.blocks.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/blocks")
@RequiredArgsConstructor
@Tag(name = "Lore Blocks", description = "Reusable, roll-gated content blocks embedded in lore")
public class BlockController {
    private final BlockService blockService;


    @Operation(
            summary = "List blocks in a room",
            description = "Returns a paginated summary of blocks, optionally filtered by owning lore, full-text search and block type.",
            responses = @ApiResponse(responseCode = "200", description = "Page of blocks returned")
    )
    @GetMapping("/{roomId}/list")
    public ResponseEntity<Page<BlockSummaryResponse>> getBlocks(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(description = "Filter by owning lore ID") @RequestParam(name = "id", required = false) String loreIdParam,
            @Parameter(description = "Full-text search over block content", example = "ancient rune") @RequestParam(required = false) String search,
            @Parameter(description = "Filter by block type", example = "chance") @RequestParam(required = false) String type,
            @Parameter(hidden = true) @PageableDefault Pageable pageable
    ) {
        return ResponseEntity.ok(
                blockService.getBlocksByRoomId(roomId, loreIdParam, search, type, pageable)
        );
    }

    @Operation(
            summary = "Get a block's access record",
            description = "Returns the access/roll metadata for the given block.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Block access returned"),
                    @ApiResponse(responseCode = "404", description = "Block not found", content = @Content)
            }
    )
    @GetMapping("/{roomId}/{blockId}/access")
    @SuppressWarnings("unused")
    public ResponseEntity<BlockAccessResponse<?>> getBlockAccess(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(description = "ID of the block", required = true, example = "99") @PathVariable Long blockId
    ) {
        return ResponseEntity.ok(blockService.getBlockAccess(blockId));
    }

    @Operation(
            summary = "Get a block",
            description = "Returns the full detail of a single block.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Block returned"),
                    @ApiResponse(responseCode = "404", description = "Block not found", content = @Content)
            }
    )
    @GetMapping("/{roomId}/{blockId}")
    @SuppressWarnings("unused")
    public ResponseEntity<BlockDetailResponse> getBlock(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(description = "ID of the block", required = true, example = "99") @PathVariable Long blockId) {
        return ResponseEntity.ok(blockService.getBlock(blockId));
    }

    @Operation(
            summary = "Get a block wrapped with its context",
            description = "Returns the block together with its surrounding lore context.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Wrapped block returned"),
                    @ApiResponse(responseCode = "404", description = "Block not found", content = @Content)
            }
    )
    @GetMapping("/{roomId}/{blockId}/wrapped")
    @SuppressWarnings("unused")
    public ResponseEntity<WrappedBlockResponse> getBlockWrapped(
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId,
            @Parameter(description = "ID of the block", required = true, example = "99") @PathVariable Long blockId) {
        return ResponseEntity.ok(blockService.getBlockWrapped(blockId));
    }

    @Operation(
            summary = "Roll a chance block",
            description = "Performs a skill roll for the given chance block and returns the outcome.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Roll result",
                            content = @Content(examples = @ExampleObject(name = "RollResult", value = """
                                    { "id": 5, "rollValue": 17, "modifier": 3, "threshold": 15, "skill": "perception", "passed": true }
                                    """))),
                    @ApiResponse(responseCode = "403", description = "Access denied for this block", content = @Content)
            }
    )
    @PostMapping("/{roomId}/{blockId}/roll")
    @SuppressWarnings("unused")
    public ResponseEntity<RollResultResponse> roll(
            @Parameter(description = "ID of the block", required = true, example = "99") @PathVariable Long blockId,
            @Parameter(hidden = true) @CurrentAccount Account account,
            @Parameter(description = "ID of the room", required = true, example = "1") @PathVariable Long roomId) {
        return ResponseEntity.ok(blockService.roll(blockId, account));
    }

    @Operation(
            summary = "Reset an access record",
            description = "Resets the roll/access state of an access record back to its initial value.",
            responses = @ApiResponse(responseCode = "200", description = "Access record reset")
    )
    @PostMapping("/access/{recordId}/reset")
    public ResponseEntity<AccessRecordResponse> resetAccess(
            @Parameter(description = "ID of the access record", required = true, example = "31") @PathVariable Long recordId) {
        return ResponseEntity.ok(blockService.resetAccess(recordId));
    }

    @Operation(
            summary = "Grant access",
            description = "Explicitly grants access to a block for the record's user.",
            responses = @ApiResponse(responseCode = "200", description = "Access granted")
    )
    @PostMapping("/access/{recordId}/grant")
    public ResponseEntity<AccessRecordResponse> grantAccess(
            @Parameter(description = "ID of the access record", required = true, example = "31") @PathVariable Long recordId) {
        return ResponseEntity.ok(blockService.grantAccess(recordId));
    }

    @Operation(
            summary = "Deny access",
            description = "Explicitly denies access to a block for the record's user.",
            responses = @ApiResponse(responseCode = "200", description = "Access denied")
    )
    @PostMapping("/access/{recordId}/deny")
    public ResponseEntity<AccessRecordResponse> denyAccess(
            @Parameter(description = "ID of the access record", required = true, example = "31") @PathVariable Long recordId) {
        return ResponseEntity.ok(blockService.denyAccess(recordId));
    }

    @Operation(
            summary = "Update a block's chance configuration",
            description = "Changes the skill and difficulty threshold used when rolling a chance block.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Chance configuration updated",
                            content = @Content(examples = @ExampleObject(name = "ChanceConfig", value = """
                                    { "skill": "perception", "threshold": 15 }
                                    """))),
                    @ApiResponse(responseCode = "404", description = "Block not found", content = @Content)
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(name = "ChanceConfigRequest", value = """
                    { "skill": "perception", "threshold": 15 }
                    """))
    )
    @PatchMapping("/{blockId}/chance")
    public ResponseEntity<ChanceConfigResponse> updateChanceConfig(
            @Parameter(description = "ID of the block", required = true, example = "99") @PathVariable Long blockId,
            @RequestBody ChanceConfigRequest request) {
        return ResponseEntity.ok(blockService.updateChanceConfig(blockId, request));
    }
}
