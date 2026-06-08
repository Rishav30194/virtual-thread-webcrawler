package com.rishav.projects.model;

import java.util.List;

/**
 * Immutable result of fetching a single URL.
 *
 * @param url            the URL that was fetched
 * @param depth          BFS depth at which this URL was reached (seeds are depth 0)
 * @param statusCode     HTTP status code, or 0 if the request never completed
 * @param status         a short human-readable status ("OK", "Forbidden", "ERROR: ...")
 * @param title          the page's &lt;title&gt;, or "" when absent/unavailable
 * @param durationMillis wall-clock time taken to fetch this URL, in milliseconds
 * @param links          absolute http(s) links discovered on the page (empty on failure)
 */
public record CrawlResult(
        String url,
        int depth,
        int statusCode,
        String status,
        String title,
        long durationMillis,
        List<String> links) {

    public CrawlResult {
        links = links == null ? List.of() : List.copyOf(links);
    }

    /** True when the request returned a 2xx status. */
    public boolean ok() {
        return statusCode >= 200 && statusCode < 300;
    }

    /** Convenience factory for a failed fetch (no response / exception). */
    public static CrawlResult failure(String url, int depth, String error, long durationMillis) {
        return new CrawlResult(url, depth, 0, "ERROR: " + error, "", durationMillis, List.of());
    }
}
