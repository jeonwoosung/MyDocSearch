package com.example.mailsearch.model;

import java.time.Instant;
import java.util.List;

public class EmlParseResult {
    private final String subject;
    private final String from;
    private final String to;
    private final Instant sentAt;
    private final String bodyText;
    private final List<AttachmentInfo> attachments;

    public EmlParseResult(String subject, String from, String to, Instant sentAt, String bodyText, List<AttachmentInfo> attachments) {
        this.subject = subject;
        this.from = from;
        this.to = to;
        this.sentAt = sentAt;
        this.bodyText = bodyText;
        this.attachments = attachments;
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

    public Instant getSentAt() {
        return sentAt;
    }

    public String getBodyText() {
        return bodyText;
    }

    public List<AttachmentInfo> getAttachments() {
        return attachments;
    }
}
