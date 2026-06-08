package com.rishav.projects.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrawlerUtilTest {

    @Test
    void normalizeStripsTrailingSlashAndFragment() {
        assertEquals("https://example.com/a", CrawlerUtil.normalize("https://example.com/a/"));
        assertEquals("https://example.com/page", CrawlerUtil.normalize("https://example.com/page#section"));
        assertEquals("https://example.com/s?q=1", CrawlerUtil.normalize("https://example.com/s?q=1"));
    }

    @Test
    void normalizeCollapsesRootVariants() {
        String canonical = "https://example.com";
        assertEquals(canonical, CrawlerUtil.normalize("https://example.com"));
        assertEquals(canonical, CrawlerUtil.normalize("https://example.com/"));
        assertEquals(canonical, CrawlerUtil.normalize("https://example.com/#top"));
    }

    @Test
    void normalizeRejectsNonHttpUrls() {
        assertNull(CrawlerUtil.normalize("mailto:hi@example.com"));
        assertNull(CrawlerUtil.normalize("javascript:void(0)"));
        assertNull(CrawlerUtil.normalize("ftp://example.com/file"));
        assertNull(CrawlerUtil.normalize("not a url"));
    }

    @Test
    void sameHostIgnoresWwwPrefix() {
        assertTrue(CrawlerUtil.sameHost("https://www.example.com/a", "https://example.com/b"));
        assertTrue(CrawlerUtil.sameHost("http://example.com", "https://example.com/deep/path"));
        assertFalse(CrawlerUtil.sameHost("https://example.com", "https://other.com"));
    }

    @Test
    void readSeedsFallsBackToClasspathUrlsFile() throws IOException {
        List<String> seeds = CrawlerUtil.readSeeds(null);
        assertFalse(seeds.isEmpty(), "bundled urls.txt should provide seeds");
        assertTrue(seeds.stream().allMatch(s -> s.startsWith("http")), "all seeds should be http(s)");
    }
}
