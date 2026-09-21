package com.cozyfireplace.server.lore.blocks.dto;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.AccountChar;
import com.cozyfireplace.server.lore.Lore;
import com.cozyfireplace.server.lore.blocks.AccessRecord;
import com.cozyfireplace.server.lore.blocks.Block;
import com.cozyfireplace.server.lore.blocks.ChanceConfig;
import com.cozyfireplace.server.lore.dto.IdName;
import com.cozyfireplace.server.roles.Role;
import org.mapstruct.*;

import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface BlockMapper {

    default BlockSummaryResponse toSummaryResponse(Block block) {
        if (block == null) {
            return null;
        }

        Lore lore = block.getLore();

        return BlockSummaryResponse.builder()
                .id(block.getId())
                .loreId(lore != null ? lore.getId() : null)
                .title(lore != null ? lore.getTitle() : null)
                .contentPreview(getContentPreview(block.getContent())) // Исправлено здесь
                .type(block.hasChance() ? "chance" : "normal")
                .hasChance(block.hasChance())
                .roles(block.getRoles().stream()
                        .map(this::toRoleDto)
                        .collect(Collectors.toList()))
                .build();
    }

    // Безопасная обрезка строки
    default String getContentPreview(String content) {
        if (content == null) {
            return null;
        }
        int maxLength = 100;
        return content.length() > maxLength ? content.substring(0, maxLength) : content;
    }

    RollResultResponse toRollResultResponse(AccessRecord accessRecord, Integer threshold, String skill);

    default BlockDetailResponse toDetailResponse(Block block, Lore lore) {
        if (block == null) return null;
        return BlockDetailResponse.builder()
                .id(block.getId())
                .loreId(lore.getId())
                .title(lore.getTitle())
                .content(block.getContent())
                .type(block.hasChance() ? "chance" : "normal")
                .roles(block.getRoles().stream().map(this::toRoleDto).toList())
                .chance(block.hasChance() ? toChanceConfigResponse(block.getChanceConfig()) : null)
                .build();
    }

    default ChanceConfigResponse toChanceConfigResponse(ChanceConfig config) {
        if (config == null) return null;
        return ChanceConfigResponse.builder()
                .skill(config.getSkill().getKey())
                .threshold(config.getThreshold())
                .build();
    }

    default String toRoleDto(Role role) {
        if (role == null) return null;
        return
                role.getName();
    }

    default AccessRecordResponse toAccessRecordResponse(AccessRecord record, Long userId, String userName) {
        if (record == null) return null;
        return AccessRecordResponse.builder()
                .id(record.getId())
                .blockId(record.getBlock().getId())
                .userId(userId)
                .userName(userName)
                .status(record.getStatus().name().toLowerCase())
                .rollValue(record.getRollValue())
                .passed(record.isPassed())
                .build();
    }
}