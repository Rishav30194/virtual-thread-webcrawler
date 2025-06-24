package com.rishav.projects.service;

import com.rishav.projects.model.CrawlResult;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;

import java.io.IOException;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service class for web crawling operations.
 * This class provides methods to crawl URLs and perform related tasks.
 */
public class WebCrawlerService {

    Logger logger = Logger.getLogger(WebCrawlerService.class.getName());
    /**
     * Crawls the specified URL.
     * This method is a placeholder and does not perform any actual crawling.
     *
     * @param url the URL to crawl
     */
    public CrawlResult crawlUrl(String url) {

        CrawlResult result = new CrawlResult();
        result.setUrl(url);
        HttpGet httpGet = new HttpGet(url);
        long startTime = System.nanoTime();

        try(CloseableHttpClient httpClient = HttpClients.createDefault()) {
            try(CloseableHttpResponse response = httpClient.execute(httpGet)) {

                result.setStatusCode(response.getCode());
                result.setStatus(response.getReasonPhrase());

                HttpEntity entity = response.getEntity();
                if(entity != null){
                    String html = new String(entity.getContent().readAllBytes(), "UTF-8");

                    Pattern pattern = Pattern.compile("<title>(.*?)</title>", Pattern.DOTALL);
                    Matcher matcher = pattern.matcher(html);
                    String title = matcher.find()? matcher.group(1).trim() : "No title found";
                    result.setTitle(title); // Simple title extraction

                } else {
                    result.setStatus("No content found for the URL: " + url);
                }
            }
        }catch (IOException e) {
            result.setStatus("Error: " + e.getMessage());
        }

        long endTime = System.nanoTime();
        result.setDurationMillis((endTime-startTime)/1_000_000); // Convert to milliseconds
        logger.log(Level.INFO, "Time take to crawl {0}: {1} ms", new Object[]{url, result.getDurationMillis()});

        return result;
    }
}
