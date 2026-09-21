package com.cozyfireplace.server.lore.search;

import com.cozyfireplace.server.lore.blocks.Block;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Разбирает lore.content на сегменты, подставляет тела видимых блоков и
 * собирает единый документ для чанкования.
 * <p>
 * Почему сборка, а не «чанковать каждый блок отдельно»: если гонять чанкер
 * отдельно по публичному тексту и отдельно по каждому блоку, совпадение рядом с
 * маркером теряет половину контекста, чанки одной записи приходят в двух
 * несвязанных порядках, и из публичных кусков всё равно надо вырезать маркеры —
 * включая обрезанные краем окна ("{restrictedBl"). Сборка даёт один связный
 * документ ровно в том порядке, в каком его читает пользователь.
 * <p>
 * Подсветка при этом делается ДО сборки, посегментно (см.
 * LoreSearchRepository#highlightBatch) — заголовок блока с JSON-метаданными
 * никогда не попадает в ts_headline.
 */
@Component
@RequiredArgsConstructor
public class LoreDocumentAssembler {

    /**
     * Чем заменяется маркер невидимого блока. ДОЛЖНО совпадать со строкой в
     * генерируемой колонке lore.content_tsv (changeSet 1777822239163-101),
     * иначе БД и приложение разойдутся в том, где кончается одна лексема и
     * начинается другая, и появятся совпадения, которые нечем подсветить.
     */
    public static final String BLOCK_PLACEHOLDER = " xrbxboundaryxrbx ";

    /**
     * Тот же шаблон, что в regexp_replace миграции.
     */
    private static final Pattern MARKER = Pattern.compile("\\{restrictedBlock[^}]*}");

    private static final Pattern MARKER_ID = Pattern.compile("id\\s*=\\s*\"(\\d+)\"");

    private final ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // Модель
    // -------------------------------------------------------------------------

    public sealed interface Segment permits PublicText, BlockText, Placeholder {

        String text();

        /**
         * Плейсхолдеры не содержат авторского текста — подсвечивать нечего.
         */
        default boolean highlightable() {
            return true;
        }
    }

    public record PublicText(String text) implements Segment {
    }

    public record BlockText(Block block, String text) implements Segment {
    }

    public record Placeholder(String text) implements Segment {
        @Override
        public boolean highlightable() {
            return false;
        }
    }

    /**
     * Границы блока в собранном документе.
     *
     * @param start     индекс первого символа заголовка "::: restrictedBlock {...}"
     * @param bodyStart индекс первого символа тела (сразу после '\n' заголовка)
     * @param end       индекс за последним символом закрывающего ":::"
     */
    public record BlockRegion(long blockId, int start, int bodyStart, int end, String header) {
    }

    public record AssembledDocument(String text, List<BlockRegion> regions) {
    }

    // -------------------------------------------------------------------------
    // Разбор
    // -------------------------------------------------------------------------

    /**
     * Режет сырой content на сегменты. Маркер, чей блок есть в visibleBlocks,
     * становится BlockText; маркер невидимого блока — Placeholder с тем же
     * текстом, что использует индекс.
     */
    public List<Segment> split(String content, Map<Long, Block> visibleBlocks) {
        List<Segment> segments = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return segments;
        }

        Matcher m = MARKER.matcher(content);
        int pos = 0;
        while (m.find()) {
            if (m.start() > pos) {
                segments.add(new PublicText(content.substring(pos, m.start())));
            }

            Long blockId = extractId(m.group());
            Block block = blockId == null ? null : visibleBlocks.get(blockId);

            if (block == null) {
                segments.add(new Placeholder(BLOCK_PLACEHOLDER));
            } else {
                segments.add(new BlockText(block, block.getContent() == null ? "" : block.getContent()));
            }
            pos = m.end();
        }
        if (pos < content.length()) {
            segments.add(new PublicText(content.substring(pos)));
        }
        return segments;
    }

    private Long extractId(String marker) {
        Matcher m = MARKER_ID.matcher(marker);
        if (!m.find()) {
            return null;
        }
        try {
            return Long.parseLong(m.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Сборка
    // -------------------------------------------------------------------------

    /**
     * Склеивает подсвеченные сегменты в один документ и попутно записывает
     * границы блоков.
     * <p>
     * Границы считаются по длинам во время сборки, а не поиском ":::" в готовой
     * строке: ts_headline может слегка переписать пробелы внутри сегмента, и
     * восстановленные постфактум индексы «поедут».
     *
     * @param highlighted подсвеченный текст каждого сегмента, параллельно segments
     */
    public AssembledDocument assemble(List<Segment> segments, List<String> highlighted) {
        StringBuilder sb = new StringBuilder();
        List<BlockRegion> regions = new ArrayList<>();

        for (int i = 0; i < segments.size(); i++) {
            Segment segment = segments.get(i);
            String text = highlighted.get(i);

            if (!(segment instanceof BlockText bt)) {
                sb.append(text);
                continue;
            }

            String header = buildBlockHeader(bt.block());
            int start = sb.length();
            sb.append(header).append('\n');
            int bodyStart = sb.length();
            sb.append(text).append("\n:::");

            regions.add(new BlockRegion(bt.block().getId(), start, bodyStart, sb.length(), header));
        }

        return new AssembledDocument(sb.toString(), regions);
    }

    /**
     * Первая строка развёрнутого блока: "::: restrictedBlock {json}".
     * <p>
     * Это ровно то, что раньше делал buildExpandedBlock(), но без тела — тело
     * подставляется отдельно, уже подсвеченным. Конкатенация
     * header + "\n" + body + "\n:::" байт в байт совпадает с прежним выводом
     * buildExpandedBlock(), поэтому фронту менять ничего не нужно.
     */
    public String buildBlockHeader(Block block) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("id", block.getId().toString());
        metadata.put("spoilerType", block.hasChance() ? "chance" : "normal");

        List<String> roleIds = block.getRoles().stream()
                .map(role -> String.valueOf(role.getId()))
                .collect(Collectors.toList());
        metadata.put("roles", roleIds);

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

        return "::: restrictedBlock " + metadataJson;
    }
}
