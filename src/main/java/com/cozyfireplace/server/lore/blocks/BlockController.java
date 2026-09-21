package com.cozyfireplace.server.lore.blocks;


import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.current.CurrentAccount;
import com.cozyfireplace.server.lore.blocks.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/blocks")
@RequiredArgsConstructor
public class BlockController {
    private final BlockService blockService;


    @GetMapping("/{roomId}/list")
    public ResponseEntity<Page<BlockSummaryResponse>> getBlocks(
            @PathVariable Long roomId,
            @RequestParam(name = "id", required = false) String loreIdParam,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(
                blockService.getBlocksByRoomId(roomId, loreIdParam, search, type, pageable)
        );
    }

    @GetMapping("/{roomId}/{blockId}/access")
    public ResponseEntity<BlockAccessResponse<?>> getBlockAccess(
            @PathVariable Long roomId,
            @PathVariable Long blockId
    ) {
        return ResponseEntity.ok(blockService.getBlockAccess(blockId));
    }

    @GetMapping("/{roomId}/{blockId}")
    public ResponseEntity<BlockDetailResponse> getBlock(@PathVariable Long roomId, @PathVariable Long blockId) {
        return ResponseEntity.ok(blockService.getBlock(blockId));
    }

    @GetMapping("/{roomId}/{blockId}/wrapped")
    public ResponseEntity<WrappedBlockResponse> getBlockWrapped(@PathVariable Long roomId, @PathVariable Long blockId) {
        return ResponseEntity.ok(blockService.getBlockWrapped(blockId));
    }

    @PostMapping("/{roomId}/{blockId}/roll")
    public ResponseEntity<RollResultResponse> roll(
            @PathVariable Long blockId,
            @CurrentAccount Account account, @PathVariable Long roomId) {
        return ResponseEntity.ok(blockService.roll(blockId, account));
    }

    @PostMapping("/access/{recordId}/reset")
    public ResponseEntity<AccessRecordResponse> resetAccess(@PathVariable Long recordId) {
        return ResponseEntity.ok(blockService.resetAccess(recordId));
    }

    @PostMapping("/access/{recordId}/grant")
    public ResponseEntity<AccessRecordResponse> grantAccess(@PathVariable Long recordId) {
        return ResponseEntity.ok(blockService.grantAccess(recordId));
    }

    @PostMapping("/access/{recordId}/deny")
    public ResponseEntity<AccessRecordResponse> denyAccess(@PathVariable Long recordId) {
        return ResponseEntity.ok(blockService.denyAccess(recordId));
    }

    @PatchMapping("/{blockId}/chance")
    public ResponseEntity<ChanceConfigResponse> updateChanceConfig(
            @PathVariable Long blockId,
            @RequestBody ChanceConfigRequest request) {
        return ResponseEntity.ok(blockService.updateChanceConfig(blockId, request));
    }
}
