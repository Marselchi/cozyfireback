package com.cozyfireplace.server.lore;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.lore.dto.LoreRequest;
import com.cozyfireplace.server.roles.Role;
import com.cozyfireplace.server.rooms.Room;
import com.cozyfireplace.server.rooms.RoomRepository;
import com.cozyfireplace.server.rooms.dto.LoreImportRequest;
import com.cozyfireplace.server.tags.Tag;
import com.cozyfireplace.server.util.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoreImportService {

    private static final Pattern WIKI_LINK = Pattern.compile("\\[\\[([^\\]|#]+)(?:#([^\\]|]*))?\\|([^\\]]*)\\]\\]");

    private final LoreRepository loreRepository;
    private final RoomRepository roomRepository;
    private final LoreService loreService;

    @Value("${app.domain:http://localhost:3000}")
    private String appDomain;

    /**
     * Импорт одного MD файла
     */
    @Transactional
    public void importMdFile(Long roomId, MultipartFile file, LoreImportRequest filters, Account account) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room", roomId));

        if (!isMarkdownFile(file)) {
            throw new IllegalArgumentException("Only .md files are supported for single file import");
        }

        String content = readMarkdownContent(file);
        String title = extractTitleFromFileName(file.getOriginalFilename());

        processAndSaveLore(roomId, title, content, account, filters.autolink(), Map.of());
    }

    /**
     * Импорт ZIP архива
     */
    @Transactional
    public void importZipFile(Long roomId, MultipartFile file, LoreImportRequest filters, Account account) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room", roomId));

        if (filters.roomReset()) {
            deleteAllLoreInRoom(roomId);
        }

        Map<String, LoreFileData> archiveFiles = extractZipArchive(file);

        if (archiveFiles.isEmpty()) {
            throw new IllegalArgumentException("ZIP archive contains no markdown files");
        }

        Map<String, String> titleToPathMap = new HashMap<>();
        for (String path : archiveFiles.keySet()) {
            String title = extractTitleFromFileName(getFileNameFromPath(path));
            titleToPathMap.put(title, path);
        }

        Map<String, Long> reservedTitleToId = new HashMap<>();
        String roomUrl = room.getUrl();

        Set<String> processedFiles = new HashSet<>();
        Queue<String> processingQueue = new LinkedList<>();

        String firstFile = archiveFiles.keySet().iterator().next();
        processingQueue.add(firstFile);

        log.info("Starting ZIP import for room {}, files to process: {}", roomId, archiveFiles.size());

        while (!processingQueue.isEmpty() || processedFiles.size() < archiveFiles.size()) {
            while (!processingQueue.isEmpty()) {
                String currentPath = processingQueue.poll();

                if (processedFiles.contains(currentPath)) {
                    continue;
                }

                LoreFileData fileData = archiveFiles.get(currentPath);

                log.info("Picked path: {}, fileData is null: {}", currentPath, fileData == null);

                if (fileData == null) {
                    continue;
                }

                processedFiles.add(currentPath);

                String currentTitle = extractTitleFromFileName(getFileNameFromPath(currentPath));
                log.info("Processing file: {} -> title: {}", currentPath, currentTitle);
                String processedContent = processWikiLinks(
                        fileData.content(),
                        filters.autolink(),
                        archiveFiles,
                        titleToPathMap,
                        reservedTitleToId,
                        roomId,
                        roomUrl
                );

                archiveFiles.put(currentPath, new LoreFileData(processedContent));

                Long loreId = saveOrUpdateLore(
                        roomId,
                        currentTitle,
                        processedContent,
                        account,
                        filters.replaceOnConflict(),
                        reservedTitleToId
                );

                log.info("Saved Lore with id: {} for title: {}", loreId, currentTitle);
                reservedTitleToId.putIfAbsent(currentTitle, loreId);
            }

            if (processedFiles.size() < archiveFiles.size()) {
                for (String path : archiveFiles.keySet()) {
                    if (!processedFiles.contains(path)) {
                        processingQueue.add(path);
                        break;
                    }
                }
            }
        }
    }

    private String processWikiLinks(String content,
                                    boolean autolink,
                                    Map<String, LoreFileData> archiveFiles,
                                    Map<String, String> titleToPathMap,
                                    Map<String, Long> reservedTitleToId,
                                    Long roomId,
                                    String roomUrl) {

        Matcher matcher = WIKI_LINK.matcher(content);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String fileName = matcher.group(1).trim();
            String header = matcher.group(2);
            String display = matcher.group(3);

            String linkReplacement = processSingleLink(fileName, header, display, autolink, archiveFiles,
                    titleToPathMap, reservedTitleToId, roomId, roomUrl);

            matcher.appendReplacement(result, Matcher.quoteReplacement(linkReplacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String processSingleLink(String fileName,
                                     String header,
                                     String display,
                                     boolean autolink,
                                     Map<String, LoreFileData> archiveFiles,
                                     Map<String, String> titleToPathMap,
                                     Map<String, Long> reservedTitleToId,
                                     Long roomId,
                                     String roomUrl) {

        if (!autolink) {
            return display;
        }

        Long loreId = reservedTitleToId.get(fileName);

        if (loreId != null) {
            String link = buildLoreLink(roomUrl, loreId, header);
            return "[" + display + "](" + link + ")";
        }

        Optional<Lore> existingLore = loreRepository.findByTitleAndRoomId(fileName, roomId);

        if (existingLore.isPresent()) {
            Lore lore = existingLore.get();
            String link = buildLoreLink(roomUrl, lore.getId(), header);
            return "[" + display + "](" + link + ")";
        }

        String archiveFilePath = titleToPathMap.get(fileName);
        if (archiveFilePath != null) {
            return display;
        }

        return display;
    }

    private String buildLoreLink(String roomUrl, Long loreId, String header) {
        String baseLink = appDomain + "/rooms/" + roomUrl + "/lore/" + loreId;
        if (header != null && !header.isBlank()) {
            String slug = LoreContentParser.textToSlug(header);
            String encoded = LoreContentParser.encodeHeaderSlug(header);
            return baseLink + "#" + encoded;
        }
        return baseLink;
    }

    private Long saveOrUpdateLore(Long roomId,
                                  String title,
                                  String content,
                                  Account account,
                                  boolean replaceOnConflict,
                                  Map<String, Long> reservedTitleToId) {

        Optional<Lore> existingLore = loreRepository.findByTitleAndRoomId(title, roomId);

        if (existingLore.isPresent()) {
            if (replaceOnConflict) {
                Lore lore = existingLore.get();
                Set<Long> roleIds = lore.getRoles().stream()
                        .map(Role::getId)
                        .collect(java.util.stream.Collectors.toSet());
                Set<Long> tagIds = lore.getTags().stream()
                        .map(Tag::getId)
                        .collect(java.util.stream.Collectors.toSet());
                LoreRequest request = new LoreRequest(
                        title,
                        lore.getDescription(),
                        lore.getDate(),
                        content,
                        null,
                        roleIds,
                        tagIds
                );
                loreService.updateLore(lore.getId(), request, account, roomId);
                return lore.getId();
            } else {
                return existingLore.get().getId();
            }
        }

        LoreRequest request = new LoreRequest(
                title,
                null,
                null,
                content,
                null,
                Set.of(),
                Set.of()
        );

        return loreService.createLore(roomId, account, request);
    }

    private void processAndSaveLore(Long roomId,
                                    String title,
                                    String content,
                                    Account account,
                                    boolean autolink,
                                    Map<String, Long> reservedIds) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room", roomId));

        String processedContent = processWikiLinks(
                content,
                autolink,
                Map.of(),
                Map.of(),
                reservedIds,
                roomId,
                room.getUrl()
        );

        saveOrUpdateLore(roomId, title, processedContent, account, false, reservedIds);
    }

    @Transactional
    public void deleteAllLoreInRoom(Long roomId) {
        List<Lore> lores = loreRepository.findAllByRoomId(roomId);
        loreRepository.deleteAll(lores);
    }

    private Map<String, LoreFileData> extractZipArchive(MultipartFile zipFile) {
        Map<String, LoreFileData> files = new HashMap<>();

        try {
            byte[] zipBytes = zipFile.getInputStream().readAllBytes();

            Charset[] filenameCharsets = {
                    StandardCharsets.UTF_8,
                    Charset.forName("Cp866"),
                    Charset.forName("windows-1251"),
                    Charset.forName("IBM437")
            };

            Charset[] contentCharsets = {
                    StandardCharsets.UTF_8,
                    Charset.forName("windows-1251"),
                    Charset.forName("Cp866"),
                    Charset.forName("KOI8-R")
            };

            for (Charset filenameCharset : filenameCharsets) {
                Path tempFile = null;
                try {
                    tempFile = Files.createTempFile("zip-import-", ".zip");
                    Files.write(tempFile, zipBytes);

                    Map<String, byte[]> rawEntries = new LinkedHashMap<>();
                    try (ZipFile zf = new ZipFile(tempFile.toFile(), filenameCharset)) {
                        Enumeration<? extends ZipEntry> entries = zf.entries();
                        while (entries.hasMoreElements()) {
                            ZipEntry entry = entries.nextElement();
                            if (!entry.isDirectory() && isMarkdownFileName(entry.getName())) {
                                rawEntries.put(entry.getName(), zf.getInputStream(entry).readAllBytes());
                            }
                        }
                    }

                    Files.deleteIfExists(tempFile);

                    if (rawEntries.isEmpty()) {
                        continue;
                    }

                    log.info("Found {} markdown entries with filename charset: {}", rawEntries.size(), filenameCharset);

                    for (Charset contentCharset : contentCharsets) {
                        Map<String, LoreFileData> decodedFiles = new LinkedHashMap<>();
                        boolean allDecoded = true;

                        for (Map.Entry<String, byte[]> entry : rawEntries.entrySet()) {
                            try {
                                String content = new String(entry.getValue(), contentCharset);
                                decodedFiles.put(entry.getKey(), new LoreFileData(content));
                            } catch (Exception e) {
                                allDecoded = false;
                                break;
                            }
                        }

                        if (allDecoded && !decodedFiles.isEmpty()) {
                            files.putAll(decodedFiles);
                            log.info("Successfully decoded content with charset: {}", contentCharset);
                            break;
                        }
                    }

                    if (!files.isEmpty()) {
                        break;
                    }
                } catch (Exception e) {
                    log.debug("Filename charset {} failed for ZIP: {}", filenameCharset, e.getMessage());
                    assert tempFile != null;
                    Files.deleteIfExists(tempFile);
                }
            }

            if (files.isEmpty()) {
                throw new IllegalArgumentException("Failed to extract ZIP archive: unable to parse with any charset");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read ZIP file", e);
        }

        return files;
    }

    private boolean isMarkdownFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        return filename != null && (filename.toLowerCase().endsWith(".md") || filename.toLowerCase().endsWith(".markdown"));
    }

    private boolean isMarkdownFileName(String filename) {
        return filename.toLowerCase().endsWith(".md") || filename.toLowerCase().endsWith(".markdown");
    }

    private String readMarkdownContent(MultipartFile file) {
        try {
            return readStreamContent(file.getInputStream());
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read markdown file", e);
        }
    }

    private String readStreamContent(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }

    private String extractTitleFromFileName(String fileName) {
        if (fileName == null) {
            throw new IllegalArgumentException("File name is null");
        }
        String name = fileName.contains("/") || fileName.contains("\\")
                ? fileName.substring(fileName.lastIndexOf('/') + 1)
                : fileName;

        int dotIndex = name.lastIndexOf('.');
        return dotIndex > 0 ? name.substring(0, dotIndex) : name;
    }

    private String getFileNameFromPath(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path is null");
        }
        int separatorIndex = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return separatorIndex >= 0 ? path.substring(separatorIndex + 1) : path;
    }

    private record LoreFileData(String content) {
    }
}
