package com.cozyfireplace.server.lore.blocks.dto;

import com.cozyfireplace.server.lore.blocks.AccessType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class BlockAccessResponse<T> {
    private AccessType accessType;
    private List<T> records;
}
