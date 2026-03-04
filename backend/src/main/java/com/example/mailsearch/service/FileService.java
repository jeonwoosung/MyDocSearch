package com.example.mailsearch.service;

import com.example.mailsearch.dto.DocumentDetailResponse;
import com.example.mailsearch.model.AttachmentInfo;
import com.example.mailsearch.util.EmlParser;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class FileService {
    private final SearchService searchService;
    private final EmlParser emlParser;

    public FileService(SearchService searchService, EmlParser emlParser) {
        this.searchService = searchService;
        this.emlParser = emlParser;
    }

    public ResponseEntity<Resource> download(String id) {
        DocumentDetailResponse doc = searchService.findById(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path sourcePath = Path.of(doc.getEmlPath());

            if (IndexService.KIND_EML.equals(doc.getKind())) {
                byte[] bytes = Files.readAllBytes(sourcePath);
                return fileResponse(bytes, sourcePath.getFileName().toString(), "message/rfc822");
            }

            if (IndexService.KIND_FILE.equals(doc.getKind())) {
                byte[] bytes = Files.readAllBytes(sourcePath);
                String contentType = Files.probeContentType(sourcePath);
                if (contentType == null || contentType.isBlank()) {
                    contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
                }
                return fileResponse(bytes, sourcePath.getFileName().toString(), contentType);
            }

            AttachmentInfo attachment = emlParser.loadAttachment(sourcePath, doc.getAttachmentIndex());
            if (attachment == null) {
                return ResponseEntity.notFound().build();
            }

            String contentType = attachment.getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            return fileResponse(attachment.getBytes(), attachment.getName(), contentType);
        } catch (Exception e) {
            throw new RuntimeException("파일 다운로드 중 오류가 발생했습니다.", e);
        }
    }

    public ResponseEntity<String> preview(String id) {
        DocumentDetailResponse doc = searchService.findById(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        String content = doc.getContent() == null ? "" : doc.getContent();
        if (doc.isDrm() && content.isBlank()) {
            content = "DRM 파일(SCDSA)로 확인되어 본문 색인은 제외되었습니다. 파일명만 색인되었습니다.";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }

    private ResponseEntity<Resource> fileResponse(byte[] bytes, String fileName, String contentType) {
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition(fileName))
                .contentLength(bytes.length)
                .body(resource);
    }

    private String buildContentDisposition(String originalFileName) {
        String safeFileName = sanitizeFileName(originalFileName);
        String asciiFallback = safeFileName.replaceAll("[^\\x20-\\x7E]", "_");
        if (asciiFallback.isBlank()) {
            asciiFallback = "download";
        }
        String encoded = URLEncoder.encode(safeFileName, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + encoded;
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "download";
        }
        return fileName
                .replace("\r", "_")
                .replace("\n", "_")
                .replace("\"", "_")
                .trim();
    }
}
