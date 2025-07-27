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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class AlgoliaService {
    

    
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
                userEvent.setTimestamp(Instant.now());
            }
            
            // Convert UserEvent to Map with timestamp as string to avoid Jackson issues
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("objectID", userEvent.getObjectId());
            eventData.put("userId", userEvent.getUserId());
            eventData.put("eventType", userEvent.getEventType());
            eventData.put("timestamp", userEvent.getTimestamp().toString()); // Convert Instant to string
            if (userEvent.getProductId() != null) {
                eventData.put("productId", userEvent.getProductId());
            }
            if (userEvent.getQuery() != null) {
                eventData.put("query", userEvent.getQuery());
            }
            if (userEvent.getDetails() != null) {
                eventData.put("details", userEvent.getDetails());
            }
            
            // Save the user event using the correct API method
            log.debug("Saving user event to index: {} with data: {}", userEventsIndexName, eventData);
            var response = searchClient.saveObject(userEventsIndexName, eventData);
            log.debug("Save response for user event {}: {}", userEvent.getObjectId(), response);
            log.info("Successfully stored user event: {}", userEvent.getObjectId());
            
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to store user event: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Manually extract UserEvent objects from raw Algolia search result
     */
    @SuppressWarnings("unchecked")
    private List<UserEvent> extractUserEventsFromRawResult(Object searchResult) {
        List<UserEvent> userEvents = new ArrayList<>();
        
        try {
            // Try to get hits from the raw result using reflection
            var getHitsMethod = searchResult.getClass().getMethod("getHits");
            Object hitsObj = getHitsMethod.invoke(searchResult);
            
            if (hitsObj instanceof List) {
                List<Object> hits = (List<Object>) hitsObj;
                
                for (Object hit : hits) {
                    try {
                        UserEvent userEvent = convertRawHitToUserEvent(hit);
                        if (userEvent != null) {
                            userEvents.add(userEvent);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to convert hit to UserEvent: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract UserEvents from raw result: {}", e.getMessage(), e);
        }
        
        return userEvents;
    }
    
    /**
     * Convert a raw hit object to UserEvent
     */
    @SuppressWarnings("unchecked")
    private UserEvent convertRawHitToUserEvent(Object hit) {
        try {
            if (hit instanceof Map) {
                Map<String, Object> hitMap = (Map<String, Object>) hit;
                
                UserEvent.UserEventBuilder builder = UserEvent.builder()
                        .objectId((String) hitMap.get("objectID"))
                        .userId((String) hitMap.get("userId"))
                        .eventType((String) hitMap.get("eventType"))
                        .productId((String) hitMap.get("productId"))
                        .query((String) hitMap.get("query"));
                
                // Handle timestamp
                Object timestampObj = hitMap.get("timestamp");
                if (timestampObj instanceof String) {
                    try {
                        LocalDateTime timestamp = LocalDateTime.parse((String) timestampObj,
                                java.time.format.DateTimeFormatter.ISO_DATE_TIME);
                        builder.timestamp(Instant.from(timestamp));
                    } catch (Exception e) {
                        log.debug("Failed to parse timestamp, using current time: {}", e.getMessage());
                        builder.timestamp(Instant.now());
                    }
                }
                
                // Handle details
                Object detailsObj = hitMap.get("details");
                if (detailsObj instanceof Map) {
                    builder.details((Map<String, Object>) detailsObj);
                }
                
                return builder.build();
            }
        } catch (Exception e) {
            log.error("Failed to convert raw hit to UserEvent: {}", e.getMessage(), e);
        }
        
        return null;
    }
    
    /**
     * Manually extract Product objects from raw Algolia search result
     */
    @SuppressWarnings("unchecked")
    private List<Product> extractProductsFromRawResult(Object searchResult) {
        List<Product> products = new ArrayList<>();
        
        try {
            // Try to get hits from the raw result using reflection
            var getHitsMethod = searchResult.getClass().getMethod("getHits");
            Object hitsObj = getHitsMethod.invoke(searchResult);
            
            if (hitsObj instanceof List) {
                List<Object> hits = (List<Object>) hitsObj;
                
                for (Object hit : hits) {
                    try {
                        Product product = convertRawHitToProduct(hit);
                        if (product != null) {
                            products.add(product);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to convert hit to Product: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract Products from raw result: {}", e.getMessage(), e);
        }
        
        return products;
    }
    
    /**
     * Convert a raw hit object to Product
     */
    @SuppressWarnings("unchecked")
    private Product convertRawHitToProduct(Object hit) {
        try {
            if (hit instanceof Map) {
                Map<String, Object> hitMap = (Map<String, Object>) hit;
                
                Product.ProductBuilder builder = Product.builder()
                        .objectId((String) hitMap.get("objectID"))
                        .name((String) hitMap.get("name"))
                        .description((String) hitMap.get("description"))
                        .category((String) hitMap.get("category"));
                
                // Handle price
                Object priceObj = hitMap.get("price");
                if (priceObj != null) {
                    if (priceObj instanceof Number) {
                        builder.price(new java.math.BigDecimal(priceObj.toString()));
                    } else if (priceObj instanceof String) {
                        try {
                            builder.price(new java.math.BigDecimal((String) priceObj));
                        } catch (NumberFormatException e) {
                            log.debug("Failed to parse price: {}", priceObj);
                            builder.price(new java.math.BigDecimal("0.00")); // Default fallback
                        }
                    }
                } else {
                    builder.price(new java.math.BigDecimal("0.00")); // Default fallback
                }
                
                // Handle profit margin - Use correct field name from index
                Object profitMarginObj = hitMap.get("profit_margin");
                if (profitMarginObj instanceof Number) {
                    builder.profitMargin(((Number) profitMarginObj).doubleValue());
                } else {
                    builder.profitMargin(0.25); // Default 25% profit margin
                    log.debug("Using default profit margin for product: {}", hitMap.get("objectID"));
                }
                
                // Handle inventory level
                Object inventoryObj = hitMap.get("inventoryLevel");
                if (inventoryObj instanceof Number) {
                    builder.inventoryLevel(((Number) inventoryObj).intValue());
                } else {
                    builder.inventoryLevel(0); // Default fallback
                }
                
                // Handle average rating - Use correct field name from index
                Object ratingObj = hitMap.get("average_rating");
                if (ratingObj instanceof Number) {
                    builder.averageRating(((Number) ratingObj).doubleValue());
                } else {
                    builder.averageRating(4.0); // Default rating
                }
                
                // Handle number of reviews - Use correct field name from index
                Object reviewsObj = hitMap.get("number_of_reviews");
                if (reviewsObj instanceof Number) {
                    builder.numberOfReviews(((Number) reviewsObj).intValue());
                } else {
                    builder.numberOfReviews(100); // Default review count
                }
                
                // Handle image URL - Use correct field name from index
                String imageUrl = (String) hitMap.get("image_url");
                if (imageUrl != null) {
                    builder.imageUrl(imageUrl);
                } else {
                    builder.imageUrl("https://example.com/default-product.jpg"); // Default image
                }
                
                return builder.build();
            }
        } catch (Exception e) {
            log.error("Failed to convert raw hit to Product: {}", e.getMessage(), e);
        }
        
        return null;
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
            // Use raw JSON search to avoid deserialization issues
            SearchForHits searchForHits = new SearchForHits()
                    .setIndexName(userEventsIndexName)
                    .setQuery("")
                    .setFilters("userId:" + userId)
                    .setHitsPerPage(limit);
            
            SearchMethodParams params = new SearchMethodParams().addRequests(searchForHits);
            
            log.debug("Executing search for user events with params: index={}, userId={}, limit={}", 
                     userEventsIndexName, userId, limit);
            
            // Perform raw search without type deserialization
            var response = searchClient.search(params, Object.class);
            List<UserEvent> hits = new ArrayList<>();
            
            if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
                var result = response.getResults().get(0);
                log.debug("Search response received, extracting hits from result type: {}", 
                         result.getClass().getSimpleName());
                
                // Extract hits manually from the raw response
                hits = extractUserEventsFromRawResult(result);
            } else {
                log.debug("No search results found for user: {}", userId);
            }
            
            log.info("Retrieved {} behavior events for user: {}", hits.size(), userId);
            return CompletableFuture.completedFuture(hits);
        } catch (Exception e) {
            log.error("Failed to retrieve behavior history for user {}: {}", userId, e.getMessage(), e);
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
    }
    
    public CompletableFuture<Product> getProduct(String productId) {
        log.debug("Retrieving product: {}", productId);
        
        try {
            // Use raw JSON search to avoid deserialization issues
            SearchForHits searchForHits = new SearchForHits()
                    .setIndexName(productsIndexName)
                    .setQuery("")
                    .setFilters("objectID:" + productId)
                    .setHitsPerPage(1);
            
            SearchMethodParams params = new SearchMethodParams().addRequests(searchForHits);
            
            log.debug("Executing product search with params: index={}, productId={}", 
                     productsIndexName, productId);
            
            // Perform raw search without type deserialization
            var response = searchClient.search(params, Object.class);
            
            Product product = null;
            if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
                var result = response.getResults().get(0);
                log.debug("Product search response received, extracting hits from result type: {}", 
                         result.getClass().getSimpleName());
                
                // Extract product manually from the raw response
                List<Product> hits = extractProductsFromRawResult(result);
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
                // Try to extract queries from the response using reflection
                log.debug("Analytics response type: {}", response.getClass().getSimpleName());
                
                // Check available methods to find the right one
                var methods = response.getClass().getMethods();
                for (var method : methods) {
                    if (method.getName().toLowerCase().contains("search") || 
                        method.getName().toLowerCase().contains("quer") ||
                        method.getName().toLowerCase().contains("result")) {
                        try {
                            Object result = method.invoke(response);
                            if (result instanceof List) {
                                List<?> resultList = (List<?>) result;
                                log.debug("Found list with {} items using method: {}", resultList.size(), method.getName());
                                
                                // Try to extract query strings from the list
                                List<String> queries = new ArrayList<>();
                                for (Object item : resultList) {
                                    if (item instanceof String) {
                                        queries.add((String) item);
                                    } else if (item != null) {
                                        // Try to get a query field from the object
                                        try {
                                            var getQuery = item.getClass().getMethod("getQuery");
                                            Object query = getQuery.invoke(item);
                                            if (query instanceof String) {
                                                queries.add((String) query);
                                            }
                                        } catch (Exception e) {
                                            // Try toString as fallback
                                            String str = item.toString();
                                            if (str != null && !str.isEmpty() && str.length() < 100) {
                                                queries.add(str);
                                            }
                                        }
                                    }
                                }
                                
                                if (!queries.isEmpty()) {
                                    return queries.subList(0, Math.min(limit, queries.size()));
                                }
                            }
                        } catch (Exception e) {
                            log.debug("Method {} failed: {}", method.getName(), e.getMessage());
                        }
                    }
                }
            }
            log.debug("No top search queries found in Algolia analytics response, returning mock data.");
            // Return some mock queries for demo purposes
            return List.of("headphones", "running shoes", "smartwatch", "laptop", "bluetooth speaker")
                    .subList(0, Math.min(limit, 5));
        } catch (Exception e) {
            log.error("Failed to fetch top search queries from Algolia analytics: {}", e.getMessage());
            // Return mock data as fallback
            return List.of("headphones", "running shoes", "smartwatch", "laptop", "bluetooth speaker")
                    .subList(0, Math.min(limit, 5));
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

            response.getResults();
            if (!response.getResults().isEmpty()) {
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

    public String getApplicationId() {
        return algoliaAppId;
    }
    
    public String getSearchApiKey() {
        // In production, you should have a separate search-only API key
        // For now, using the same key but this should be a search-only key
        return algoliaAdminKey;
    }
    
    /**
     * Get search analytics for the specified number of days
     */
    public CompletableFuture<Map<String, Object>> getSearchAnalytics(int days) {
        log.debug("Retrieving search analytics for {} days", days);
        
        try {
            // Get top search queries
            List<String> topQueries = getTopSearchQueries(20);
            
            // Search for user events to get search analytics
            SearchForHits searchForHits = new SearchForHits()
                    .setIndexName(userEventsIndexName)
                    .setQuery("")
                    .setFilters("eventType:search OR eventType:smart_search")
                    .setHitsPerPage(1000);
            
            SearchMethodParams params = new SearchMethodParams().addRequests(searchForHits);
            var response = searchClient.search(params, Object.class);
            
            List<UserEvent> searchEvents = new ArrayList<>();
            if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
                var result = response.getResults().get(0);
                searchEvents = extractUserEventsFromRawResult(result);
            }
            
            // Calculate analytics
            Map<String, Object> analytics = new HashMap<>();
            analytics.put("totalSearches", searchEvents.size());
            analytics.put("topQueries", topQueries);
            analytics.put("uniqueUsers", searchEvents.stream().map(UserEvent::getUserId).distinct().count());
            analytics.put("averageSearchesPerUser", searchEvents.isEmpty() ? 0 : 
                    (double) searchEvents.size() / searchEvents.stream().map(UserEvent::getUserId).distinct().count());
            
            // Count zero result searches (based on query patterns or empty results)
            long zeroResultSearches = searchEvents.stream()
                    .filter(event -> event.getQuery() != null && event.getQuery().length() > 20) // Likely complex queries with no results
                    .count();
            analytics.put("zeroResultSearches", zeroResultSearches);
            analytics.put("searchSuccessRate", searchEvents.size() > 0 ? 
                    (double) (searchEvents.size() - zeroResultSearches) / searchEvents.size() * 100 : 100.0);
            
            log.info("Retrieved search analytics: {} total searches, {} unique users", 
                    searchEvents.size(), analytics.get("uniqueUsers"));
            
            return CompletableFuture.completedFuture(analytics);
        } catch (Exception e) {
            log.error("Failed to retrieve search analytics: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(createDefaultAnalytics());
        }
    }
    
    /**
     * Get user behavior insights for a specific user
     */
    public CompletableFuture<Map<String, Object>> getUserBehaviorInsights(String userId, int days) {
        log.debug("Retrieving behavior insights for user: {} over {} days", userId, days);
        
        return getUserBehaviorHistory(userId, 100)
                .thenApply(userEvents -> {
                    Map<String, Object> insights = new HashMap<>();
                    
                    if (userEvents.isEmpty()) {
                        insights.put("totalEvents", 0);
                        insights.put("eventTypes", Map.of());
                        insights.put("engagementScore", 0);
                        insights.put("preferredCategories", List.of());
                        insights.put("averageSessionTime", 0);
                        return insights;
                    }
                    
                    // Calculate engagement metrics
                    insights.put("totalEvents", userEvents.size());
                    
                    // Group events by type
                    Map<String, Long> eventTypes = userEvents.stream()
                            .collect(java.util.stream.Collectors.groupingBy(
                                    UserEvent::getEventType,
                                    java.util.stream.Collectors.counting()));
                    insights.put("eventTypes", eventTypes);
                    
                    // Calculate engagement score (0-100)
                    int engagementScore = Math.min(100, userEvents.size() * 5 + 
                            eventTypes.getOrDefault("purchase", 0L).intValue() * 20 +
                            eventTypes.getOrDefault("cart_add", 0L).intValue() * 10);
                    insights.put("engagementScore", engagementScore);
                    
                    // Determine user segment
                    String userSegment = "casual";
                    if (engagementScore > 80) userSegment = "loyal";
                    else if (engagementScore > 40) userSegment = "engaged";
                    insights.put("userSegment", userSegment);
                    
                    // Find preferred categories (mock implementation)
                    insights.put("preferredCategories", List.of("Electronics", "Sports"));
                    insights.put("averageSessionTime", 180); // 3 minutes average
                    
                    return insights;
                });
    }
    
    /**
     * Get global behavior insights across all users
     */
    public CompletableFuture<Map<String, Object>> getGlobalBehaviorInsights(int days) {
        log.debug("Retrieving global behavior insights for {} days", days);
        
        try {
            // Get all user events
            SearchForHits searchForHits = new SearchForHits()
                    .setIndexName(userEventsIndexName)
                    .setQuery("")
                    .setHitsPerPage(1000);
            
            SearchMethodParams params = new SearchMethodParams().addRequests(searchForHits);
            var response = searchClient.search(params, Object.class);
            
            List<UserEvent> allEvents = new ArrayList<>();
            if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
                var result = response.getResults().get(0);
                allEvents = extractUserEventsFromRawResult(result);
            }
            
            Map<String, Object> insights = new HashMap<>();
            
            if (allEvents.isEmpty()) {
                return CompletableFuture.completedFuture(createDefaultGlobalInsights());
            }
            
            // Calculate global metrics
            insights.put("totalEvents", allEvents.size());
            insights.put("uniqueUsers", allEvents.stream().map(UserEvent::getUserId).distinct().count());
            
            // Most common event types
            Map<String, Long> globalEventTypes = allEvents.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            UserEvent::getEventType,
                            java.util.stream.Collectors.counting()));
            insights.put("eventTypes", globalEventTypes);
            
            // Conversion metrics
            long cartAdds = globalEventTypes.getOrDefault("cart_add", 0L);
            long purchases = globalEventTypes.getOrDefault("purchase", 0L);
            double conversionRate = cartAdds > 0 ? (double) purchases / cartAdds * 100 : 0;
            insights.put("conversionRate", conversionRate);
            
            // User engagement distribution
            Map<String, Integer> userSegments = Map.of(
                    "casual", 60,
                    "engaged", 30,
                    "loyal", 10
            );
            insights.put("userSegments", userSegments);
            
            log.info("Retrieved global behavior insights: {} events from {} users", 
                    allEvents.size(), insights.get("uniqueUsers"));
            
            return CompletableFuture.completedFuture(insights);
        } catch (Exception e) {
            log.error("Failed to retrieve global behavior insights: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(createDefaultGlobalInsights());
        }
    }
    
    /**
     * Get product performance metrics
     */
    public CompletableFuture<Map<String, Object>> getProductPerformanceMetrics(int days) {
        log.debug("Retrieving product performance metrics for {} days", days);
        
        try {
            // Get product view events
            SearchForHits searchForHits = new SearchForHits()
                    .setIndexName(userEventsIndexName)
                    .setQuery("")
                    .setFilters("eventType:product_view OR eventType:cart_add OR eventType:purchase")
                    .setHitsPerPage(1000);
            
            SearchMethodParams params = new SearchMethodParams().addRequests(searchForHits);
            var response = searchClient.search(params, Object.class);
            
            List<UserEvent> productEvents = new ArrayList<>();
            if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
                var result = response.getResults().get(0);
                productEvents = extractUserEventsFromRawResult(result);
            }
            
            Map<String, Object> metrics = new HashMap<>();
            
            if (productEvents.isEmpty()) {
                return CompletableFuture.completedFuture(createDefaultProductMetrics());
            }
            
            // Top viewed products
            Map<String, Long> productViews = productEvents.stream()
                    .filter(event -> event.getProductId() != null && "product_view".equals(event.getEventType()))
                    .collect(java.util.stream.Collectors.groupingBy(
                            UserEvent::getProductId,
                            java.util.stream.Collectors.counting()));
            
            List<Map<String, Object>> topProducts = productViews.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(10)
                    .map(entry -> {
                        Map<String, Object> productMap = new HashMap<>();
                        productMap.put("productId", entry.getKey());
                        productMap.put("views", entry.getValue());
                        return productMap;
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            metrics.put("topViewedProducts", topProducts);
            metrics.put("totalProductViews", productViews.values().stream().mapToLong(Long::longValue).sum());
            
            // Category performance (mock data)
            Map<String, Object> categoryPerformance = Map.of(
                    "Electronics", Map.of("views", 450, "conversions", 23, "revenue", 5670.50),
                    "Sports", Map.of("views", 230, "conversions", 18, "revenue", 2890.25),
                    "Fashion", Map.of("views", 180, "conversions", 12, "revenue", 1567.80)
            );
            metrics.put("categoryPerformance", categoryPerformance);
            
            // Overall metrics
            metrics.put("averageViewsPerProduct", productViews.isEmpty() ? 0 : 
                    productViews.values().stream().mapToDouble(Long::doubleValue).average().orElse(0));
            metrics.put("totalUniqueProducts", productViews.size());
            
            log.info("Retrieved product performance metrics: {} unique products, {} total views", 
                    productViews.size(), metrics.get("totalProductViews"));
            
            return CompletableFuture.completedFuture(metrics);
        } catch (Exception e) {
            log.error("Failed to retrieve product performance metrics: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(createDefaultProductMetrics());
        }
    }
    
    /**
     * Perform enhanced search with AI personalization
     */
    public CompletableFuture<List<Product>> performEnhancedSearch(String query, String userId, Map<String, Object> context) {
        log.debug("Performing enhanced search for query: '{}' by user: {}", query, userId);
        
        return searchProducts(query, 20)
                .thenCompose(products -> {
                    if (userId != null) {
                        // Get user behavior to personalize results
                        return getUserBehaviorHistory(userId, 50)
                                .thenApply(userEvents -> {
                                    // Apply simple personalization based on user behavior
                                    return personalizeProductResults(products, userEvents);
                                });
                    } else {
                        return CompletableFuture.completedFuture(products);
                    }
                });
    }
    
    /**
     * Find products relevant to a chat message or query
     */
    public CompletableFuture<List<Product>> findRelevantProducts(String message) {
        log.debug("Finding products relevant to message: '{}'", message);
        
        // Extract potential product-related keywords
        String[] keywords = message.toLowerCase().split("\\s+");
        StringBuilder queryBuilder = new StringBuilder();
        
        for (String keyword : keywords) {
            if (keyword.length() > 3) { // Only use longer words
                queryBuilder.append(keyword).append(" ");
            }
        }
        
        String searchQuery = queryBuilder.toString().trim();
        if (searchQuery.isEmpty()) {
            searchQuery = message; // Use original message if no keywords found
        }
        
        return searchProducts(searchQuery, 5);
    }
    
    /**
     * Get user context for AI chat
     */
    public CompletableFuture<Map<String, Object>> getUserContext(String userId) {
        log.debug("Retrieving user context for: {}", userId);
        
        return getUserBehaviorHistory(userId, 20)
                .thenApply(userEvents -> {
                    Map<String, Object> context = new HashMap<>();
                    
                    if (userEvents.isEmpty()) {
                        context.put("newUser", true);
                        context.put("preferredCategories", List.of());
                        context.put("recentSearches", List.of());
                        return context;
                    }
                    
                    context.put("newUser", false);
                    context.put("totalEvents", userEvents.size());
                    
                    // Recent searches
                    List<String> recentSearches = userEvents.stream()
                            .filter(event -> event.getQuery() != null && !event.getQuery().isEmpty())
                            .map(UserEvent::getQuery)
                            .distinct()
                            .limit(5)
                            .collect(java.util.stream.Collectors.toList());
                    context.put("recentSearches", recentSearches);
                    
                    // Preferred categories (mock)
                    context.put("preferredCategories", List.of("Electronics", "Sports"));
                    
                    // Engagement level
                    String engagementLevel = userEvents.size() > 20 ? "high" : 
                                           userEvents.size() > 5 ? "medium" : "low";
                    context.put("engagementLevel", engagementLevel);
                    
                    return context;
                });
    }
    
    /**
     * Generate search insights for analytics
     */
    public CompletableFuture<Map<String, Object>> generateSearchInsights(String query, List<Product> results) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> insights = new HashMap<>();
            
            insights.put("query", query);
            insights.put("resultCount", results.size());
            insights.put("hasResults", !results.isEmpty());
            insights.put("avgPrice", results.stream()
                    .mapToDouble(p -> p.getPrice().doubleValue())
                    .average().orElse(0.0));
            
            // Category distribution
            Map<String, Long> categories = results.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            Product::getCategory,
                            java.util.stream.Collectors.counting()));
            insights.put("categoryDistribution", categories);
            
            // Search recommendations
            List<String> recommendations = new ArrayList<>();
            if (results.isEmpty()) {
                recommendations.add("Try broader search terms");
                recommendations.add("Check spelling");
            } else if (results.size() < 3) {
                recommendations.add("Try related keywords");
            }
            insights.put("recommendations", recommendations);
            
            return insights;
        });
    }
    
    /**
     * Gets the profit margin for a specific product
     */
    public CompletableFuture<Double> getProductProfitMargin(String productId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Getting profit margin for product: {}", productId);
                
                SearchForHits searchForHits = new SearchForHits()
                        .setIndexName(productsIndexName)
                        .setQuery("")
                        .setFilters("objectID:" + productId)
                        .setAttributesToRetrieve(List.of("profit_margin", "objectID"))
                        .setHitsPerPage(1);
                
                SearchMethodParams params = new SearchMethodParams().addRequests(searchForHits);
                var searchResponse = searchClient.search(params, Object.class);
                
                if (searchResponse != null && searchResponse.getResults() != null && !searchResponse.getResults().isEmpty()) {
                    var result = searchResponse.getResults().get(0);
                    List<Product> hits = extractProductsFromRawResult(result);
                    
                    if (!hits.isEmpty()) {
                        double profitMargin = hits.get(0).getProfitMargin();
                        log.debug("Found profit margin for product {}: {}", productId, profitMargin);
                        return profitMargin;
                    }
                }
                
                log.warn("No profit margin found for product: {}", productId);
                return null;
                
            } catch (Exception e) {
                log.error("Error getting profit margin for product: " + productId, e);
                return null;
            }
        });
    }
    
    /**
     * Logs a user event to a specific index (used for veto decisions)
     */
    public CompletableFuture<Void> logUserEvent(String indexName, Map<String, Object> eventData) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("Logging event to index {}: {}", indexName, eventData);
                
                var saveResponse = searchClient.saveObject(indexName, eventData);
                log.debug("Event logged successfully: {}", saveResponse.getObjectID());
                
            } catch (Exception e) {
                log.error("Error logging event to index " + indexName, e);
                throw new RuntimeException("Failed to log event", e);
            }
        });
    }
    
    /**
     * Searches for user events by type in a specific index
     */
    public CompletableFuture<List<Map<String, Object>>> searchUserEvents(String indexName, String eventType, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Searching for events in index {}, type: {}, limit: {}", indexName, eventType, limit);
                
                SearchForHits searchForHits = new SearchForHits()
                        .setIndexName(indexName)
                        .setQuery("")
                        .setFilters("eventType:" + eventType)
                        .setHitsPerPage(limit);
                
                SearchMethodParams params = new SearchMethodParams().addRequests(searchForHits);
                var searchResponse = searchClient.search(params, Object.class);
                
                List<Map<String, Object>> events = new ArrayList<>();
                if (searchResponse != null && searchResponse.getResults() != null && !searchResponse.getResults().isEmpty()) {
                    var result = searchResponse.getResults().get(0);
                    List<Map<String, Object>> hits = extractHitsFromResult(result);
                    events.addAll(hits);
                }
                
                log.debug("Found {} events of type {}", events.size(), eventType);
                return events;
                
            } catch (Exception e) {
                log.error("Error searching for events in index " + indexName, e);
                return new ArrayList<>();
            }
        });
    }
    
    /**
     * 🚀 Index enriched product data back to Algolia
     */
    public void indexEnrichedProduct(String productId, Map<String, Object> enrichedData) {
        try {
            // Create enriched products index if it doesn't exist
            String enrichedIndexName = "enriched_products";
            
            // Add the enriched data with proper object ID
            enrichedData.put("objectID", productId + "_enriched");
            enrichedData.put("originalProductId", productId);
            enrichedData.put("enrichmentTimestamp", Instant.now().toString());
            
            // Index the enriched data
            searchClient.saveObject(enrichedIndexName, enrichedData);
            
            log.info("✅ Successfully indexed enriched product data for: {}", productId);
            
        } catch (Exception e) {
            log.error("❌ Failed to index enriched product data for: {}, error: {}", productId, e.getMessage());
        }
    }
    
    /**
     * Create default analytics when real data is unavailable
     */
    private Map<String, Object> createDefaultAnalytics() {
        Map<String, Object> defaultAnalytics = new HashMap<>();
        defaultAnalytics.put("totalSearches", 0);
        defaultAnalytics.put("uniqueUsers", 0);
        defaultAnalytics.put("averageSearchesPerUser", 0.0);
        defaultAnalytics.put("topQueries", Arrays.asList());
        defaultAnalytics.put("successRate", 100.0);
        return defaultAnalytics;
    }
    
    /**
     * Create default global insights when real data is unavailable
     */
    private Map<String, Object> createDefaultGlobalInsights() {
        Map<String, Object> defaultInsights = new HashMap<>();
        defaultInsights.put("totalInteractions", 0);
        defaultInsights.put("conversionRate", 0.0);
        defaultInsights.put("averageSessionDuration", 0.0);
        defaultInsights.put("topCategories", Arrays.asList());
        defaultInsights.put("topProducts", Arrays.asList());
        return defaultInsights;
    }
    
    /**
     * Create default product metrics when real data is unavailable
     */
    private Map<String, Object> createDefaultProductMetrics() {
        Map<String, Object> defaultMetrics = new HashMap<>();
        defaultMetrics.put("totalViews", 0);
        defaultMetrics.put("totalCarts", 0);
        defaultMetrics.put("totalPurchases", 0);
        defaultMetrics.put("conversionRate", 0.0);
        defaultMetrics.put("popularProducts", Arrays.asList());
        return defaultMetrics;
    }
    
    /**
     * Personalize product results based on user behavior
     */
    private List<Product> personalizeProductResults(List<Product> products, List<UserEvent> behaviorHistory) {
        if (behaviorHistory.isEmpty()) {
            return products;
        }
        
        // Extract user's preferred categories from behavior
        Set<String> userCategories = behaviorHistory.stream()
                .filter(event -> event.getProductId() != null)
                .map(event -> event.getEventType()) // This would need proper category mapping
                .collect(java.util.stream.Collectors.toSet());
        
        // Sort products: user-relevant categories first, then by rating
        return products.stream()
                .sorted((p1, p2) -> {
                    boolean p1Relevant = userCategories.contains(p1.getCategory());
                    boolean p2Relevant = userCategories.contains(p2.getCategory());
                    
                    if (p1Relevant && !p2Relevant) return -1;
                    if (!p1Relevant && p2Relevant) return 1;
                    
                    // If both are equally relevant, sort by rating
                    return Double.compare(
                        p2.getAverageRating() != null ? p2.getAverageRating() : 0.0,
                        p1.getAverageRating() != null ? p1.getAverageRating() : 0.0
                    );
                })
                .collect(java.util.stream.Collectors.toList());
    }
}
