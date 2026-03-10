package com.example.mailsearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String emlRoot;
    private String indexDir;
    private String dbPath;
    private String authUsername;
    private String authPassword;
    private long maxExtractBytes = 20971520L;
    private int maxExtractChars = 200000;

    public String getEmlRoot() {
        return emlRoot;
    }

    public void setEmlRoot(String emlRoot) {
        this.emlRoot = emlRoot;
    }

    public String getIndexDir() {
        return indexDir;
    }

    public void setIndexDir(String indexDir) {
        this.indexDir = indexDir;
    }

    public String getDbPath() {
        return dbPath;
    }

    public void setDbPath(String dbPath) {
        this.dbPath = dbPath;
    }

    public String getAuthUsername() {
        return authUsername;
    }

    public void setAuthUsername(String authUsername) {
        this.authUsername = authUsername;
    }

    public String getAuthPassword() {
        return authPassword;
    }

    public void setAuthPassword(String authPassword) {
        this.authPassword = authPassword;
    }

    public long getMaxExtractBytes() {
        return maxExtractBytes;
    }

    public void setMaxExtractBytes(long maxExtractBytes) {
        this.maxExtractBytes = maxExtractBytes;
    }

    public int getMaxExtractChars() {
        return maxExtractChars;
    }

    public void setMaxExtractChars(int maxExtractChars) {
        this.maxExtractChars = maxExtractChars;
    }
}
