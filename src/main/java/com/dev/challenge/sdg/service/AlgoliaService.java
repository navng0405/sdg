package com.dev.challenge.sdg.service;

import com.algolia.search.SearchClient;
import com.algolia.search.SearchIndex;
import com.algolia.search.models.indexing.SearchResult;
import com.algolia.search.models.indexing.Query;
import com.dev.challenge.sdg.model.Product;
import com.dev.challenge.sdg.model.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlgoliaService {
    
    private final SearchClient searchClient;
    
    @Value("${algolia.indexes.products}")
    private String productsIndexName;
    
    @Value("${algolia.indexes.user-events}")
    private String userEventsIndexName;
    
    @Value("${algolia.indexes.discount-templates}")
    private String discountTemplatesIndexName;
    
    public CompletableFuture<Void> storeUserEvent(UserEvent userEvent) {
        log.debug("Storing user event: {}", userEvent);
        
        SearchIndex<UserEvent> userEventsIndex = searchClient.initIndex(userEventsIndexName, UserEvent.class);
        
        // Generate unique ID if not provided
        if (userEvent.getObjectId() == null) {
            userEvent.setObjectId(UUID.randomUUID().toString());
        }
        
        // Set timestamp if not provided
        if (userEvent.getTimestamp() == null) {
            userEvent.setTimestamp(LocalDateTime.now());
        }
        
        return userEventsIndex.saveObjectAsync(userEvent)
                .thenRun(() -> log.info("Successfully stored user event: {}", userEvent.getObjectId()));
    }
    
    public CompletableFuture<List<UserEvent>> getUserBehaviorHistory(String userId, int limit) {
        log.debug("Retrieving behavior history for user: {}", userId);
        
        SearchIndex<UserEvent> userEventsIndex = searchClient.initIndex(userEventsIndexName, UserEvent.class);
        
        Query query = new Query()
                .setFilters("userId:" + userId)
                .setHitsPerPage(limit);
        
        return userEventsIndex.searchAsync(query)
                .thenApply(SearchResult::getHits)
                .thenApply(hits -> {
                    log.info("Retrieved {} behavior events for user: {}", hits.size(), userId);
                    return hits;
                });
    }
    
    public CompletableFuture<Product> getProduct(String productId) {
        log.debug("Retrieving product: {}", productId);
        
        SearchIndex<Product> productsIndex = searchClient.initIndex(productsIndexName, Product.class);
        
        return productsIndex.getObjectAsync(productId)
                .thenApply(product -> {
                    log.info("Retrieved product: {}", product.getName());
                    return product;
                });
    }
    
    public CompletableFuture<List<Product>> searchProducts(String query, int limit) {
        log.debug("Searching products with query: {}", query);
        
        SearchIndex<Product> productsIndex = searchClient.initIndex(productsIndexName, Product.class);
        
        Query searchQuery = new Query(query)
                .setHitsPerPage(limit);
        
        return productsIndex.searchAsync(searchQuery)
                .thenApply(SearchResult::getHits)
                .thenApply(hits -> {
                    log.info("Found {} products for query: {}", hits.size(), query);
                    return hits;
                });
    }
    
    public CompletableFuture<Void> initializeIndexes() {
        log.info("Initializing Algolia indexes");
        
        // Initialize products index with sample data
        return initializeProductsIndex()
                .thenCompose(v -> initializeUserEventsIndex())
                .thenCompose(v -> initializeDiscountTemplatesIndex())
                .thenRun(() -> log.info("All Algolia indexes initialized successfully"));
    }
    
    private CompletableFuture<Void> initializeProductsIndex() {
        SearchIndex<Product> productsIndex = searchClient.initIndex(productsIndexName, Product.class);
        
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
        
        return productsIndex.saveObjectsAsync(sampleProducts);
    }
    
    private CompletableFuture<Void> initializeUserEventsIndex() {
        SearchIndex<UserEvent> userEventsIndex = searchClient.initIndex(userEventsIndexName, UserEvent.class);
        
        // Set up index settings for user events
        Map<String, Object> settings = Map.of(
                "searchableAttributes", List.of("userId", "eventType", "query"),
                "attributesForFaceting", List.of("userId", "eventType", "productId")
        );
        
        return userEventsIndex.setSettingsAsync(settings);
    }
    
    private CompletableFuture<Void> initializeDiscountTemplatesIndex() {
        SearchIndex<Map<String, Object>> discountTemplatesIndex = searchClient.initIndex(discountTemplatesIndexName, Map.class);
        
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
        
        return discountTemplatesIndex.saveObjectsAsync(templates);
    }
}
