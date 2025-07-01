package com.rishav.projects;

import com.rishav.projects.model.CrawlResult;
import com.rishav.projects.service.WebCrawlerService;
import com.rishav.projects.util.CrawlerUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hello Programmer!
 */
public class App {
    public static void main(String[] args) {


        Logger logger = Logger.getLogger(App.class.getName());
        logger.log(Level.INFO, "Hello Programmer!");
        long startTime = System.nanoTime();

        WebCrawlerService crawlerService = new WebCrawlerService();

        try (BufferedReader fileReader = new BufferedReader(new FileReader("src/main/resources/urls.txt"))) {

            logger.log(Level.INFO,"File found successfully!");
            List<String> lines = CrawlerUtil.readFiles(fileReader);

            // Use virtual threads for better scalability (Java 21+)
            try(ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {

                List<Future<CrawlResult>> futures = new ArrayList<>();

                for (String line : lines) {
                    futures.add(executorService.submit(() -> crawlerService.crawlUrl(line)));
                }

                for (Future<CrawlResult> future : futures) {
                    CrawlResult result = future.get();
                    if (result != null) {
                        logger.log(Level.INFO, "Crawled URL: {0}, Title: {1}, Status Code: {2}, Status: {3}, Duration: {4} ms",
                                new Object[]{result.getUrl(), result.getTitle(), result.getStatusCode(), result.getStatus(), result.getDurationMillis()});
                    } else {
                        logger.log(Level.WARNING, "CrawlResult is null for one of the URLs.");
                    }
                }
                long endTime = System.nanoTime();
                logger.log(Level.INFO, "Total time taken to crawl all URLs: {0} ms", (endTime - startTime) / 1_000_000);
            }catch (Exception e){
                logger.log(Level.SEVERE, "Error during crawling: {0}", e.getMessage());
            }

        }
        catch (FileNotFoundException e) {
            logger.log(Level.SEVERE,"File not found: {0}", e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE,"An unexpected error occurred: {0}", e.getMessage());
        }

    }
}
