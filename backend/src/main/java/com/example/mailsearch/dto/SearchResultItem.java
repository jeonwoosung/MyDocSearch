package com.example.mailsearch.dto;

public class SearchResultItem {
    private final String id;
    private final String kind;
    private final String title;
    private final String subject;
    private final String emlPath;
    private final String attachmentName;
    private final int attachmentIndex;
    private final String snippet;
    private final boolean drm;

    public SearchResultItem(String id, String kind, String title, String subject, String emlPath,
                            String attachmentName, int attachmentIndex, String snippet, boolean drm) {
        this.id = id;
        this.kind = kind;
        this.title = title;
        this.subject = subject;
        this.emlPath = emlPath;
        this.attachmentName = attachmentName;
        this.attachmentIndex = attachmentIndex;
        this.snippet = snippet;
        this.drm = drm;
    }

    public String getId() {
        return id;
    }

    public String getKind() {
        return kind;
    }

    public String getTitle() {
        return title;
    }

    public String getSubject() {
        return subject;
    }

    public String getEmlPath() {
        return emlPath;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public int getAttachmentIndex() {
        return attachmentIndex;
    }

    public String getSnippet() {
        return snippet;
    }

    public boolean isDrm() {
        return drm;
    }
}
