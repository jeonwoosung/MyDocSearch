package com.example.mailsearch.dto;

public class DrmFileItemResponse {
    private final String id;
    private final String title;
    private final String path;
    private final long lastModified;

    public DrmFileItemResponse(String id, String title, String path, long lastModified) {
        this.id = id;
        this.title = title;
        this.path = path;
        this.lastModified = lastModified;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getPath() {
        return path;
    }

    public long getLastModified() {
        return lastModified;
    }
}
