package com.example.mailsearch.dto;

import java.time.Instant;

public class IndexStatusResponse {
    private final String rootPath;
    private final String indexPath;
    private final int emlCount;
    private final int attachmentCount;
    private final int fileCount;
    private final Instant lastIndexedAt;
    private final Instant lastRebuildAt;
    private final Instant lastIncrementalAt;

    public IndexStatusResponse(
            String rootPath,
            String indexPath,
            int emlCount,
            int attachmentCount,
            int fileCount,
            Instant lastIndexedAt,
            Instant lastRebuildAt,
            Instant lastIncrementalAt
    ) {
        this.rootPath = rootPath;
        this.indexPath = indexPath;
        this.emlCount = emlCount;
        this.attachmentCount = attachmentCount;
        this.fileCount = fileCount;
        this.lastIndexedAt = lastIndexedAt;
        this.lastRebuildAt = lastRebuildAt;
        this.lastIncrementalAt = lastIncrementalAt;
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

    public Instant getLastRebuildAt() {
        return lastRebuildAt;
    }

    public Instant getLastIncrementalAt() {
        return lastIncrementalAt;
    }
}
