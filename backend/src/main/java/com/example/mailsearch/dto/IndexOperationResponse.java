package com.example.mailsearch.dto;

public class IndexOperationResponse {
    private final String message;
    private final int emlCount;
    private final int attachmentCount;
    private final int fileCount;

    public IndexOperationResponse(String message, int emlCount, int attachmentCount, int fileCount) {
        this.message = message;
        this.emlCount = emlCount;
        this.attachmentCount = attachmentCount;
        this.fileCount = fileCount;
    }

    public String getMessage() {
        return message;
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
}
