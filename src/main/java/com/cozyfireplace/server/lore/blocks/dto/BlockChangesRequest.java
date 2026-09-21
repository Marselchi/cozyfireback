package com.cozyfireplace.server.lore.blocks.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockChangesRequest {
    private List<AddedBlockRequest> addedBlocks;
    private List<UpdatedBlockRequest> updatedBlocks;
    private List<String> deletedBlockIds;
}



