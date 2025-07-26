package com.dev.challenge.sdg.service;

import com.algolia.api.SearchClient;
import com.algolia.api.AnalyticsClient;
import com.algolia.model.search.*;
import com.dev.challenge.sdg.model.Product;
import com.dev.challenge.sdg.model.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
            
            var addResponse = searchClient.saveObject(userEventsIndexName, userEvent);
            searchClient.waitForTask(userEventsIndexName, addResponse.getTaskID());
            log.info("Successfully stored user event: {}", userEvent.getObjectId());
            
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to store user event: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
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
            
            // Perform the search
            SearchResponses<UserEvent> response = searchClient.search(params, UserEvent.class);
            List<UserEvent> hits = new ArrayList<>();
            
            if (response.getResults() != null && !response.getResults().isEmpty()) {
                var result = response.getResults().get(0);
                if (result.getHits() != null) {
                    hits = result.getHits();
                }
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
            Product product = searchClient.getObject(productsIndexName, productId, Product.class);
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
                if (result.getHits() != null) {
                    hits = result.getHits();
                }
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
            
            var addResponse = searchClient.saveObjects(productsIndexName, sampleProducts);
            searchClient.waitForTask(productsIndexName, addResponse.getTaskID());
            log.info("Products index initialized with {} sample products", sampleProducts.size());
        } catch (Exception e) {
            log.error("Failed to initialize products index: {}", e.getMessage(), e);
        }
    }
    
    private void initializeUserEventsIndex() {
        try {
            // Set up index settings for user events
            Map<String, Object> settings = Map.of(
                    "searchableAttributes", List.of("userId", "eventType", "query"),
                    "attributesForFaceting", List.of("userId", "eventType", "productId")
            );
            
            searchClient.setSettings(userEventsIndexName, settings);
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
            
            var addResponse = searchClient.saveObjects(discountTemplatesIndexName, templates);
            searchClient.waitForTask(discountTemplatesIndexName, addResponse.getTaskID());
            log.info("Discount templates index initialized with {} templates", templates.size());
        } catch (Exception e) {
            log.error("Failed to initialize discount templates index: {}", e.getMessage(), e);
        }
    }
    
    // Additional utility methods from the working example
    public void addProduct(Product product) {
        try {
            var addResponse = searchClient.saveObject(productsIndexName, product);
            searchClient.waitForTask(productsIndexName, addResponse.getTaskID());
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
}
