package com.rishav.projects.model;

import java.util.Map;

/**
 * Represents the result of a web crawl operation.
 * This class can be extended to include properties such as status, content, and metadata.
 */
public class CrawlResult {

    private String url;
    private String status;
    private String content;
    private Map<String, String> metadata;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public CrawlResult() {
        // Default constructor
    }

    public CrawlResult(String url, String status, String content, Map<String, String> metadata) {
        this.url = url;
        this.status = status;
        this.content = content;
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "CrawlResult{" +
                "url='" + url + '\'' +
                ", status='" + status + '\'' +
                ", content='" + content + '\'' +
                ", metadata=" + metadata +
                '}';
    }
}
