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
        HttpGet httpGet = new HttpGet(url);
        try(CloseableHttpClient httpClient = HttpClients.createDefault()) {
            try(CloseableHttpResponse response = httpClient.execute(httpGet)) {
                logger.log(Level.INFO, String.valueOf(response.getCode()));
                logger.log(Level.INFO, response.getReasonPhrase());
                HttpEntity entity = response.getEntity();
                if(entity != null){
                    result.setStatus("Crawled successfully");
                    result.setUrl(url);
                    result.setContent(entity.getContent().toString());
                } else {
                    result.setStatus("No content found for the URL: " + url);
                }
            }
        }catch (IOException e) {
            result.setStatus("Error: " + e.getMessage());
            return result;
        }

        return result;
    }
}
