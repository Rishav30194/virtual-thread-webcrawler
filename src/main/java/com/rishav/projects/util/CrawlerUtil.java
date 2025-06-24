package com.rishav.projects.util;

import java.io.BufferedReader;
import java.util.List;

/**
 * Utility class for web crawler operations.
 * Provides methods to read files and handle common tasks related to crawling.
 */
public class CrawlerUtil {

    private CrawlerUtil() {
        throw new IllegalStateException("Utility class should not be instantiated");
    }

    /**
     * Reads lines from a BufferedReader and returns them as a List of Strings.
     *
     * @param bufferedReader the BufferedReader to read from
     * @return a List of Strings containing the lines read from the BufferedReader
     */
    public static List<String> readFiles(BufferedReader bufferedReader) {
        return bufferedReader.lines().toList();
    }

}
