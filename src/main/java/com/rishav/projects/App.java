package com.rishav.projects;

import com.rishav.projects.model.CrawlResult;
import com.rishav.projects.service.WebCrawlerService;
import com.rishav.projects.util.CrawlerUtil;

import java.io.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hello Programmer!
 */
public class App {
    public static void main(String[] args) {

        Logger logger = Logger.getLogger(App.class.getName());
        logger.log(Level.INFO, "Hello Programmer!");

        WebCrawlerService crawlerService = new WebCrawlerService();

        try (BufferedReader fileReader = new BufferedReader(new FileReader("src/main/resources/urls.txt"))) {

            logger.log(Level.INFO,"File found successfully!");
            List<String> lines = CrawlerUtil.readFiles(fileReader);

            for(String line : lines){
                CrawlResult crawlResult = crawlerService.crawlUrl(line);
                logger.log(Level.INFO, "Crawl Result for URL {0}: {2}: {1}", new Object[]{crawlResult.getUrl(),
                        crawlResult.getTitle(), crawlResult.getStatusCode()});
            }

        }
        catch (FileNotFoundException e) {
            logger.log(Level.SEVERE,"File not found: {0}", e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE,"An unexpected error occurred: {0}", e.getMessage());
        }

    }
}
