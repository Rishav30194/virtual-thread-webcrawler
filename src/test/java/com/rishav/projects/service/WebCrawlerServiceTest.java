package com.rishav.projects.service;

import com.rishav.projects.model.CrawlResult;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the crawler against an in-JVM {@link HttpServer} so the tests are
 * deterministic and never touch the real network.
 */
class WebCrawlerServiceTest {

    private static HttpServer server;
    private static String base;
    private static final WebCrawlerService CRAWLER = new WebCrawlerService();

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        // Site graph:  / -> {/a, /b, external}   /a -> {/c}   /b -> {}   /c -> {}
        page(server, "/", "Home", "<a href='/a'>a</a><a href='/b'>b</a><a href='http://external.test/x'>ext</a>");
        page(server, "/a", "Page A", "<a href='/c'>c</a>");
        page(server, "/b", "Page B", "");
        page(server, "/c", "Page C", "");
        server.start();
        base = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    private static void page(HttpServer srv, String path, String title, String body) {
        String html = "<html><head><title>" + title + "</title></head><body>" + body + "</body></html>";
        srv.createContext(path, exchange -> {
            // Only serve the exact path; let everything else 404 (HttpServer default).
            if (!exchange.getRequestURI().getPath().equals(path)) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
    }

    @Test
    void fetchParsesTitleStatusAndLinks() {
        CrawlResult r = CRAWLER.fetch(base + "/", 0);
        assertEquals(200, r.statusCode());
        assertTrue(r.ok());
        assertEquals("Home", r.title());
        assertTrue(r.links().contains(base + "/a"), "should discover /a");
        assertTrue(r.links().contains(base + "/b"), "should discover /b");
    }

    @Test
    void fetchReportsFailureForMissingPageWithoutThrowing() {
        CrawlResult r = CRAWLER.fetch(base + "/missing", 0);
        assertFalse(r.ok());
        assertEquals(404, r.statusCode());
    }

    @Test
    void crawlDepthZeroFetchesOnlySeed() {
        List<CrawlResult> results = CRAWLER.crawl(List.of(base + "/"), 0, true, 10);
        assertEquals(1, results.size());
    }

    @Test
    void crawlDepthOneFollowsOneHopAndStaysOnDomain() {
        List<CrawlResult> results = CRAWLER.crawl(List.of(base + "/"), 1, true, 10);
        Set<String> urls = results.stream().map(CrawlResult::url).collect(Collectors.toSet());
        // root + /a + /b ; /c is two hops away, external host is filtered out.
        assertEquals(3, results.size());
        assertTrue(urls.contains(base + "/a"));
        assertTrue(urls.contains(base + "/b"));
        assertFalse(urls.contains(base + "/c"));
        assertTrue(urls.stream().noneMatch(u -> u.contains("external.test")));
    }

    @Test
    void crawlDepthTwoReachesTransitiveLinkAndVisitsEachOnce() {
        List<CrawlResult> results = CRAWLER.crawl(List.of(base + "/"), 2, true, 10);
        Map<String, Long> counts = results.stream()
                .collect(Collectors.groupingBy(CrawlResult::url, Collectors.counting()));
        assertEquals(4, results.size(), "root + a + b + c");
        assertTrue(counts.containsKey(base + "/c"), "should reach /c via /a at depth 2");
        assertTrue(counts.values().stream().allMatch(c -> c == 1L), "each URL fetched exactly once");
    }
}
