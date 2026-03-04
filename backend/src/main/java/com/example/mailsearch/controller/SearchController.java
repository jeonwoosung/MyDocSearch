package com.example.mailsearch.controller;

import com.example.mailsearch.dto.DocumentDetailResponse;
import com.example.mailsearch.dto.SearchResponse;
import com.example.mailsearch.service.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public SearchResponse search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "all") String kind,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return searchService.search(q, kind, page, size);
    }

    @GetMapping("/{id}")
    public DocumentDetailResponse detail(@PathVariable String id) {
        return searchService.findById(id);
    }
}
