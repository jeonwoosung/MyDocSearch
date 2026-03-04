package com.example.mailsearch.dto;

import java.util.List;

public class SearchResponse {
    private final long total;
    private final List<SearchResultItem> items;

    public SearchResponse(long total, List<SearchResultItem> items) {
        this.total = total;
        this.items = items;
    }

    public long getTotal() {
        return total;
    }

    public List<SearchResultItem> getItems() {
        return items;
    }
}
