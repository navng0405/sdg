package com.dev.challenge.sdg.service;

import com.algolia.api.SearchClient;
import com.algolia.api.AnalyticsClient;
import com.algolia.model.search.*;
import com.dev.challenge.sdg.model.Product;
import com.dev.challenge.sdg.model.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class AlgoliaService {
    
    private static final Logger log = LoggerFactory.getLogger(AlgoliaService.class);
    
    private final SearchClient searchClient;
    private final AnalyticsClient analyticsClient;
    
    @Value("${algolia.indexes.products}")
    private String productsIndexName;
    
    @Value("${algolia.indexes.user-events}")
    private String userEventsIndexName;
    
    @Value("${algolia.indexes.discount-templates}")
    private String discountTemplatesIndexName;
    
    @Value("${algolia.application-id}")
    private String algoliaAppId;
    
    @Value("${algolia.admin-api-key}")
    private String algoliaAdminKey;
    
    private WebClient webClient;
    
    @Autowired
    public AlgoliaService(SearchClient searchClient, AnalyticsClient analyticsClient) {
        this.searchClient = searchClient;
        this.analyticsClient = analyticsClient;
    }
    
    @Autowired
    public void setWebClientBuilder(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("https://%s-dsn.algolia.net".formatted(algoliaAppId)).build();
    }
    
    public CompletableFuture<Void> storeUserEvent(UserEvent userEvent) {
        log.debug("Storing user event: {}", userEvent);
        
        try {
            // Generate unique ID if not provided
            if (userEvent.getObjectId() == null) {
                userEvent.setObjectId(UUID.randomUUID().toString());
            }
            
            // Set timestamp if not provided
            if (userEvent.getTimestamp() == null) {
                userEvent.setTimestamp(LocalDateTime.now());
            }
            
            // Save the user event using the correct API method
            log.debug("Saving user event to index: {} with data: {}", userEventsIndexName, userEvent);
            var response = searchClient.saveObject(userEventsIndexName, userEvent);
            log.debug("Save response for user event {}: {}", userEvent.getObjectId(), response);
            log.info("Successfully stored user event: {}", userEvent.getObjectId());
            
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to store user event: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Helper method to extract hits from search results, handling Algolia API response format
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> extractHitsFromResult(Object searchResult) {
        if (searchResult == null) {
            log.warn("Search result is null");
            return new ArrayList<>();
        }
        
        try {
            // First, try the standard Algolia SearchResponse.getHits() method
            var getHitsMethod = searchResult.getClass().getMethod("getHits");
            Object hits = getHitsMethod.invoke(searchResult);
            if (hits instanceof List) {
                List<T> result = (List<T>) hits;
                log.debug("Successfully extracted {} hits using getHits() method", result.size());
                return result;
            }
        } catch (Exception e) {
            log.debug("getHits() method not available or failed: {}", e.getMessage());
        }
        
        try {
            // Try alternative method names
            var getItemsMethod = searchResult.getClass().getMethod("getItems");
            Object items = getItemsMethod.invoke(searchResult);
            if (items instanceof List) {
                List<T> result = (List<T>) items;
                log.debug("Successfully extracted {} items using getItems() method", result.size());
                return result;
            }
        } catch (Exception e) {
            log.debug("getItems() method not available or failed: {}", e.getMessage());
        }
        
        // If reflection fails, log the actual response structure for debugging
        log.error("Failed to extract hits from search result. Result type: {}", searchResult.getClass().getName());
        log.error("Available methods:");
        for (var method : searchResult.getClass().getMethods()) {
            if (method.getName().toLowerCase().contains("hit") || 
                method.getName().toLowerCase().contains("item") ||
                method.getName().toLowerCase().contains("result") ||
                method.getName().toLowerCase().contains("get")) {
                log.error("  - {} returns {}", method.getName(), method.getReturnType().getSimpleName());
            }
        }
        
        // Return empty list to prevent null pointer exceptions
        log.warn("Returning empty list due to extraction failure");
        return new ArrayList<>();
    }
    
    @SuppressWarnings("unchecked")
    public CompletableFuture<List<UserEvent>> getUserBehaviorHistory(String userId, int limit) {
        log.debug("Retrieving behavior history for user: {}", userId);
        
        try {
            // Build the search request
            SearchForHits searchForHits = new SearchForHits()
                    .setIndexName(userEventsIndexName)
                    .setQuery("")
                    .setFilters("userId:" + userId)
                    .setHitsPerPage(limit);
            
            SearchMethodParams params = new SearchMethodParams().addRequests(searchForHits);
            
            log.debug("Executing search for user events with params: index={}, userId={}, limit={}", 
                     userEventsIndexName, userId, limit);
            
            // Perform the search with better error handling
            SearchResponses<UserEvent> response = searchClient.search(params, UserEvent.class);
            List<UserEvent> hits = new ArrayList<>();
            
            if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
                var result = response.getResults().get(0);
                log.debug("Search response received, extracting hits from result type: {}", 
                         result.getClass().getSimpleName());
                hits = extractHitsFromResult(result);
            } else {
                log.debug("No search results found for user: {}", userId);
            }
            
            log.info("Retrieved {} behavior events for user: {}", hits.size(), userId);
            return CompletableFuture.completedFuture(hits);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("JSON deserialization error for user {}: {}", userId, e.getMessage(), e);
            return CompletableFuture.completedFuture(Collections.emptyList());
        } catch (Exception e) {
            log.error("Failed to retrieve behavior history for user {}: {}", userId, e.getMessage(), e);
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
    }
    
    public CompletableFuture<Product> getProduct(String productId) {
        log.debug("Retrieving product: {}", productId);
        
        try {
            // Use search with filters to get a specific product by ID
            SearchForHits searchForHits = new SearchForHits()
                    .setIndexName(productsIndexName)
                    .setQuery("")
                    .setFilters("objectID:" + productId)
                    .setHitsPerPage(1);
            
            SearchMethodParams params = new SearchMethodParams().addRequests(searchForHits);
            
            log.debug("Executing product search with params: index={}, productId={}", 
                     productsIndexName, productId);
            
            SearchResponses<Product> response = searchClient.search(params, Product.class);
            
            Product product = null;
            if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
                var result = response.getResults().get(0);
                log.debug("Product search response received, extracting hits from result type: {}", 
                         result.getClass().getSimpleName());
                List<Product> hits = extractHitsFromResult(result);
                if (!hits.isEmpty()) {
                    product = hits.get(0);
                }
            } else {
                log.debug("No search results found for product: {}", productId);
            }
            
            if (product != null) {
                log.info("Retrieved product: {}", product.getName());
            } else {
                log.warn("Product not found: {}", productId);
            }
            return CompletableFuture.completedFuture(product);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("JSON deserialization error for product {}: {}", productId, e.getMessage(), e);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to retrieve product {}: {}", productId, e.getMessage(), e);
            return CompletableFuture.completedFuture(null);
        }
    }
    
    @SuppressWarnings("unchecked")
    public CompletableFuture<List<Product>> searchProducts(String query, int limit) {
        log.debug("Searching products with query: {}", query);
        
        try {
            // Build the search request
            SearchForHits searchForHits = new SearchForHits()
                    .setIndexName(productsIndexName)
                    .setQuery(query)
                    .setHitsPerPage(limit);
            
            SearchMethodParams params = new SearchMethodParams().addRequests(searchForHits);
            
            // Perform the search
            SearchResponses<Product> response = searchClient.search(params, Product.class);
            List<Product> hits = new ArrayList<>();
            
            if (response.getResults() != null && !response.getResults().isEmpty()) {
                var result = response.getResults().get(0);
                hits = extractHitsFromResult(result);
            }
            
            log.info("Found {} products for query: {}", hits.size(), query);
            return CompletableFuture.completedFuture(hits);
        } catch (Exception e) {
            log.error("Failed to search products with query {}: {}", query, e.getMessage(), e);
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
    }
    
    public CompletableFuture<Void> initializeIndexes() {
        log.info("Initializing Algolia indexes");
        
        try {
            // Initialize products index with sample data
            initializeProductsIndex();
            initializeUserEventsIndex();
            initializeDiscountTemplatesIndex();
            
            log.info("All Algolia indexes initialized successfully");
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to initialize Algolia indexes: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    private void initializeProductsIndex() {
        try {
            List<Product> sampleProducts = List.of(
                    Product.builder()
                            .objectId("PROD001")
                            .name("Wireless Bluetooth Headphones")
                            .description("Premium noise-cancelling wireless headphones with 30-hour battery life")
                            .price(new java.math.BigDecimal("199.99"))
                            .category("Electronics")
                            .profitMargin(0.35)
                            .inventoryLevel(50)
                            .imageUrl("https://example.com/headphones.jpg")
                            .averageRating(4.5)
                            .numberOfReviews(1250)
                            .build(),
                    Product.builder()
                            .objectId("PROD002")
                            .name("Running Shoes - Men's")
                            .description("Lightweight running shoes with advanced cushioning technology")
                            .price(new java.math.BigDecimal("129.99"))
                            .category("Sports")
                            .profitMargin(0.40)
                            .inventoryLevel(75)
                            .imageUrl("https://example.com/running-shoes.jpg")
                            .averageRating(4.3)
                            .numberOfReviews(890)
                            .build(),
                    Product.builder()
                            .objectId("PROD003")
                            .name("Smart Watch Series X")
                            .description("Advanced fitness tracking smartwatch with heart rate monitoring")
                            .price(new java.math.BigDecimal("299.99"))
                            .category("Electronics")
                            .profitMargin(0.30)
                            .inventoryLevel(25)
                            .imageUrl("https://example.com/smartwatch.jpg")
                            .averageRating(4.7)
                            .numberOfReviews(2100)
                            .build()
            );
            
            log.info("Attempting to save {} products to index: {}", sampleProducts.size(), productsIndexName);
            
            // Save products one by one using the correct API method
            for (Product product : sampleProducts) {
                try {
                    log.debug("Saving product: {} to index: {}", product.getObjectId(), productsIndexName);
                    var response = searchClient.saveObject(productsIndexName, product);
                    log.debug("Save response for {}: {}", product.getObjectId(), response);
                } catch (Exception saveException) {
                    log.error("Failed to save product {}: {}", product.getObjectId(), saveException.getMessage(), saveException);
                }
            }
            
            // Add a small delay to allow Algolia to process the data
            Thread.sleep(2000);
            
            log.info("Products index initialized with {} sample products", sampleProducts.size());
            
            // Verify data was saved by doing a test search
            verifyProductsIndexData();
            
        } catch (Exception e) {
            log.error("Failed to initialize products index: {}", e.getMessage(), e);
        }
    }
    
    private void initializeUserEventsIndex() {
        try {
            // Set up index settings for user events using IndexSettings object
            com.algolia.model.search.IndexSettings settings = new com.algolia.model.search.IndexSettings()
                    .setSearchableAttributes(List.of("userId", "eventType", "query"))
                    .setAttributesForFaceting(List.of("userId", "eventType", "productId"));
            
            // Set index settings using the correct API method
            try {
                searchClient.setSettings(userEventsIndexName, settings);
            } catch (Exception e) {
                log.warn("Could not set index settings, continuing without custom settings: {}", e.getMessage());
            }
            log.info("User events index settings configured");
        } catch (Exception e) {
            log.error("Failed to initialize user events index: {}", e.getMessage(), e);
        }
    }
    
    private void initializeDiscountTemplatesIndex() {
        try {
            List<Map<String, Object>> templates = List.of(
                    Map.of(
                            "objectID", "TEMPLATE001",
                            "type", "percentage",
                            "base_copy", "Still thinking about this? Get {discount}% off!",
                            "triggers", Map.of("hesitation_level", "high", "user_segment", "new")
                    ),
                    Map.of(
                            "objectID", "TEMPLATE002",
                            "type", "free_shipping",
                            "base_copy", "Free shipping on your order!",
                            "triggers", Map.of("cart_value", "low", "shipping_concern", "true")
                    )
            );
            
            // Save templates one by one using the correct API method
            for (Map<String, Object> template : templates) {
                searchClient.saveObject(discountTemplatesIndexName, template);
            }
            log.info("Discount templates index initialized with {} templates", templates.size());
        } catch (Exception e) {
            log.error("Failed to initialize discount templates index: {}", e.getMessage(), e);
        }
    }
    
    // Additional utility methods from the working example
    public void addProduct(Product product) {
        try {
            searchClient.saveObject(productsIndexName, product);
            log.info("Product added to Algolia index: {}", product.getObjectId());
        } catch (Exception e) {
            log.error("Failed to add product to Algolia: {}", e.getMessage(), e);
        }
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getTopSearchQueries(int limit) {
        try {
            var response = analyticsClient.getTopSearches(productsIndexName);
            if (response != null) {
                // Try to extract queries from the response
                var method = response.getClass().getMethod("getQueries");
                List<String> queries = (List<String>) method.invoke(response);
                if (queries != null && !queries.isEmpty()) {
                    return queries.subList(0, Math.min(limit, queries.size()));
                }
            }
            log.warn("No top search queries found in Algolia analytics response.");
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch top search queries from Algolia analytics: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    private void verifyProductsIndexData() {
        try {
            log.info("Verifying products index data...");
            
            // Try to search for all products
            SearchForHits searchForHits = new SearchForHits()
                    .setIndexName(productsIndexName)
                    .setQuery("") // Empty query to get all records
                    .setHitsPerPage(10);
            
            SearchMethodParams params = new SearchMethodParams().addRequests(searchForHits);
            SearchResponses<Product> response = searchClient.search(params, Product.class);
            
            if (response.getResults() != null && !response.getResults().isEmpty()) {
                var result = response.getResults().get(0);
                List<Product> hits = extractHitsFromResult(result);
                log.info("Verification: Found {} products in index", hits.size());
                
                for (Product product : hits) {
                    log.debug("Found product: {} - {}", product.getObjectId(), product.getName());
                }
            } else {
                log.warn("Verification: No products found in index - this indicates a data storage issue");
            }
            
        } catch (Exception e) {
            log.error("Failed to verify products index data: {}", e.getMessage(), e);
        }
    }
}
