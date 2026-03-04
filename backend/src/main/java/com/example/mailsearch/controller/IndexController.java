package com.example.mailsearch.controller;

import com.example.mailsearch.dto.IndexOperationResponse;
import com.example.mailsearch.dto.IndexRequest;
import com.example.mailsearch.dto.IndexStatusResponse;
import com.example.mailsearch.service.IndexService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/index")
public class IndexController {
    private final IndexService indexService;

    public IndexController(IndexService indexService) {
        this.indexService = indexService;
    }

    @GetMapping("/status")
    public IndexStatusResponse status() {
        return indexService.status();
    }

    @PostMapping("/rebuild")
    public IndexOperationResponse rebuild(@RequestBody(required = false) IndexRequest request) {
        String rootPath = request == null ? null : request.getRootPath();
        return indexService.rebuild(rootPath);
    }

    @PostMapping("/update")
    public IndexOperationResponse update(@RequestBody(required = false) IndexRequest request) {
        String rootPath = request == null ? null : request.getRootPath();
        return indexService.update(rootPath);
    }

    @DeleteMapping
    public IndexOperationResponse deleteAll() {
        return indexService.deleteAll();
    }
}
