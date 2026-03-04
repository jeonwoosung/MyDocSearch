package com.example.mailsearch.dto;

public class DocumentDetailResponse {
    private final String id;
    private final String kind;
    private final String title;
    private final String subject;
    private final String from;
    private final String to;
    private final String emlPath;
    private final String attachmentName;
    private final int attachmentIndex;
    private final String content;
    private final boolean drm;

    public DocumentDetailResponse(String id, String kind, String title, String subject, String from, String to,
                                  String emlPath, String attachmentName, int attachmentIndex, String content,
                                  boolean drm) {
        this.id = id;
        this.kind = kind;
        this.title = title;
        this.subject = subject;
        this.from = from;
        this.to = to;
        this.emlPath = emlPath;
        this.attachmentName = attachmentName;
        this.attachmentIndex = attachmentIndex;
        this.content = content;
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

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
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

    public String getContent() {
        return content;
    }

    public boolean isDrm() {
        return drm;
    }
}
