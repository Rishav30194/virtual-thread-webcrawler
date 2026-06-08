package com.rishav.projects.service;

import com.rishav.projects.model.CrawlResult;
import com.rishav.projects.util.CrawlerUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Crawls web pages concurrently using virtual threads (Java 21+).
 *
 * <p>A single {@link HttpClient} is shared across all requests (connection
 * pooling), every request is bounded by a timeout, and the number of in-flight
 * requests is capped with a {@link Semaphore} for politeness. Link discovery and
 * title extraction are handled by jsoup.
 */
public class WebCrawlerService {

    private static final String DEFAULT_USER_AGENT =
            "virtual-thread-webcrawler/1.0 (+https://github.com/Rishav30194/virtual-thread-webcrawler)";

    private final HttpClient httpClient;
    private final Duration requestTimeout;
    private final String userAgent;

    public WebCrawlerService() {
        this(Duration.ofSeconds(10), Duration.ofSeconds(15), DEFAULT_USER_AGENT);
    }

    public WebCrawlerService(Duration connectTimeout, Duration requestTimeout, String userAgent) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.requestTimeout = requestTimeout;
        this.userAgent = userAgent;
    }

    /**
     * Fetches a single URL, parsing its title and outbound links. Never throws —
     * failures are returned as a {@link CrawlResult} with status code 0.
     */
    public CrawlResult fetch(String url, int depth) {
        long start = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(requestTimeout)
                    .header("User-Agent", userAgent)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long elapsed = millisSince(start);

            Document doc = Jsoup.parse(response.body(), response.uri().toString());
            String title = doc.title().isBlank() ? "No title found" : doc.title();
            List<String> links = doc.select("a[href]").stream()
                    .map(e -> e.absUrl("href"))
                    .map(CrawlerUtil::normalize)
                    .filter(s -> s != null)
                    .distinct()
                    .toList();

            return new CrawlResult(url, depth, response.statusCode(),
                    reasonPhrase(response.statusCode()), title, elapsed, links);
        } catch (Exception e) {
            return CrawlResult.failure(url, depth,
                    e.getClass().getSimpleName() + ": " + e.getMessage(), millisSince(start));
        }
    }

    /**
     * Breadth-first crawl from {@code seeds}, following discovered links up to
     * {@code maxDepth} (seeds are depth 0). Each level is fetched concurrently on
     * virtual threads; a thread-safe visited set guarantees every URL is fetched
     * at most once.
     *
     * @param seeds          starting URLs
     * @param maxDepth       how many link-hops to follow (0 = fetch seeds only)
     * @param sameDomainOnly when true, only follow links on the same host as their parent page
     * @param maxConcurrency cap on simultaneous in-flight requests
     * @return every {@link CrawlResult} produced, in completion order
     */
    public List<CrawlResult> crawl(List<String> seeds, int maxDepth, boolean sameDomainOnly, int maxConcurrency) {
        return crawl(seeds, maxDepth, sameDomainOnly, maxConcurrency, Integer.MAX_VALUE);
    }

    /**
     * As {@link #crawl(List, int, boolean, int)}, but stops once {@code maxPages}
     * URLs have been scheduled — a hard bound so a deep crawl of a large site
     * cannot grow without limit.
     */
    public List<CrawlResult> crawl(List<String> seeds, int maxDepth, boolean sameDomainOnly,
                                   int maxConcurrency, int maxPages) {
        Set<String> visited = ConcurrentHashMap.newKeySet();
        List<CrawlResult> results = new CopyOnWriteArrayList<>();
        Semaphore gate = new Semaphore(Math.max(1, maxConcurrency));

        Set<String> currentLevel = new LinkedHashSet<>();
        for (String seed : seeds) {
            String normalized = CrawlerUtil.normalize(seed);
            if (normalized != null) {
                currentLevel.add(normalized);
            }
        }

        int submitted = 0;
        for (int depth = 0; depth <= maxDepth && !currentLevel.isEmpty() && submitted < maxPages; depth++) {
            Set<String> nextLevel = ConcurrentHashMap.newKeySet();
            final int currentDepth = depth;

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (String url : currentLevel) {
                    if (submitted >= maxPages) {
                        break;
                    }
                    if (!visited.add(url)) {
                        continue;
                    }
                    submitted++;
                    executor.submit(() -> {
                        try {
                            gate.acquire();
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        try {
                            CrawlResult result = fetch(url, currentDepth);
                            results.add(result);
                            if (currentDepth < maxDepth) {
                                for (String link : result.links()) {
                                    if (sameDomainOnly && !CrawlerUtil.sameHost(url, link)) {
                                        continue;
                                    }
                                    if (!visited.contains(link)) {
                                        nextLevel.add(link);
                                    }
                                }
                            }
                        } finally {
                            gate.release();
                        }
                    });
                }
            } // try-with-resources awaits all tasks in this level before continuing

            currentLevel = new LinkedHashSet<>(nextLevel);
        }
        return results;
    }

    private static long millisSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private static String reasonPhrase(int code) {
        return switch (code) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 301 -> "Moved Permanently";
            case 302 -> "Found";
            case 304 -> "Not Modified";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 429 -> "Too Many Requests";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            default -> "HTTP " + code;
        };
    }
}
