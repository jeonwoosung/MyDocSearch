package com.example.mailsearch.controller;

import com.example.mailsearch.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable String id) {
        return fileService.download(id);
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<String> preview(@PathVariable String id) {
        return fileService.preview(id);
    }
}
