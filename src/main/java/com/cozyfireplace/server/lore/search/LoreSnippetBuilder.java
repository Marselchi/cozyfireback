package com.cozyfireplace.server.lore.search;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.cozyfireplace.server.lore.search.LoreDocumentAssembler.BlockRegion;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Поиск подсвеченных вхождений в собранном документе и нарезка контекстных
 * чанков вокруг них.
 */
@Component
@RequiredArgsConstructor
public class LoreSnippetBuilder {

    public static final String MATCH_START = "==";
    public static final String MATCH_END = "==";
    public static final String HIGHLIGHT_OPTIONS =
            "StartSel=" + MATCH_START + ", StopSel=" + MATCH_END + ", HighlightAll=true";

    private final LoreSearchProperties properties;

    /**
     * Индексы вхождений ВМЕСТЕ с маркерами.
     * <p>
     * Арифметика длин была написана под односимвольные маркеры (startIdx + 1),
     * а маркеры давно двухсимвольные — каждый диапазон заканчивался на символ
     * раньше настоящего закрывающего "==". При contextChars=300 окно всё равно
     * захватывало потерянный символ, поэтому баг не проявлялся, но matchLen в
     * ветке обрезки по maxChunkSize считался неверно. Теперь длина берётся из
     * самих констант.
     */
    public List<int[]> findOccurrences(String highlighted) {
        List<int[]> ranges = new ArrayList<>();
        if (highlighted == null || highlighted.isEmpty()) {
            return ranges;
        }

        int searchPos = 0;
        while (true) {
            int startIdx = highlighted.indexOf(MATCH_START, searchPos);
            if (startIdx == -1) {
                break;
            }

            int matchTextStart = startIdx + MATCH_START.length();
            int endIdx = highlighted.indexOf(MATCH_END, matchTextStart);
            if (endIdx == -1) {
                break;
            }

            ranges.add(new int[]{startIdx, endIdx + MATCH_END.length()});
            searchPos = endIdx + MATCH_END.length();
        }
        return ranges;
    }

    /**
     * Строит окна контекста вокруг каждого вхождения, обрезает переросшие,
     * сливает пересекающиеся/соседние до maxChunkSize, восстанавливает
     * заголовок блока, если окно начинается внутри блока, и закрывает
     * незакрытую разметку на хвосте чанка.
     */
    public List<Chunk> buildChunks(String content, List<int[]> occurrences, List<BlockRegion> regions) {
        int contextChars = properties.getContextChars();
        int maxChunkSize = properties.getMaxChunkSize();
        int len = content.length();

        List<int[]> windows = new ArrayList<>(); // [start, end]
        for (int[] match : occurrences) {
            int matchLen = match[1] - match[0]; // длина включает маркеры

            int start = Math.max(0, match[0] - contextChars);
            int end = Math.min(len, match[1] + contextChars);

            if (end - start > maxChunkSize) {
                int allowedContext = Math.max(0, maxChunkSize - matchLen);
                int perSide = allowedContext / 2;
                start = Math.max(0, match[0] - perSide);
                end = Math.min(len, match[1] + perSide);
            }

            windows.add(new int[]{start, end});
        }

        List<Chunk> chunks = new ArrayList<>();
        Chunk current = null;

        for (int[] window : windows) {
            if (current == null) {
                current = new Chunk(window[0], window[1], 1);
                continue;
            }

            int mergedEnd = Math.max(current.end, window[1]);
            boolean overlaps = window[0] <= current.end;
            boolean withinSize = (mergedEnd - current.start) <= maxChunkSize;

            if (overlaps && withinSize) {
                current.end = mergedEnd;
                current.occurrenceCount++;
            } else {
                chunks.add(current);
                current = new Chunk(window[0], window[1], 1);
            }
        }
        if (current != null) {
            chunks.add(current);
        }

        for (Chunk chunk : chunks) {
            String prefix = repairBlockHeader(chunk, regions);

            String raw = prefix + content.substring(chunk.start, chunk.end);
            String closed = closeDanglingMarkup(raw);

            // Обозначаем обрезку "...". Добавляем СНАРУЖИ уже восстановленных/
            // закрытых маркеров разметки.
            //
            // Инлайновые маркеры (**, ~~, `) можно клеить вплотную. А ":::"
            // (граница restricted-блока) обязан стоять ОДИН на строке — это
            // требование и closeDanglingMarkup, и фронтового рендерера. Если
            // приклеить "..." к ":::" на той же строке, граница перестаёт
            // распознаваться и спойлер "не закрывается".
            boolean trimmedStart = chunk.start > 0 || !prefix.isEmpty();
            boolean trimmedEnd = chunk.end < len;

            if (!trimmedStart && !trimmedEnd) {
                chunk.text = closed;
            } else {
                StringBuilder sb = new StringBuilder(closed.length() + 8);
                if (trimmedStart) {
                    sb.append("...");
                    if (closed.startsWith(":::")) {
                        sb.append('\n');
                    }
                }
                sb.append(closed);
                if (trimmedEnd) {
                    if (closed.endsWith(":::")) {
                        sb.append('\n');
                    }
                    sb.append("...");
                }
                chunk.text = sb.toString();
            }
        }

        return chunks;
    }

