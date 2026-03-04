package com.example.mailsearch.model;

public class AttachmentInfo {
    private final int index;
    private final String name;
    private final String contentType;
    private final byte[] bytes;
    private final String extractedText;

    public AttachmentInfo(int index, String name, String contentType, byte[] bytes, String extractedText) {
        this.index = index;
        this.name = name;
        this.contentType = contentType;
        this.bytes = bytes;
        this.extractedText = extractedText;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getExtractedText() {
        return extractedText;
    }
}
