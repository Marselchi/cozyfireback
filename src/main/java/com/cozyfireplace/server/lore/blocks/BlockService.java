package com.cozyfireplace.server.lore.blocks;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.AccountChar;
import com.cozyfireplace.server.accounts.AccountRepository;
import com.cozyfireplace.server.accounts.stats.AccountCharSkill;
import com.cozyfireplace.server.accounts.stats.Skill;
import com.cozyfireplace.server.accounts.stats.SkillRepository;
import com.cozyfireplace.server.lore.Lore;
import com.cozyfireplace.server.lore.blocks.dto.*;
import com.cozyfireplace.server.roles.Role;
import com.cozyfireplace.server.roles.RoleRepository;
import com.cozyfireplace.server.rooms.RoomSecurityService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlockService {

    private final BlockRepository blockRepository;
    private final AccessRecordRepository accessRecordRepository;
    private final SkillRepository skillRepository;
    private final RoleRepository roleRepository;
    private final BlockMapper blockMapper;
    private final RoomSecurityService roomSecurityService;
    private final AccountRepository accountRepository;

    private final ObjectMapper objectMapper;

    private static final Pattern RESTRICTED_BLOCK_PATTERN =
            Pattern.compile("\\{restrictedBlock\\s+(?:id|localId)=\"([^\"]+)\"\\}");


    /**
     * Обрабатывает изменения блоков: создаёт новые, обновляет существующие, удаляет указанные.
     * Возвращает обновлённый content с заменёнными localId на реальные id.
     */
    @Transactional
    public String processBlockChanges(BlockChangesRequest request, Lore lore) {
        String content = lore.getContent();

        // Удаляем блоки
        if (request.getDeletedBlockIds() != null && !request.getDeletedBlockIds().isEmpty()) {
            List<Long> idsToDelete = request.getDeletedBlockIds().stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            blockRepository.deleteAllById(idsToDelete);
        }

        // Обновляем существующие блоки
        if (request.getUpdatedBlocks() != null) {
            for (UpdatedBlockRequest updatedBlock : request.getUpdatedBlocks()) {
                updateBlock(updatedBlock);
            }
        }

        // Создаём новые блоки и заменяем localId на реальные id
        Map<String, Long> localIdToRealId = new HashMap<>();
        if (request.getAddedBlocks() != null) {
            for (AddedBlockRequest addedBlock : request.getAddedBlocks()) {
                Long realId = createBlock(addedBlock, lore);
                localIdToRealId.put(addedBlock.getLocalId(), realId);
            }
        }

        // Заменяем localId на реальные id в content
        for (Map.Entry<String, Long> entry : localIdToRealId.entrySet()) {
            content = content.replace(
                    "{restrictedBlock localId=\"" + entry.getKey() + "\"}",
                    "{restrictedBlock id=\"" + entry.getValue() + "\"}"
            );
        }

        return content;
    }

    @Transactional(readOnly = true)
    public Page<BlockSummaryResponse> getBlocksByRoomId(
            Long roomId,
            String loreIdParam,
            String search,
            String type,
            Pageable pageable
    ) {
        PageRequest pageRequest = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        Long loreId = parseLoreId(loreIdParam);

        String searchFilter = search == null || search.isBlank()
                ? null
                : search.trim();

        Boolean hasChance = parseBlockType(type);

        Page<Long> ids = blockRepository.findBlockIdsByRoomId(
                roomId,
                loreId,
                searchFilter,
                hasChance,
                pageRequest
        );

        if (!ids.hasContent()) {
            return new PageImpl<>(List.of(), pageRequest, ids.getTotalElements());
        }

        List<Block> blocks = blockRepository.findBlocksForSummaryByIds(ids.getContent());

        Map<Long, Block> blocksById = blocks.stream()
                .collect(Collectors.toMap(Block::getId, Function.identity()));

        List<BlockSummaryResponse> content = ids.getContent().stream()
                .map(blocksById::get)
                .filter(Objects::nonNull)
                .map(blockMapper::toSummaryResponse)
                .toList();

        return new PageImpl<>(content, pageRequest, ids.getTotalElements());
    }

    private Long parseLoreId(String loreIdParam) {
        if (loreIdParam == null || loreIdParam.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(loreIdParam.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("id must be a number");
        }
    }

    private Boolean parseBlockType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }

        String normalized = type.trim().toLowerCase();

        return switch (normalized) {
            case "chance" -> true;
            case "normal" -> false;
            default -> throw new IllegalArgumentException("type must be 'normal' or 'chance'");
        };
    }

    public Set<Long> getAccountRoleIds(Account account) {
        return account.getRoles().stream().map(Role::getId).collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public String expandBlocks(String content, Account account) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        boolean canSeeAll = roomSecurityService.isCreator(account);

        // 1. Собираем все blockId из контента
        List<Long> blockIds = new ArrayList<>();
        Matcher matcher = RESTRICTED_BLOCK_PATTERN.matcher(content);
        while (matcher.find()) {
            String blockIdStr = matcher.group(1);
            blockIds.add(Long.parseLong(blockIdStr));
        }

        if (blockIds.isEmpty()) {
            return content;
        }

        // 2. Загружаем все блоки с ролями одним запросом
        Map<Long, Block> blocksById = blockRepository.findAllWithChanceAndRolesByIds(blockIds).stream()
                .collect(Collectors.toMap(Block::getId, b -> b));

        // 3. Загружаем все AccessRecord для персонажа одним запросом
        Set<Long> userRoleIds = getAccountRoleIds(account);
        Long accountCharId = account.getAccountChar() != null ? account.getAccountChar().getId() : null;

        Map<Long, AccessRecord> accessRecordsByBlockId = new HashMap<>();
        if (accountCharId != null) {
            accessRecordRepository.findAllByBlockIdsAndAccountCharId(blockIds, accountCharId)
                    .forEach(record -> accessRecordsByBlockId.put(record.getBlock().getId(), record));
        }

        // 4. Второй проход - заменяем плейсхолдеры
        StringBuilder result = new StringBuilder();
        matcher = RESTRICTED_BLOCK_PATTERN.matcher(content);

        while (matcher.find()) {
            Long blockId = Long.parseLong(matcher.group(1));
            Block block = blocksById.get(blockId);

            if (block == null) {
                // Блок не найден - удаляем
                matcher.appendReplacement(result, "");
                continue;
            }

            // Проверка ролей
            if (!hasAccessToBlock(block, userRoleIds, canSeeAll)) {
                matcher.appendReplacement(result, "");
                continue;
            }

            // Для chance блоков - проверка AccessRecord
            if (block.hasChance() && !canSeeAll) {
                AccessRecord record = accessRecordsByBlockId.get(blockId);

                if (record != null && record.isRestricted()) {
                    // RESTRICTED - скрываем полностью
                    matcher.appendReplacement(result, "");
                    continue;
                }

                if (record != null && record.isPassed()) {
                    // PASSED - раскрываем полностью
                    String expanded = buildExpandedBlock(block);
                    matcher.appendReplacement(result, Matcher.quoteReplacement(expanded));
                    continue;
                }

                // Нет записи или другие статусы (FAIL) - rollable
                matcher.appendReplacement(result, "{rollable id=\"" + blockId + "\"}");
            } else {
                // Блок НЕ chance или у пользователя canSeeAll — раскрываем полностью
                String expanded = buildExpandedBlock(block);
                matcher.appendReplacement(result, Matcher.quoteReplacement(expanded));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private boolean hasAccessToBlock(Block block, Set<Long> userRoleIds, Boolean canSeeAll) {
        if (canSeeAll) {
            return true;
        }
        Set<Role> blockRoles = block.getRoles();
        if (blockRoles == null || blockRoles.isEmpty()) {
            return true;
        }

        return blockRoles.stream()
                .anyMatch(role -> userRoleIds.contains(role.getId()));
    }

    private Long createBlock(AddedBlockRequest addedBlock, Lore lore) {
        Block block = Block.builder()
                .content(addedBlock.getContent())
                .lore(lore)
                .build();

        Map<String, Object> metadata = addedBlock.getMetadata();

        if (metadata != null) {
            // Устанавливаем роли
            if (metadata.containsKey("roles")) {
                Object rawRoles = metadata.get("roles");
                if (rawRoles instanceof List<?> rawList) {
                    @SuppressWarnings("unchecked")
                    List<String> roleIds = (List<String>) rawList;
                    Set<Role> roles = parseRoles(roleIds);
                    block.setRoles(roles);
                }
            }
            String spoilerType = (String) metadata.get("spoilerType");
            // Устанавливаем ChanceConfig если тип "chance"
            if ("chance".equals(spoilerType)) {
                ChanceConfig chanceConfig = parseChanceConfig(metadata, block);
                block.setChanceConfig(chanceConfig);
            }
        }

        block = blockRepository.save(block);
        return block.getId();
    }

    private void updateBlock(UpdatedBlockRequest updatedBlock) {
        Long blockId = Long.parseLong(updatedBlock.getId());
        Block block = blockRepository.findByIdWithChanceAndRoles(blockId)
                .orElseThrow(() -> new EntityNotFoundException("Block not found: " + blockId));

        block.setContent(updatedBlock.getContent());

        Map<String, Object> metadata = updatedBlock.getMetadata();

        if (metadata != null) {
            // Обновляем роли
            if (metadata.containsKey("roles")) {
                Object rawRoles = metadata.get("roles");
                if (rawRoles instanceof List<?> rawList) {
                    @SuppressWarnings("unchecked")
                    List<String> roleIds = (List<String>) rawList;
                    Set<Role> roles = parseRoles(roleIds);
                    block.getRoles().clear();
                    block.getRoles().addAll(roles);
                }
            }

            // Обновляем ChanceConfig
            // Исправлено: проверяем spoilerType, а не type блока
            String spoilerType = (String) metadata.get("spoilerType");
            if ("chance".equals(spoilerType)) {
                ChanceConfig chanceConfig = block.getChanceConfig();
                if (chanceConfig == null) {
                    chanceConfig = ChanceConfig.builder()
                            .block(block)
                            .build();
                    block.setChanceConfig(chanceConfig);
                }
                updateChanceConfigFromMetadata(chanceConfig, metadata);
            } else {
                // Если тип сменился на другой, удаляем ChanceConfig
                if (block.getChanceConfig() != null) {
                    block.setChanceConfig(null);
                }
            }
        } else {
            // Если метаданных вообще нет, но блок мог быть chance-типом — удаляем config
            if (block.getChanceConfig() != null) {
                block.setChanceConfig(null);
            }
        }

        blockRepository.save(block);
    }

    private Set<Role> parseRoles(List<String> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new HashSet<>();
        }

        List<Long> ids = roleIds.stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());
        return new HashSet<>(roleRepository.findAllById(ids));
    }

    private ChanceConfig parseChanceConfig(Map<String, Object> metadata, Block block) {
        ChanceConfig config = ChanceConfig.builder()
                .block(block)
                .build();
        updateChanceConfigFromMetadata(config, metadata);
        return config;
    }

    private void updateChanceConfigFromMetadata(ChanceConfig config, Map<String, Object> metadata) {
        if (metadata.containsKey("chance")) {
            Object rawChance = metadata.get("chance");
            if (rawChance instanceof Map<?, ?> rawChanceMap) {
                if (rawChanceMap.containsKey("skill")) {
                    String skillId = String.valueOf(rawChanceMap.get("skill"));
                    Skill skill = skillRepository.findById(skillId)
                            .orElseThrow(() -> new EntityNotFoundException("Skill not found: " + skillId));
                    config.setSkill(skill);
                }

                if (rawChanceMap.containsKey("threshold")) {
                    Object rawThreshold = rawChanceMap.get("threshold");
                    int threshold = 0;
                    if (rawThreshold instanceof Number num) {
                        threshold = num.intValue();
                    } else if (rawThreshold != null) {
                        threshold = Integer.parseInt(rawThreshold.toString());
                    }
                    config.setThreshold(threshold);
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public BlockAccessResponse<?> getBlockAccess(Long blockId) {
        BlockAccessMeta meta = blockRepository.findAccessMetaByBlockId(blockId)
                .orElseThrow(() -> new EntityNotFoundException("Block not found"));

        Long roomId = meta.getRoomId();

        if (roomId == null) {
            throw new EntityNotFoundException("Room for block not found");
        }

        if (meta.getHasChance()) {
            List<AccessRecordResponse> records =
                    accessRecordRepository.findChanceAccess(roomId, blockId);

            return new BlockAccessResponse<>(AccessType.CHANCE, records);
        }

        List<NormalAccessRecordResponse> records =
                accountRepository.findNormalAccess(roomId, blockId);

        return new BlockAccessResponse<>(AccessType.NORMAL, records);
    }

    private String buildExpandedBlock(Block block) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("id", block.getId().toString());
        metadata.put("spoilerType", block.hasChance() ? "chance" : "normal");

        // Роли как массив строк-айдишников
        List<String> roleIds = block.getRoles().stream()
                .map(role -> String.valueOf(role.getId()))
                .collect(Collectors.toList());
        metadata.put("roles", roleIds);

        // Chance конфиг
        if (block.hasChance()) {
            Map<String, Object> chance = new LinkedHashMap<>();
            chance.put("skill", block.getChanceConfig().getSkill().getKey());
            chance.put("threshold", block.getChanceConfig().getThreshold());
            metadata.put("chance", chance);
        }

        String metadataJson;
        try {
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize block metadata", e);
        }

        return "::: restrictedBlock " + metadataJson + "\n" + block.getContent() + "\n:::";
    }


    @Transactional(readOnly = true)
    public BlockDetailResponse getBlock(Long blockId) {
        Block block = blockRepository.findByIdWithChanceAndRoles(blockId)
                .orElseThrow(() -> new EntityNotFoundException("Block not found"));
        return blockMapper.toDetailResponse(block, block.getLore());
    }

    @Transactional(readOnly = true)
    public WrappedBlockResponse getBlockWrapped(Long blockId) {
        Block block = blockRepository.findByIdWithChanceAndRoles(blockId)
                .orElseThrow(() -> new EntityNotFoundException("Block not found"));
        return new WrappedBlockResponse(buildExpandedBlock(block));
    }

    @Transactional
    public RollResultResponse roll(Long blockId, Account account) {
        Block block = blockRepository.findByIdWithChanceAndRoles(blockId)
                .orElseThrow(() -> new EntityNotFoundException("Block not found"));

        if (!block.hasChance()) {
            throw new IllegalStateException("Block does not have chance config");
        }

        AccountChar accountChar = account.getAccountChar();
        if (accountChar == null) {
            throw new IllegalStateException("Account does not have a character");
        }

        AccessRecord existingRecord = accessRecordRepository
                .findByBlockIdAndAccountCharId(blockId, accountChar.getId())
                .orElse(null);

        if (existingRecord != null && existingRecord.getStatus() != AccessStatus.PENDING) {
            throw new IllegalStateException("Roll already performed. Use reset first.");
        }

        ChanceConfig config = block.getChanceConfig();
        int roll = new Random().nextInt(20) + 1;

        int skillModifier = getSkillModifier(accountChar, config.getSkill().getKey());

        AccessStatus status = roll + skillModifier >= config.getThreshold()
                ? AccessStatus.PASSED
                : AccessStatus.FAIL;

        if (existingRecord == null) {
            existingRecord = AccessRecord.builder()
                    .block(block)
                    .accountChar(accountChar)
                    .build();
        }

        existingRecord.setRollValue(roll);
        existingRecord.setStatus(status);
        existingRecord.setModifier(skillModifier);

        existingRecord = accessRecordRepository.save(existingRecord);
        return blockMapper.toRollResultResponse(existingRecord, config.getThreshold(), config.getSkill().getLabel());
    }

    @Transactional
    public AccessRecordResponse resetAccess(Long recordId) {
        AccessRecordWithAccount res = accessRecordRepository.findByIdWithAccount(recordId)
                .orElseThrow(() -> new EntityNotFoundException("Access record not found"));

        AccessRecord record = res.getRecord();
        record.setStatus(AccessStatus.PENDING);
        record.setRollValue(null);

        record = accessRecordRepository.save(record);
        return blockMapper.toAccessRecordResponse(record, res.getUserId(), res.getUserName());
    }

    @Transactional
    public AccessRecordResponse grantAccess(Long recordId) {
        AccessRecordWithAccount res = accessRecordRepository.findByIdWithAccount(recordId)
                .orElseThrow(() -> new EntityNotFoundException("Access record not found"));
        AccessRecord record = res.getRecord();
        record.setStatus(AccessStatus.GRANTED);

        record = accessRecordRepository.save(record);
        return blockMapper.toAccessRecordResponse(record, res.getUserId(), res.getUserName());
    }

    @Transactional
    public AccessRecordResponse denyAccess(Long recordId) {
        AccessRecordWithAccount res = accessRecordRepository.findByIdWithAccount(recordId)
                .orElseThrow(() -> new EntityNotFoundException("Access record not found"));

        AccessRecord record = res.getRecord();
        record.setStatus(AccessStatus.DENIED);

        record = accessRecordRepository.save(record);
        return blockMapper.toAccessRecordResponse(record, res.getUserId(), res.getUserName());
    }

    @Transactional
    public ChanceConfigResponse updateChanceConfig(Long blockId, ChanceConfigRequest request) {
        Block block = blockRepository.findByIdWithChanceAndRoles(blockId)
                .orElseThrow(() -> new EntityNotFoundException("Block not found"));

        Skill skill = skillRepository.findById(request.getSkill())
                .orElseThrow(() -> new EntityNotFoundException("Skill not found: " + request.getSkill()));

        ChanceConfig config = block.getChanceConfig();
        if (config == null) {
            config = ChanceConfig.builder()
                    .block(block)
                    .build();
            block.setChanceConfig(config);
        }

        config.setSkill(skill);
        config.setThreshold(request.getThreshold());

        block = blockRepository.save(block);
        return blockMapper.toChanceConfigResponse(block.getChanceConfig());
    }

    private int getSkillModifier(AccountChar accountChar, String skillKey) {
        return accountChar.getSkills().stream()
                .filter(s -> s.getSkill().getKey().equals(skillKey))
                .map(AccountCharSkill::getModifier)
                .findFirst()
                .orElse(0);
    }
}