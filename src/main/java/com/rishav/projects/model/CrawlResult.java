package com.rishav.projects.model;


/**
 * Represents the result of a web crawl operation.
 * This class can be extended to include properties such as status, content, and metadata.
 */
public class CrawlResult {

    private String url;
    private String title;
    private int statusCode;
    private String status;
    private  long durationMillis;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getStatusCode() {
        return statusCode;
    }
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
    public long getDurationMillis() {
        return durationMillis;
    }
    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    public CrawlResult() {
        // Default constructor
    }

    public CrawlResult(String url, String title, int statusCode, String status, long durationMillis) {
        this.url = url;
        this.title = title;
        this.statusCode = statusCode;
        this.status = status;
        this.durationMillis = durationMillis;
    }

    @Override
    public String toString() {
        return "CrawlResult{" +
                "url='" + url + '\'' +
                ", title='" + title + '\'' +
                ", statusCode=" + statusCode +
                ", status='" + status + '\'' +
                ", durationMillis=" + durationMillis +
                '}';
    }



}
