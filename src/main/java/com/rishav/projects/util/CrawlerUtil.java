package com.rishav.projects.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Stateless helpers for reading seed URLs and reasoning about URLs.
 */
public final class CrawlerUtil {

    private CrawlerUtil() {
        throw new IllegalStateException("Utility class should not be instantiated");
    }

    /**
     * Reads seed URLs, preferring a file on disk and falling back to the
     * bundled {@code urls.txt} on the classpath. Blank lines and lines starting
     * with {@code #} are ignored.
     *
     * @param path optional path to a newline-delimited URL file; may be {@code null}
     * @return the de-duplicated list of seed URLs, in file order
     */
    public static List<String> readSeeds(Path path) throws IOException {
        if (path != null && Files.isReadable(path)) {
            try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                return parse(reader);
            }
        }
        try (InputStream in = CrawlerUtil.class.getClassLoader().getResourceAsStream("urls.txt")) {
            if (in == null) {
                throw new IOException("No seed file at " + path + " and no urls.txt on the classpath");
            }
            try (var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return parse(reader);
            }
        }
    }

    private static List<String> parse(BufferedReader reader) {
        return reader.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .distinct()
                .toList();
    }

    /**
     * Normalizes a URL for use as a visited-set key: drops the fragment and any
     * trailing slash on the path. Returns {@code null} if the URL is unparseable
     * or not http(s).
     */
    public static String normalize(String url) {
        try {
            URI uri = URI.create(url.trim());
            if (!isHttp(uri)) {
                return null;
            }
            String path = uri.getRawPath();
            if (path != null && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1); // "/" -> "", "/a/" -> "/a"
            }
            StringBuilder sb = new StringBuilder()
                    .append(uri.getScheme()).append("://").append(uri.getRawAuthority());
            if (path != null && !path.isEmpty()) {
                sb.append(path);
            }
            if (uri.getRawQuery() != null) {
                sb.append('?').append(uri.getRawQuery());
            }
            return sb.toString();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** True when both URLs share a registrable host, ignoring a leading {@code www.}. */
    public static boolean sameHost(String a, String b) {
        String ha = host(a);
        String hb = host(b);
        return ha != null && ha.equals(hb);
    }

    private static String host(String url) {
        try {
            String h = URI.create(url).getHost();
            if (h == null) {
                return null;
            }
            h = h.toLowerCase();
            return h.startsWith("www.") ? h.substring(4) : h;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean isHttp(URI uri) {
        String scheme = uri.getScheme();
        return scheme != null
                && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                && uri.getHost() != null;
    }
}
