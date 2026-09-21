package com.cozyfireplace.server.lore;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public final class LoreContentParser {

    private LoreContentParser() {
    }

    private static final Pattern BLOCK = Pattern.compile(
            "(?s):::\\s*restrictedBlock(?:[ \\t]+(\\{[^\\r\\n]*}))?[ \\t]*[\\r\\n]+" + // group(1)=metadata JSON (optional)
                    "(.*?)" +                                                          // group(2)=block content
                    "[\\r\\n]+:::",
            Pattern.MULTILINE
    );


    // Паттерн для поиска заголовков (h1-h6)
    private static final Pattern HEADING = Pattern.compile(
            "^(#{1,6})\\s+(.+?)$",
            Pattern.MULTILINE
    );

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Проходит по content и вырезает (полностью, вместе с маркерами) те restrictedBlock,
     * на которые у viewer'а нет прав. Блоки, которые остаются видимыми, не переоборачиваются —
     * формат в БД и формат для отображения теперь идентичны.
     */
    public static String parseRestrictedContent(String content,
                                                boolean canSeeAll,
                                                Set<Long> viewerRoleIds) {
        if (content == null || content.isEmpty()) return content;
        if (canSeeAll) return content; // нечего вырезать, формат уже финальный

        Matcher m = BLOCK.matcher(content);
        StringBuilder out = new StringBuilder(content.length());

        while (m.find()) {
            Set<Long> roleIds = extractRoleIds(m.group(1));
            boolean allow = hasAnyRoleId(roleIds, viewerRoleIds);

            String replacement = allow ? m.group(0) : "";
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);

        return out.toString();
    }

    private static Set<Long> extractRoleIds(String metadataRaw) {
        if (metadataRaw == null || metadataRaw.isBlank()) return Set.of();

        try {
            JsonNode node = MAPPER.readTree(metadataRaw);
            JsonNode rolesNode = node.get("roles");
            if (rolesNode == null || !rolesNode.isArray()) return Set.of();

            Set<Long> result = new HashSet<>();
            for (JsonNode roleNode : rolesNode) {
                if (roleNode.isNumber()) result.add(roleNode.asLong());
            }
            return result;
        } catch (Exception e) {
            return Set.of();
        }
    }

    /**
     * Парсит все заголовки и их контент один раз.
     * Возвращает Map<slug, excerpt>.
     */
    /**
     * Парсит контент и извлекает только нужные заголовки.
     *
     * @param content          контент лора
     * @param headersToExtract список заголовков, которые нужно извлечь
     * @param canSeeAll        может ли юзер видеть весь контент
     * @param viewerRoleIds    роли юзера
     * @return Map<headerSlug, headerContent>
     */
    public static Map<String, String> extractAllExcerpts(String content,
                                                         List<String> headersToExtract,
                                                         boolean canSeeAll,
                                                         Set<Long> viewerRoleIds) {
        if (content == null || content.isEmpty() || headersToExtract == null || headersToExtract.isEmpty()) {
            return Map.of();
        }

        // Очищаем контент один раз
        String cleanedContent = parseRestrictedContent(content, canSeeAll, viewerRoleIds);

        Map<String, String> result = new LinkedHashMap<>();
        Set<String> headersToFind = headersToExtract.stream()
                .map(LoreContentParser::textToSlug)
                .collect(Collectors.toSet());

        Matcher headingMatcher = HEADING.matcher(cleanedContent);

        while (headingMatcher.find()) {
            String hLevel = headingMatcher.group(1);
            String hText = headingMatcher.group(2);
            String hSlug = textToSlug(hText);

            // Берём только те заголовки, которые нужны
            if (!headersToFind.contains(hSlug)) continue;

            int headingStart = headingMatcher.start();
            int headingLevel = hLevel.length();

            // Находим конец раздела
            int endPos = cleanedContent.length();
            Matcher nextMatcher = HEADING.matcher(cleanedContent);
            nextMatcher.region(headingStart + hLevel.length(), cleanedContent.length());

            while (nextMatcher.find()) {
                int nextHeadingLevel = nextMatcher.group(1).length();
                if (nextHeadingLevel <= headingLevel) {
                    endPos = nextMatcher.start();
                    break;
                }
            }

            String excerpt = cleanedContent.substring(headingStart, endPos).trim();
            if (excerpt.length() > 500) {
                excerpt = excerpt.substring(0, 500).trim() + "...";
            }

            result.put(hSlug, excerpt);
        }

        return result;
    }


    /**
     * Конвертирует текст в slug формат (нижний регистр, пробелы->тире, спецсимволы удаляются).
     */
    public static String textToSlug(String text) {
        if (text == null || text.isEmpty()) return "";

        return text.toLowerCase()
                .replaceAll("[^a-zа-я0-9\\s-]", "")  // Удаляем спецсимволы
                .replaceAll("\\s+", "-")           // Пробелы -> тире
                .replaceAll("-+", "-")             // Множественные тире -> одно
                .replaceAll("^-|-$", "");           // Удаляем тире в начале/конце
    }

    public static String encodeHeaderSlug(String text) {
        if (text == null || text.isEmpty()) return "";

        try {
            return URLEncoder.encode(text, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private static boolean hasAnyRoleId(Set<Long> roleIds, Set<Long> viewerRoleIds) {
        if (viewerRoleIds == null || viewerRoleIds.isEmpty()) return false;
        if (roleIds == null || roleIds.isEmpty()) return false;

        for (Long id : roleIds) {
            if (viewerRoleIds.contains(id)) return true;
        }
        return false;
    }
}