package com.cozyfireplace.server.lore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoreAnchorExtractor {

    @Value("${app.frontend.protocol:http?}")
    private String protocolRegex;

    @Value("${app.frontend.host}")
    private String host;

    public Pattern buildLoreLinkPattern() {
        String safeHost = Pattern.quote(host);

        // (www\.)? - опциональный префикс www.
        // (\.ru)? - опциональный суффикс .ru (не группа захвата, ?:)
        String regex = "\\[([^]]+)]\\(" +
                "https?://" +
                "(www\\.)?" +      // опциональный www. (группа 2)
                safeHost +
                "(?:\\.ru)?" +     // опциональный .ru (НЕ группа захвата)
                "/rooms/[^/]+/lore/(\\d+)#([^)]+)\\)";

        return Pattern.compile(regex);
    }


    /**
     * Извлекает список якорей (loreId + header) из контента.
     * Возвращает List<AnchorLink> с информацией о каждой ссылке.
     */
    public List<AnchorLink> extractAnchorLinks(String content) {
        if (content == null || content.isEmpty()) return List.of();
        
        List<AnchorLink> links = new ArrayList<>();
        Pattern pattern = buildLoreLinkPattern();
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String linkText = matcher.group(1);
            Long loreId = Long.parseLong(matcher.group(3));
            String header = matcher.group(4);

            links.add(new AnchorLink(loreId, header, linkText));
        }


        return links;
    }

    /**
     * Удаляет все ссылки с недоступными якорями.
     * Если якорь (loreId, header) не в validAnchors -> заменяет [текст](...) на просто текст.
     */
    public String removeInvalidLinks(String content, Set<String> validAnchors) {
        if (content == null || content.isEmpty()) return content;

        Pattern pattern = buildLoreLinkPattern();
        Matcher matcher = pattern.matcher(content);
        StringBuilder out = new StringBuilder(content.length());

        while (matcher.find()) {
            String linkText = matcher.group(1);
            Long loreId = Long.parseLong(matcher.group(3));
            String header = matcher.group(4);

            // Ключ якоря: "loreId:header"
            String anchorKey = loreId + ":" + header;

            // Если якорь доступен -> оставляем ссылку, иначе -> только текст
            String replacement = validAnchors.contains(anchorKey)
                    ? Matcher.quoteReplacement(matcher.group(0))
                    : Matcher.quoteReplacement(linkText);         // только текст

            matcher.appendReplacement(out, replacement);
        }
        matcher.appendTail(out);

        return out.toString();
    }


    /**
         * DTO для информации о якоре
         */
        public record AnchorLink(Long loreId, String header, String linkText) {
    }
}

