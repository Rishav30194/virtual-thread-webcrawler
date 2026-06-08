package com.rishav.projects;

import com.rishav.projects.model.CrawlResult;
import com.rishav.projects.service.WebCrawlerService;
import com.rishav.projects.util.CrawlerUtil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command-line entry point for the virtual-thread web crawler.
 *
 * <pre>
 *   mvn exec:java -Dexec.args="--depth 1 --concurrency 50"
 *   java -jar target/virtual-thread-webcrawler-1.0-SNAPSHOT.jar --seeds urls.txt --depth 2
 * </pre>
 *
 * Flags (all optional): {@code --seeds <path>}, {@code --depth <n>},
 * {@code --concurrency <n>}, {@code --same-domain <true|false>}, {@code --out <dir>}.
 */
public final class App {

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());

    public static void main(String[] args) {
        Map<String, String> opts = parseArgs(args);
        Path seedsPath = opts.containsKey("seeds") ? Path.of(opts.get("seeds")) : null;
        int depth = intOpt(opts, "depth", 1);
        int concurrency = intOpt(opts, "concurrency", 50);
        int maxPages = intOpt(opts, "max-pages", 500);
        boolean sameDomain = Boolean.parseBoolean(opts.getOrDefault("same-domain", "true"));
        Path outDir = Path.of(opts.getOrDefault("out", "output"));

        List<String> seeds;
        try {
            seeds = CrawlerUtil.readSeeds(seedsPath);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not read seed URLs: {0}", e.getMessage());
            return;
        }
        if (seeds.isEmpty()) {
            LOGGER.warning("No seed URLs to crawl.");
            return;
        }

        LOGGER.log(Level.INFO, "Crawling {0} seed(s) | depth={1} sameDomain={2} concurrency={3} maxPages={4}",
                new Object[]{seeds.size(), depth, sameDomain, concurrency, maxPages});

        WebCrawlerService crawler = new WebCrawlerService();
        long start = System.nanoTime();
        List<CrawlResult> results = crawler.crawl(seeds, depth, sameDomain, concurrency, maxPages);
        long wallMillis = (System.nanoTime() - start) / 1_000_000;

        results.forEach(r -> LOGGER.log(Level.INFO,
                "[d{0}] {1} -> {2} {3} | {4} | {5} ms | {6} links",
                new Object[]{r.depth(), r.url(), r.statusCode(), r.status(), r.title(),
                        r.durationMillis(), r.links().size()}));

        Path csv = writeCsv(results, outDir);
        printSummary(results, wallMillis, csv);
    }

    private static void printSummary(List<CrawlResult> results, long wallMillis, Path csv) {
        long ok = results.stream().filter(CrawlResult::ok).count();
        long failed = results.size() - ok;
        double avgFetch = results.stream().mapToLong(CrawlResult::durationMillis).average().orElse(0);
        LOGGER.log(Level.INFO,
                "Done: {0} pages ({1} ok, {2} failed) in {3} ms wall-clock | avg fetch {4} ms | report: {5}",
                new Object[]{results.size(), ok, failed, wallMillis, Math.round(avgFetch), csv});
    }

    private static Path writeCsv(List<CrawlResult> results, Path outDir) {
        try {
            Files.createDirectories(outDir);
            Path csv = outDir.resolve("results.csv");
            StringBuilder sb = new StringBuilder("url,depth,statusCode,status,durationMillis,linksFound,title\n");
            for (CrawlResult r : results) {
                sb.append(csvField(r.url())).append(',')
                        .append(r.depth()).append(',')
                        .append(r.statusCode()).append(',')
                        .append(csvField(r.status())).append(',')
                        .append(r.durationMillis()).append(',')
                        .append(r.links().size()).append(',')
                        .append(csvField(r.title())).append('\n');
            }
            Files.writeString(csv, sb.toString(), StandardCharsets.UTF_8);
            return csv;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write CSV report", e);
        }
    }

    /** Quotes a CSV field and escapes embedded quotes (RFC 4180). */
    private static String csvField(String value) {
        String v = value == null ? "" : value;
        return '"' + v.replace("\"", "\"\"") + '"';
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> opts = new java.util.HashMap<>();
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].startsWith("--")) {
                opts.put(args[i].substring(2), args[i + 1]);
                i++;
            }
        }
        return opts;
    }

    private static int intOpt(Map<String, String> opts, String key, int fallback) {
        try {
            return opts.containsKey(key) ? Integer.parseInt(opts.get(key)) : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private App() {
    }
}
