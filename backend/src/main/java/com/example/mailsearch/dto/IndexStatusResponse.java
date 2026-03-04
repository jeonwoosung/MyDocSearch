package com.example.mailsearch.dto;

import java.time.Instant;

public class IndexStatusResponse {
    private final String rootPath;
    private final String indexPath;
    private final int emlCount;
    private final int attachmentCount;
    private final int fileCount;
    private final Instant lastIndexedAt;

    public IndexStatusResponse(String rootPath, String indexPath, int emlCount, int attachmentCount, int fileCount, Instant lastIndexedAt) {
        this.rootPath = rootPath;
        this.indexPath = indexPath;
        this.emlCount = emlCount;
        this.attachmentCount = attachmentCount;
        this.fileCount = fileCount;
        this.lastIndexedAt = lastIndexedAt;
    }

    public String getRootPath() {
        return rootPath;
    }

    public String getIndexPath() {
        return indexPath;
    }

    public int getEmlCount() {
        return emlCount;
    }

    public int getAttachmentCount() {
        return attachmentCount;
    }

    public int getFileCount() {
        return fileCount;
    }

    public Instant getLastIndexedAt() {
        return lastIndexedAt;
    }
}
