package com.dev.challenge.mcp.service;

import com.algolia.api.SearchClient;
import com.algolia.api.AnalyticsClient;
import com.algolia.model.search.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlgoliaService {
    
    private static final Logger log = LoggerFactory.getLogger(AlgoliaService.class);
    
    private final SearchClient searchClient;
    private final AnalyticsClient analyticsClient;
    
    @Value("${algolia.indexes.products}")
    private String productsIndexName;
    
    @Value("${algolia.indexes.user-events}")
    private String userEventsIndexName;
    
    @Value("${algolia.indexes.conversions}")
    private String conversionsIndexName;
    
    /**
     * Retrieves user behavior history from Algolia user_events index
     */
    public CompletableFuture<List<Map<String, Object>>> getUserBehaviorHistory(String userId, int limit) {
        log.debug("Retrieving behavior history for user: {}", userId);
        
        try {
            SearchForHits searchForHits = new SearchForHits()
                    .setIndexName(userEventsIndexName)
                    .setQuery("")
                    .setFilters("userId:" + userId)
                    .setHitsPerPage(limit);
            
            SearchMethodParams params = new SearchMethodParams().addRequests(searchForHits);
            SearchResponses<Map> response = searchClient.search(params, Map.class);
            
            List<Map<String, Object>> hits = new ArrayList<>();
            if (response.getResults() != null && !response.getResults().isEmpty()) {
                var result = response.getResults().get(0);
                hits = extractHitsFromResult(result);
            }
            
            log.info("Retrieved {} behavior events for user: {}", hits.size(), userId);
            return CompletableFuture.completedFuture(hits);
            
        } catch (Exception e) {
            log.error("Failed to retrieve behavior history for user {}: {}", userId, e.getMessage(), e);
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
    }
    
    /**
     * Retrieves product data from Algolia products index
     */
    public CompletableFuture<Map<String, Object>> getProduct(String productId) {
        log.debug("Retrieving product: {}", productId);
        
        try {
            SearchForHits searchForHits = new SearchForHits()
                    .setIndexName(productsIndexName)
                    .setQuery("")
                    .setFilters("objectID:" + productId)
                    .setHitsPerPage(1);
            
            SearchMethodParams params = new SearchMethodParams().addRequests(searchForHits);
            SearchResponses<Map> response = searchClient.search(params, Map.class);
            
            if (response.getResults() != null && !response.getResults().isEmpty()) {
                var result = response.getResults().get(0);
                List<Map<String, Object>> hits = extractHitsFromResult(result);
                
                if (!hits.isEmpty()) {
                    Map<String, Object> product = hits.get(0);
                    log.info("Retrieved product: {}", product.get("name"));
                    return CompletableFuture.completedFuture(product);
                }
            }
            
            log.warn("Product not found: {}", productId);
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            log.error("Failed to retrieve product {}: {}", productId, e.getMessage(), e);
            return CompletableFuture.completedFuture(null);
        }
    }
    
    /**
     * Stores conversion event in Algolia for analytics
     */
    public void storeConversionEvent(Map<String, Object> conversionLog) {
        try {
            // Generate unique ID for the conversion event
            String eventId = "conv-" + UUID.randomUUID().toString();
            conversionLog.put("objectID", eventId);
            
            log.debug("Storing conversion event: {}", eventId);
            searchClient.saveObject(conversionsIndexName, conversionLog);
            log.info("Successfully stored conversion event: {}", eventId);
            
        } catch (Exception e) {
            log.error("Failed to store conversion event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store conversion event: " + e.getMessage());
        }
    }
    
    /**
     * Stores user event in Algolia
     */
    public CompletableFuture<Void> storeUserEvent(Map<String, Object> userEvent) {
        try {
            // Generate unique ID if not provided
            if (!userEvent.containsKey("objectID")) {
                userEvent.put("objectID", UUID.randomUUID().toString());
            }
            
            // Set timestamp if not provided
            if (!userEvent.containsKey("timestamp")) {
                userEvent.put("timestamp", LocalDateTime.now().toString());
            }
            
            log.debug("Storing user event: {}", userEvent.get("objectID"));
            searchClient.saveObject(userEventsIndexName, userEvent);
            log.info("Successfully stored user event: {}", userEvent.get("objectID"));
            
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            log.error("Failed to store user event: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Helper method to extract hits from search results, handling different API versions
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> extractHitsFromResult(Object searchResult) {
        try {
            // Try getHits() first (common method name)
            var getHitsMethod = searchResult.getClass().getMethod("getHits");
            Object hits = getHitsMethod.invoke(searchResult);
            if (hits instanceof List) {
                return (List<T>) hits;
            }
        } catch (Exception e) {
            // Method not found or failed, try alternatives
        }
        
        try {
            // Try getItems() (alternative method name)
            var getItemsMethod = searchResult.getClass().getMethod("getItems");
            Object items = getItemsMethod.invoke(searchResult);
            if (items instanceof List) {
                return (List<T>) items;
            }
        } catch (Exception e) {
            // Method not found or failed
        }
        
        log.warn("Could not extract hits from search result. Available methods:");
        for (var method : searchResult.getClass().getMethods()) {
            if (method.getName().toLowerCase().contains("hit") || 
                method.getName().toLowerCase().contains("item") ||
                method.getName().toLowerCase().contains("result")) {
                log.warn("  - {}", method.getName());
            }
        }
        
        return Collections.emptyList();
    }
}
