package com.dev.challenge.sdg.controller;

import com.dev.challenge.sdg.dto.DiscountResponse;
import com.dev.challenge.sdg.dto.UserBehaviorRequest;
import com.dev.challenge.sdg.model.Discount;
import com.dev.challenge.sdg.model.Product;
import com.dev.challenge.sdg.model.UserEvent;
import com.dev.challenge.sdg.service.AlgoliaService;
import com.dev.challenge.sdg.service.DiscountService;
import com.dev.challenge.sdg.service.ProfitProtectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allow CORS for frontend integration
public class ApiController {
    
    private final AlgoliaService algoliaService;
    private final DiscountService discountService;
    private final ProfitProtectionService profitProtectionService;
    
    @PostMapping("/user-behavior")
    public ResponseEntity<Map<String, String>> trackUserBehavior(@Valid @RequestBody UserBehaviorRequest request) {
        log.info("Tracking user behavior: {} for user: {}", request.getEventType(), request.getUserId());
        
        try {
            UserEvent userEvent = UserEvent.builder()
                    .objectId(UUID.randomUUID().toString())
                    .userId(request.getUserId())
                    .eventType(request.getEventType())
                    .productId(request.getProductId())
                    .query(request.getQuery())
                    .details(request.getDetails())
                    .timestamp(request.getTimestamp() != null ? request.getTimestamp() : Instant.now())
                    .build();
            
            algoliaService.storeUserEvent(userEvent).get();
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "User behavior tracked successfully"
            ));
        } catch (Exception e) {
            log.error("Error tracking user behavior", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to track user behavior"
            ));
        }
    }
    
    @GetMapping("/get-discount")
    public ResponseEntity<DiscountResponse> getDiscount(
            @RequestParam String userId, 
            @RequestParam(required = false) String productId) {
        log.info("Getting discount for user: {} and product: {}", userId, productId);
        
        try {
            Discount discount = discountService.generatePersonalizedDiscount(userId, productId).get();
            
            if (discount == null) {
                return ResponseEntity.ok(DiscountResponse.noOffer("No specific offer for this user at this time."));
            }
            
            return ResponseEntity.ok(DiscountResponse.offerGenerated(discount));
        } catch (Exception e) {
            log.error("Error generating discount for user: " + userId, e);
            return ResponseEntity.internalServerError().body(
                    DiscountResponse.noOffer("Error generating discount offer")
            );
        }
    }
    
    @PostMapping("/validate-discount")
    public ResponseEntity<Map<String, Object>> validateDiscount(
            @RequestParam String discountCode,
            @RequestParam String userId) {
        
        log.info("Validating discount code: {} for user: {}", discountCode, userId);
        
        try {
            boolean isValid = discountService.validateDiscount(discountCode, userId);
            Discount discount = discountService.getActiveDiscount(discountCode);
            
            return ResponseEntity.ok(Map.of(
                    "valid", isValid,
                    "discount", discount != null ? discount : Map.of(),
                    "message", isValid ? "Discount is valid" : "Invalid or expired discount"
            ));
        } catch (Exception e) {
            log.error("Error validating discount", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "valid", false,
                    "message", "Error validating discount"
            ));
        }
    }
    
    @PostMapping("/apply-discount")
    public ResponseEntity<Map<String, String>> applyDiscount(
            @RequestParam String discountCode,
            @RequestParam String userId) {
        
        log.info("Applying discount code: {} for user: {}", discountCode, userId);
        
        try {
            if (discountService.validateDiscount(discountCode, userId)) {
                discountService.markDiscountAsUsed(discountCode);
                
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Discount applied successfully"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Invalid or expired discount code"
                ));
            }
        } catch (Exception e) {
            log.error("Error applying discount", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Error applying discount"
            ));
        }
    }
    
    @GetMapping("/user-behavior/{userId}")
    public ResponseEntity<Map<String, Object>> getUserBehaviorHistory(
            @PathVariable String userId,
            @RequestParam(defaultValue = "20") int limit) {
        
        log.info("Getting behavior history for user: {}", userId);
        
        try {
            var behaviorHistory = algoliaService.getUserBehaviorHistory(userId, limit).get();
            
            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "events", behaviorHistory,
                    "totalEvents", behaviorHistory.size()
            ));
        } catch (Exception e) {
            log.error("Error retrieving user behavior history", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to retrieve behavior history"
            ));
        }
    }
    
    @GetMapping("/active-discounts")
    public ResponseEntity<Map<String, Object>> getActiveDiscounts() {
        log.info("Getting all active discounts");
        
        try {
            // Clean up expired discounts first
            discountService.clearExpiredDiscounts();
            
            var activeDiscounts = discountService.getAllActiveDiscounts();
            
            return ResponseEntity.ok(Map.of(
                    "activeDiscounts", activeDiscounts,
                    "count", activeDiscounts.size()
            ));
        } catch (Exception e) {
            log.error("Error retrieving active discounts", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to retrieve active discounts"
            ));
        }
    }
    
    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> getProducts(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "20") int limit) {
        
        log.info("Getting products with query: '{}', limit: {}", query, limit);
        
        try {
            List<Product> products;
            if (query.isEmpty()) {
                // Get all products by searching with empty query
                products = algoliaService.searchProducts("", limit).get();
            } else {
                // Search products with specific query
                products = algoliaService.searchProducts(query, limit).get();
            }
            
            return ResponseEntity.ok(Map.of(
                    "products", products,
                    "count", products.size(),
                    "query", query
            ));
        } catch (Exception e) {
            log.error("Error retrieving products", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to retrieve products"
            ));
        }
    }
    
    @GetMapping("/products/{productId}")
    public ResponseEntity<Map<String, Object>> getProduct(@PathVariable String productId) {
        log.info("Getting product: {}", productId);
        
        try {
            Product product = algoliaService.getProduct(productId).get();
            
            if (product != null) {
                return ResponseEntity.ok(Map.of(
                        "product", product,
                        "found", true
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error retrieving product: {}", productId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to retrieve product"
            ));
        }
    }
    
    @PostMapping("/products/batch")
    public ResponseEntity<Map<String, Object>> getProductsBatch(@RequestBody Map<String, List<String>> request) {
        List<String> productIds = request.get("productIds");
        log.info("Getting batch of products: {}", productIds);
        
        try {
            Map<String, Product> products = new java.util.HashMap<>();
            
            for (String productId : productIds) {
                Product product = algoliaService.getProduct(productId).get();
                if (product != null) {
                    products.put(productId, product);
                }
            }
            
            return ResponseEntity.ok(Map.of(
                    "products", products,
                    "requested", productIds.size(),
                    "found", products.size()
            ));
        } catch (Exception e) {
            log.error("Error retrieving batch products", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to retrieve batch products"
            ));
        }
    }
    
    @GetMapping("/algolia-config")
    public ResponseEntity<Map<String, String>> getAlgoliaConfig() {
        log.info("Providing Algolia configuration for frontend");
        
        try {
            return ResponseEntity.ok(Map.of(
                    "applicationId", algoliaService.getApplicationId(),
                    "searchApiKey", algoliaService.getSearchApiKey(),
                    "indexName", "sdg_products"
            ));
        } catch (Exception e) {
            log.error("Error getting Algolia configuration", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get Algolia configuration"));
        }
    }

    @GetMapping("/ai-insights")
    public ResponseEntity<Map<String, Object>> getAIInsights(
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "7") int days) {
        log.info("Generating AI insights for user: {} over {} days", userId, days);
        
        try {
            // Get comprehensive analytics from Algolia
            var searchAnalytics = algoliaService.getSearchAnalytics(days).get();
            var userBehaviorInsights = userId != null ? 
                algoliaService.getUserBehaviorInsights(userId, days).get() : 
                algoliaService.getGlobalBehaviorInsights(days).get();
            var productPerformance = algoliaService.getProductPerformanceMetrics(days).get();
            
            // Generate AI-powered recommendations
            var recommendations = discountService.generateAIRecommendations(
                searchAnalytics, userBehaviorInsights, productPerformance).get();
            
            return ResponseEntity.ok(Map.of(
                    "searchAnalytics", searchAnalytics,
                    "behaviorInsights", userBehaviorInsights,
                    "productPerformance", productPerformance,
                    "aiRecommendations", recommendations,
                    "generatedAt", Instant.now().toString(),
                    "analysisScope", days + " days"
            ));
        } catch (Exception e) {
            log.error("Error generating AI insights", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to generate AI insights"
            ));
        }
    }

    @PostMapping("/smart-search")
    public ResponseEntity<Map<String, Object>> performSmartSearch(
            @RequestBody Map<String, Object> searchRequest) {
        
        String query = (String) searchRequest.get("query");
        String userId = (String) searchRequest.get("userId");
        Map<String, Object> context = (Map<String, Object>) searchRequest.getOrDefault("context", Map.of());
        
        log.info("Performing AI-enhanced smart search for query: '{}' by user: {}", query, userId);
        
        try {
            // Track the search behavior
            if (userId != null) {
                UserEvent searchEvent = UserEvent.builder()
                        .objectId(UUID.randomUUID().toString())
                        .userId(userId)
                        .eventType("smart_search")
                        .query(query)
                        .details(context)
                        .timestamp(Instant.now())
                        .build();
                algoliaService.storeUserEvent(searchEvent);
            }
            
            // Perform enhanced search with AI personalization
            var searchResults = algoliaService.performEnhancedSearch(query, userId, context).get();
            var personalizedResults = discountService.personalizeSearchResults(searchResults, userId).get();
            
            // Generate search insights
            var searchInsights = algoliaService.generateSearchInsights(query, searchResults).get();
            
            return ResponseEntity.ok(Map.of(
                    "results", personalizedResults,
                    "searchInsights", searchInsights,
                    "query", query,
                    "resultCount", personalizedResults.size(),
                    "processingTime", System.currentTimeMillis(),
                    "personalized", userId != null
            ));
        } catch (Exception e) {
            log.error("Error performing smart search", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to perform smart search"
            ));
        }
    }

    @PostMapping("/ai-chat")
    public ResponseEntity<Map<String, Object>> handleAIChat(@RequestBody Map<String, Object> chatRequest) {
        String message = (String) chatRequest.get("message");
        String userId = (String) chatRequest.get("userId");
        List<Map<String, Object>> chatHistory = (List<Map<String, Object>>) chatRequest.getOrDefault("history", List.of());
        
        log.info("Processing AI chat message from user: {}", userId);
        
        try {
            // Use Algolia data to enhance AI responses
            var relevantProducts = algoliaService.findRelevantProducts(message).get();
            var userContext = userId != null ? algoliaService.getUserContext(userId).get() : new HashMap<String, Object>();
            
            // Generate AI response with Algolia-powered context
            Map<String, Object> aiResponse = discountService.generateAIChatResponse(
                message, chatHistory, relevantProducts, userContext).get();
            
            // Track chat interaction
            if (userId != null) {
                UserEvent chatEvent = UserEvent.builder()
                        .objectId(UUID.randomUUID().toString())
                        .userId(userId)
                        .eventType("ai_chat")
                        .query(message)
                        .details(createDetailsMap((String) aiResponse.get("type"), relevantProducts.size()))
                        .timestamp(Instant.now())
                        .build();
                algoliaService.storeUserEvent(chatEvent);
            }
            
            return ResponseEntity.ok(Map.of(
                    "response", aiResponse.get("message"),
                    "suggestedProducts", relevantProducts,
                    "responseType", aiResponse.get("type"),
                    "confidence", aiResponse.get("confidence"),
                    "timestamp", Instant.now().toString()
            ));
        } catch (Exception e) {
            log.error("Error processing AI chat", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to process AI chat message"
            ));
        }
    }
    
    private Map<String, Object> createDetailsMap(String responseType, int productsFound) {
        Map<String, Object> details = new HashMap<>();
        details.put("responseType", responseType);
        details.put("productsFound", productsFound);
        return details;
    }

    @GetMapping("/profit-protection/veto-decisions")
    public ResponseEntity<Map<String, Object>> getVetoDecisions(
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("Getting recent veto decisions, limit: {}", limit);
        
        try {
            var vetoDecisions = profitProtectionService.getRecentVetoDecisions(limit).get();
            
            return ResponseEntity.ok(Map.of(
                    "vetoDecisions", vetoDecisions,
                    "count", vetoDecisions.size(),
                    "timestamp", java.time.Instant.now().toString()
            ));
        } catch (Exception e) {
            log.error("Error retrieving veto decisions", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to retrieve veto decisions",
                    "vetoDecisions", java.util.List.of(),
                    "count", 0
            ));
        }
    }
}