    /**
     * Если окно начинается внутри блока, открывающий заголовок в него не попал.
     * closeDanglingMarkup обрабатывает ":::" простым toggle и не умеет
     * восстанавливать оборванный открывающий маркер блока (в отличие от
     * инлайновых, для которых есть prependMarkers): чанк, в котором есть только
     * ЗАКРЫВАЮЩИЙ ":::", будет принят за открывающий, к нему допишется ещё
     * один — и фронт отрисует пустой спойлер, а текст вывалится наружу.
     * <p>
     * Поэтому заголовок возвращается явно. Заодно start подтягивается к началу
     * тела, чтобы окно не разрезало JSON метаданных пополам.
     *
     * @return префикс для чанка ("" если чинить нечего)
     */
    private String repairBlockHeader(Chunk chunk, List<BlockRegion> regions) {
        for (BlockRegion region : regions) {
            if (chunk.start > region.start() && chunk.start < region.end()) {
                chunk.start = Math.max(chunk.start, region.bodyStart());
                if (chunk.end < chunk.start) {
                    chunk.end = chunk.start;
                }
                return region.header() + "\n";
            }
        }
        return "";
    }

    private String closeDanglingMarkup(String text) {
        Deque<String> openMarkers = new ArrayDeque<>();
        List<String> prependMarkers = new ArrayList<>(); // восстановление оборванных открывающих
        int n = text.length();
        int i = 0;

        while (i < n) {
            char c = text.charAt(i);

            if (c == '\\' && i + 1 < n) {
                i += 2;
                continue;
            }

            if (c == '`') {
                int j = i;
                while (j < n && text.charAt(j) == '`') j++;
                processMarker(openMarkers, prependMarkers, text.substring(i, j), i, j, text);
                i = j;
                continue;
            }

            if (c == '~' && i + 1 < n && text.charAt(i + 1) == '~') {
                processMarker(openMarkers, prependMarkers, "~~", i, i + 2, text);
                i += 2;
                continue;
            }

            if (c == '*') {
                boolean atLineStart = (i == 0 || text.charAt(i - 1) == '\n');
                boolean isBullet = atLineStart && i + 1 < n && (text.charAt(i + 1) == ' ' || text.charAt(i + 1) == '\t');
                if (isBullet) {
                    i++;
                    continue;
                }

                int markerLen = (i + 1 < n && text.charAt(i + 1) == '*') ? 2 : 1;
                processMarker(openMarkers, prependMarkers, markerLen == 2 ? "**" : "*", i, i + markerLen, text);
                i += markerLen;
                continue;
            }

            if (text.startsWith(":::", i) && (i == 0 || text.charAt(i - 1) == '\n')) {
                if (!openMarkers.isEmpty() && ":::".equals(openMarkers.peek())) {
                    openMarkers.pop();
                } else {
                    openMarkers.push(":::");
                }
                // Перепрыгиваем ВСЮ строку, а не 3 символа: в открывающей строке
                // блока дальше идёт JSON метаданных, и сканер иначе зацепит в нём
                // '*', '~~' или '`' из названия навыка / роли и сломает баланс
                // маркеров. Содержимое заголовка нас вообще не касается.
                int nl = text.indexOf('\n', i);
                i = (nl == -1) ? n : nl;
                continue;
            }

            i++;
        }

        StringBuilder result = new StringBuilder();

        for (String marker : prependMarkers) {
            result.append(marker);
        }
        result.append(text);
        while (!openMarkers.isEmpty()) {
            String marker = openMarkers.pop();
            result.append(":::".equals(marker) ? "\n:::" : marker);
        }

        return result.toString();
    }

    private void processMarker(Deque<String> openMarkers, List<String> prependMarkers,
                               String token, int startIdx, int endIdx, String text) {
        char before = startIdx > 0 ? text.charAt(startIdx - 1) : ' ';
        char after = endIdx < text.length() ? text.charAt(endIdx) : ' ';

        // Слева не пробел, справа пробел/конец — считаем закрывающим.
        boolean isClosing = !Character.isWhitespace(before) && Character.isWhitespace(after);

        if (isClosing) {
            if (!openMarkers.isEmpty() && openMarkers.peek().equals(token)) {
                openMarkers.pop();
            } else {
                prependMarkers.add(token);
            }
        } else {
            if (!openMarkers.isEmpty() && openMarkers.peek().equals(token)) {
                openMarkers.pop();
            } else {
                openMarkers.push(token);
            }
        }
    }

    /**
     * Изменяемый аккумулятор слитого окна.
     */
    public static class Chunk {
        public int start;
        public int end;
        public int occurrenceCount;
        public String text;

        public Chunk(int start, int end, int occurrenceCount) {
            this.start = start;
            this.end = end;
            this.occurrenceCount = occurrenceCount;
        }
    }
}