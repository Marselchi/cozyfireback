package com.cozyfireplace.server.lore.search;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.lore.LoreService;
import com.cozyfireplace.server.lore.blocks.Block;
import com.cozyfireplace.server.lore.blocks.BlockRepository;
import com.cozyfireplace.server.rooms.RoomSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cozyfireplace.server.lore.search.LoreDocumentAssembler.AssembledDocument;
import com.cozyfireplace.server.lore.search.LoreDocumentAssembler.Segment;
import com.cozyfireplace.server.lore.search.LoreSnippetBuilder.Chunk;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoreSearchService {


    private final LoreSearchRepository repository;
    private final BlockRepository blockRepository;
    private final LoreService loreService;
    private final LoreSearchProperties properties;
    private final RoomSecurityService roomSecurityService;
    private final LoreDocumentAssembler assembler;
    private final LoreSnippetBuilder snippetBuilder;


    @Transactional(readOnly = true)
    public LoreSearchPageResponse search(LoreSearchRequest request, Account account) {
        int size = request.getSize();
        int offset = request.getOffset();

        boolean canSeeAll = roomSecurityService.isCreator(account);

        Set<Long> roleIdSet = loreService.getAccountRoleIds(account);
        long[] viewerRoleIds = roleIdSet.stream().mapToLong(Long::longValue).toArray();
        boolean viewerHasRoles = viewerRoleIds.length > 0;

        // Персонажа может не быть: тогда ни один chance-блок не виден — в SQL
        // сравнение ar.account_char_id = NULL не даст ни одной строки.
        Long viewerCharId = account.getAccountChar() == null ? null : account.getAccountChar().getId();

        if (request.getQuery() == null || request.getQuery().isBlank()) {
            return LoreSearchPageResponse.builder()
                    .content(List.of())
                    .nextOffset(null)
                    .totalElements(0L)
                    .build();
        }

        List<LoreSearchProjection> rows = repository.search(
                request.getQuery(),
                request.getRoomId(),
                account.getId(),
                viewerCharId,
                canSeeAll,
                viewerHasRoles,
                viewerRoleIds,
                request.getTitle(),
                request.getCreatedByCreator(),
                request.getStatus(),
                size,
                offset
        );

        long totalElements = repository.countSearch(
                request.getQuery(),
                request.getRoomId(),
                account.getId(),
                viewerCharId,
                canSeeAll,
                viewerHasRoles,
                viewerRoleIds,
                request.getTitle(),
                request.getCreatedByCreator(),
                request.getStatus()
        );

        if (rows.isEmpty()) {
            return LoreSearchPageResponse.builder()
                    .content(List.of())
                    .nextOffset(null)
                    .totalElements(totalElements)
                    .build();
        }

        Map<Long, Block> visibleBlocks = loadVisibleBlocks(
                rows, viewerCharId, canSeeAll, viewerHasRoles, viewerRoleIds);

        // 1. Режем каждую запись на сегменты.
        List<List<Segment>> segmentsPerLore = new ArrayList<>(rows.size());
        for (LoreSearchProjection row : rows) {
            segmentsPerLore.add(assembler.split(row.getContent(), visibleBlocks));
        }

        // 2. Подсвечиваем ВСЕ сегменты страницы одним запросом.
        List<List<String>> highlightedPerLore = highlightAll(segmentsPerLore, request.getQuery());

        // 3. Собираем документ, режем на чанки, складываем DTO.
        List<LoreSearchResponse> content = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            content.add(process(rows.get(i), segmentsPerLore.get(i), highlightedPerLore.get(i),
                    request.getQuery()));
        }

        Integer nextOffset = (offset + rows.size() < totalElements) ? offset + rows.size() : null;

        return LoreSearchPageResponse.builder()
                .content(content)
                .nextOffset(nextOffset)
                .totalElements(totalElements)
                .build();
    }

    // -------------------------------------------------------------------------

    private Map<Long, Block> loadVisibleBlocks(List<LoreSearchProjection> rows,
                                               Long viewerCharId,
                                               boolean canSeeAll,
                                               boolean viewerHasRoles,
                                               long[] viewerRoleIds) {
        long[] loreIds = rows.stream().mapToLong(LoreSearchProjection::getId).toArray();

        List<Long> blockIds = repository.findVisibleBlockIds(
                loreIds, viewerCharId, canSeeAll, viewerHasRoles, viewerRoleIds);
        if (blockIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return blockRepository.findAllByIdIn(blockIds).stream()
                .collect(Collectors.toMap(Block::getId, Function.identity()));
    }

    /**
     * Пакетная подсветка. Плоский порядковый номер сегмента служит ключом
     * сопоставления: ord = индекс в сквозном списке сегментов всей страницы.
     */
    private List<List<String>> highlightAll(List<List<Segment>> segmentsPerLore, String query) {
        List<Segment> flat = new ArrayList<>();
        for (List<Segment> segments : segmentsPerLore) {
            flat.addAll(segments);
        }

        List<Long> ords = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        for (int i = 0; i < flat.size(); i++) {
            Segment segment = flat.get(i);
            if (segment.highlightable()) {
                ords.add((long) i);
                texts.add(segment.text());
            }
        }

        Map<Long, String> headlines = new HashMap<>();
        if (!ords.isEmpty()) {
            long[] ordArray = ords.stream().mapToLong(Long::longValue).toArray();
            String[] textArray = texts.toArray(new String[0]);

            for (HighlightProjection projection :
                    repository.highlightBatch(ordArray, textArray, query, LoreSnippetBuilder.HIGHLIGHT_OPTIONS)) {
                if (projection.getHeadline() != null) {
                    headlines.put(projection.getOrd(), projection.getHeadline());
                }
            }
        }

        List<List<String>> result = new ArrayList<>(segmentsPerLore.size());
        int cursor = 0;
        for (List<Segment> segments : segmentsPerLore) {
            List<String> resolved = new ArrayList<>(segments.size());
            for (Segment segment : segments) {
                // ts_headline не вернул строку — отдаём сегмент как есть:
                // подсветки не будет, но текст не потеряется.
                resolved.add(headlines.getOrDefault((long) cursor, segment.text()));
                cursor++;
            }
            result.add(resolved);
        }
        return result;
    }

    private LoreSearchResponse process(LoreSearchProjection projection,
                                       List<Segment> segments,
                                       List<String> highlighted,
                                       String query) {
        AssembledDocument document = assembler.assemble(segments, highlighted);

        List<int[]> occurrences = snippetBuilder.findOccurrences(document.text());

        if (occurrences.isEmpty()) {
            // Инвариант: если строка совпала в SQL, её текст присутствует в
            // собранном документе дословно, значит ts_headline с той же
            // конфигурацией 'russian' и HighlightAll=true обязан что-то
            // подсветить. Сработало — значит что-то рассинхронизировано:
            // скорее всего BLOCK_PLACEHOLDER разъехался с regexp_replace в
            // миграции, либо конфигурацию поменяли в одном месте из двух.
            log.warn("lore id={} matched in SQL but no highlight found in assembled document "
                            + "(query='{}', segments={}) — index/assembly mismatch",
                    projection.getId(), query, segments.size());
            return baseDto(projection)
                    .totalOccurrences(0)
                    .notAll(false)
                    .matches(List.of())
                    .build();
        }

        List<Chunk> chunks = snippetBuilder.buildChunks(document.text(), occurrences, document.regions());

        boolean notAll = chunks.size() > properties.getMaxMatches();
        List<Chunk> limited = notAll ? chunks.subList(0, properties.getMaxMatches()) : chunks;

        List<MatchDto> matchDtos = limited.stream()
                .map(c -> MatchDto.builder()
                        .matchContent(c.text)
                        .occurrenceCount(c.occurrenceCount)
                        .build())
                .collect(Collectors.toList());

        return baseDto(projection)
                .totalOccurrences(occurrences.size())
                .notAll(notAll)
                .matches(matchDtos)
                .build();
    }


    private LoreSearchResponse.LoreSearchResponseBuilder baseDto(LoreSearchProjection projection) {
        return LoreSearchResponse.builder()
                .id(projection.getId())
                .title(projection.getTitle())
                .byAdmin(Boolean.TRUE.equals(projection.getByAdmin()))
                .secret(Boolean.TRUE.equals(projection.getSecret()));
    }

}