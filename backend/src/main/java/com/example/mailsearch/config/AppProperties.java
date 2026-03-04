package com.example.mailsearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String emlRoot;
    private String indexDir;

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
}
