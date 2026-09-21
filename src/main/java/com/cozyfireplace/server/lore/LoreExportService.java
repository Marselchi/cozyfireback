package com.cozyfireplace.server.lore;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.lore.dto.IdName;
import com.cozyfireplace.server.rooms.Room;
import com.cozyfireplace.server.rooms.RoomRepository;
import com.cozyfireplace.server.rooms.dto.LoreExportRequest;
import com.cozyfireplace.server.util.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoreExportService {

    private static final Pattern MARKDOWN_LINK = Pattern.compile(
            "\\[([^\\]]*)]\\([^)]*/lore/(\\d+)(?:#([^)]*))?\\)"
    );
    

    private final LoreRepository loreRepository;
    private final RoomRepository roomRepository;

    @Value("${app.domain:http://localhost:3000}")
    private String appDomain;

    @Transactional(readOnly = true)
    public byte[] exportLore(Long roomId, LoreExportRequest filters, Account account) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room", roomId));

        List<Lore> lores = getLoreForExport(roomId, filters.dmOnly(), room);

        Map<Long, Lore> loreById = new HashMap<>();
        for (Lore lore : lores) {
            loreById.put(lore.getId(), lore);
        }

        Map<Long, List<IdName>> tagsByLoreId = loreRepository.findTagsByLoreIds(
                        lores.stream().map(Lore::getId).toList()
                ).stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        LoreRepository.LoreTagRow::getLoreId,
                        java.util.stream.Collectors.mapping(
                                t -> new IdName(t.getTagId(), t.getTagName()),
                                java.util.stream.Collectors.toList()
                        )
                ));

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("lore-export-", ".zip");
            Set<String> usedNames = new HashSet<>();

            try (var fos = Files.newOutputStream(tempFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                for (Lore lore : lores) {
                    String baseName = sanitizeFileName(lore.getTitle());
                    String fileName = getUniqueFileName(baseName + ".md", usedNames);
                    String content = buildLoreContent(lore, tagsByLoreId.get(lore.getId()), filters);
                    content = convertLinksToWiki(content, loreById);

                    ZipEntry entry = new ZipEntry(fileName);
                    zos.putNextEntry(entry);
                    zos.write(content.getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();
                }

                zos.finish();
                zos.flush();
            }

            byte[] zipBytes = Files.readAllBytes(tempFile);
            log.info("Exported ZIP size: {} bytes, entries: {}", zipBytes.length, lores.size());
            log.info("Temp ZIP file: {}", tempFile);
            return zipBytes;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create ZIP archive", e);
        }
    }

    private List<Lore> getLoreForExport(Long roomId, boolean dmOnly, Room room) {
        List<Lore> allLore = loreRepository.findAllByRoomId(roomId);

        if (dmOnly) {
            Long creatorId = room.getCreator().getId();
            return allLore.stream()
                    .filter(lore -> Objects.equals(lore.getAccount().getId(), creatorId))
                    .toList();
        }

        return allLore;
    }

    private String buildLoreContent(Lore lore, List<IdName> tags, LoreExportRequest filters) {
        StringBuilder sb = new StringBuilder();

        if (filters.saveTags() && tags != null && !tags.isEmpty()) {
            for (IdName tag : tags) {
                sb.append("#").append(tag.name()).append(" ");
            }
            sb.append("\n");
        }

        if (lore.getDescription() != null && !lore.getDescription().isBlank()) {
            sb.append("Описание: ").append(lore.getDescription()).append("\n");
        }

        if (lore.getDate() != null && !lore.getDate().isBlank()) {
            sb.append("Дата: ").append(lore.getDate()).append("\n");
        }

        sb.append("\n");

        String content = lore.getContent();
        sb.append(content);

        return sb.toString();
    }


    private String convertLinksToWiki(String content, Map<Long, Lore> loreById) {
        Matcher matcher = MARKDOWN_LINK.matcher(content);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String display = matcher.group(1);
            String loreIdStr = matcher.group(2);
            String header = matcher.group(3);

            Long loreId = Long.parseLong(loreIdStr);
            Lore linkedLore = loreById.get(loreId);

            String wikiLink;
            if (linkedLore != null) {
                String title = linkedLore.getTitle();
                if (header != null && !header.isBlank()) {
                    wikiLink = "[[" + title + "#" + header + "|" + display + "]]";
                } else {
                    wikiLink = "[[" + title + "|" + display + "]]";
                }
            } else {
                wikiLink = display;
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(wikiLink));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) return "untitled";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String getUniqueFileName(String fileName, Set<String> usedNames) {
        if (!usedNames.contains(fileName)) {
            usedNames.add(fileName);
            return fileName;
        }

        String nameWithoutExt = fileName;
        String ext = ".md";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            nameWithoutExt = fileName.substring(0, dotIndex);
            ext = fileName.substring(dotIndex);
        }

        int counter = 1;
        while (true) {
            String newName = nameWithoutExt + " (" + counter + ")" + ext;
            if (!usedNames.contains(newName)) {
                usedNames.add(newName);
                return newName;
            }
            counter++;
        }
    }
}
