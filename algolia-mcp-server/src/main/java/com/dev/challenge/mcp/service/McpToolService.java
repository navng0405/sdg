package com.dev.challenge.mcp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpToolService {
    
    private static final Logger log = LoggerFactory.getLogger(McpToolService.class);
    
    private final AlgoliaService algoliaService;
    private final GeminiService geminiService;
    private final DiscountService discountService;
    
    /**
     * MCP Tool: getUserHesitationData
     * Analyzes user behavior patterns to determine hesitation level
     */
    public Map<String, Object> getUserHesitationData(Map<String, Object> parameters) {
        String userId = (String) parameters.get("userId");
        log.info("Analyzing hesitation data for user: {}", userId);
        
        try {
            // Get user behavior history from Algolia
            List<Map<String, Object>> behaviorHistory = algoliaService.getUserBehaviorHistory(userId, 50)
                .get(); // Convert CompletableFuture to blocking call for MCP tool
            
            // Analyze behavior patterns
            Map<String, Object> analysis = analyzeBehaviorPatterns(behaviorHistory);
            
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("hesitationLevel", determineHesitationLevel(analysis));
            result.put("behaviorSummary", analysis);
            result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            log.info("Hesitation analysis complete for user {}: level={}", userId, result.get("hesitationLevel"));
            return result;
            
        } catch (Exception e) {
            log.error("Error analyzing hesitation data for user: {}", userId, e);
            throw new RuntimeException("Failed to analyze user hesitation data: " + e.getMessage());
        }
    }
    
    /**
     * MCP Tool: getProductProfitMargin
     * Retrieves product financial data from Algolia
     */
    public Map<String, Object> getProductProfitMargin(Map<String, Object> parameters) {
        String productId = (String) parameters.get("productId");
        log.info("Retrieving profit margin for product: {}", productId);
        
        try {
            // Get product data from Algolia
            Map<String, Object> product = algoliaService.getProduct(productId)
                .get(); // Convert CompletableFuture to blocking call for MCP tool
            
            if (product == null) {
                throw new RuntimeException("Product not found: " + productId);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("productId", productId);
            result.put("profitMargin", product.get("profit_margin"));
            result.put("inventoryLevel", product.get("inventory_level"));
            result.put("price", product.get("price"));
            result.put("category", product.get("category"));
            result.put("name", product.get("name"));
            
            log.info("Retrieved profit margin for product {}: {}%", productId, 
                    ((Double) product.get("profit_margin")) * 100);
            return result;
            
        } catch (Exception e) {
            log.error("Error retrieving profit margin for product: {}", productId, e);
            throw new RuntimeException("Failed to retrieve product profit margin: " + e.getMessage());
        }
    }
    
    /**
     * MCP Tool: generateSmartDiscount
     * Uses AI to generate personalized discount offers
     */
    public Map<String, Object> generateSmartDiscount(Map<String, Object> parameters) {
        String userId = (String) parameters.get("userId");
        String productId = (String) parameters.get("productId");
        Map<String, Object> behaviorSummary = (Map<String, Object>) parameters.get("behaviorSummary");
        Double profitMargin = ((Number) parameters.get("profitMargin")).doubleValue();
        String userSegment = (String) parameters.getOrDefault("userSegment", "standard");
        
        log.info("Generating smart discount for user: {}, product: {}", userId, productId);
        
        try {
            // Create AI prompt for discount generation
            String aiPrompt = buildDiscountGenerationPrompt(userId, productId, behaviorSummary, profitMargin, userSegment);
            
            // Call Gemini API for creative discount generation
            String aiResponse = geminiService.generateDiscountOffer(aiPrompt).get();
            
            // Parse AI response and create discount
            Map<String, Object> discountData = parseAiDiscountResponse(aiResponse);
            
            // Generate unique discount code
            String discountCode = discountService.generateUniqueDiscountCode(userId, discountData);
            
            // Store active discount
            Map<String, Object> discount = createDiscountObject(userId, productId, discountCode, discountData);
            discountService.storeActiveDiscount(discount);
            
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("productId", productId);
            result.put("discountCode", discountCode);
            result.put("discountType", discountData.get("type"));
            result.put("discountValue", discountData.get("value"));
            result.put("message", discountData.get("message"));
            result.put("urgencyText", discountData.get("urgencyText"));
            result.put("expiresAt", discount.get("expiresAt"));
            result.put("expiresInSeconds", 1800); // 30 minutes
            result.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            log.info("Generated smart discount for user {}: code={}, type={}, value={}", 
                    userId, discountCode, discountData.get("type"), discountData.get("value"));
            return result;
            
        } catch (Exception e) {
            log.error("Error generating smart discount for user: {}, product: {}", userId, productId, e);
            throw new RuntimeException("Failed to generate smart discount: " + e.getMessage());
        }
    }
    
    /**
     * MCP Tool: logDiscountConversion
     * Logs discount conversion events for analytics
     */
    public Map<String, Object> logDiscountConversion(Map<String, Object> parameters) {
        String discountCode = (String) parameters.get("discountCode");
        String userId = (String) parameters.get("userId");
        String conversionStatus = (String) parameters.get("conversionStatus");
        
        log.info("Logging discount conversion: code={}, user={}, status={}", 
                discountCode, userId, conversionStatus);
        
        try {
            // Create conversion log entry
            Map<String, Object> conversionLog = new HashMap<>();
            conversionLog.put("discountCode", discountCode);
            conversionLog.put("userId", userId);
            conversionLog.put("conversionStatus", conversionStatus);
            conversionLog.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            conversionLog.put("eventType", "discount_conversion");
            
            // Store in Algolia for analytics
            algoliaService.storeConversionEvent(conversionLog);
            
            // Optional: Trigger n8n webhook for advanced analytics
            // n8nService.triggerConversionWebhook(conversionLog);
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "logged");
            result.put("discountCode", discountCode);
            result.put("userId", userId);
            result.put("conversionStatus", conversionStatus);
            result.put("loggedAt", conversionLog.get("timestamp"));
            
            log.info("Successfully logged discount conversion for code: {}", discountCode);
            return result;
            
        } catch (Exception e) {
            log.error("Error logging discount conversion for code: {}", discountCode, e);
            throw new RuntimeException("Failed to log discount conversion: " + e.getMessage());
        }
    }
    
    // Helper methods
    
    private Map<String, Object> analyzeBehaviorPatterns(List<Map<String, Object>> behaviorHistory) {
        Map<String, Object> analysis = new HashMap<>();
        
        int cartAbandonments = 0;
        int priceHovers = 0;
        int multipleViews = 0;
        int noResultsSearches = 0;
        Set<String> viewedProducts = new HashSet<>();
        
        for (Map<String, Object> event : behaviorHistory) {
            String eventType = (String) event.get("eventType");
            String productId = (String) event.get("productId");
            
            switch (eventType) {
                case "cart_abandon" -> cartAbandonments++;
                case "price_hover" -> priceHovers++;
                case "product_view" -> {
                    if (productId != null) {
                        if (viewedProducts.contains(productId)) {
                            multipleViews++;
                        }
                        viewedProducts.add(productId);
                    }
                }
                case "no_results_search" -> noResultsSearches++;
            }
        }
        
        analysis.put("cartAbandonments", cartAbandonments);
        analysis.put("priceHovers", priceHovers);
        analysis.put("multipleViews", multipleViews);
        analysis.put("noResultsSearches", noResultsSearches);
        analysis.put("totalEvents", behaviorHistory.size());
        analysis.put("uniqueProductsViewed", viewedProducts.size());
        
        return analysis;
    }
    
    private String determineHesitationLevel(Map<String, Object> analysis) {
        int cartAbandonments = (Integer) analysis.get("cartAbandonments");
        int priceHovers = (Integer) analysis.get("priceHovers");
        int multipleViews = (Integer) analysis.get("multipleViews");
        int noResultsSearches = (Integer) analysis.get("noResultsSearches");
        
        int hesitationScore = cartAbandonments * 3 + priceHovers * 2 + multipleViews + noResultsSearches * 2;
        
        if (hesitationScore >= 8) return "high";
        if (hesitationScore >= 4) return "medium";
        return "low";
    }
    
    private String buildDiscountGenerationPrompt(String userId, String productId, 
            Map<String, Object> behaviorSummary, Double profitMargin, String userSegment) {
        
        return String.format("""
            Generate a personalized discount offer based on the following user analysis:
            
            User ID: %s
            Product ID: %s
            User Segment: %s
            Profit Margin: %.2f%%
            
            Behavior Analysis:
            - Cart Abandonments: %s
            - Price Hovers: %s
            - Multiple Views: %s
            - No Results Searches: %s
            - Hesitation Level: %s
            
            Instructions:
            1. Create a compelling discount offer (percentage or flat amount)
            2. Keep profit margin above 15%%
            3. Generate urgency-driven copy (1-2 sentences)
            4. Match the discount type to the user's behavior pattern
            5. Return in JSON format: {"type": "percentage|flat_amount|free_shipping", "value": number, "message": "string", "urgencyText": "string"}
            """,
            userId, productId, userSegment, profitMargin * 100,
            behaviorSummary.get("cartAbandonments"),
            behaviorSummary.get("priceHovers"),
            behaviorSummary.get("multipleViews"),
            behaviorSummary.get("noResultsSearches"),
            behaviorSummary.get("hesitationLevel")
        );
    }
    
    private Map<String, Object> parseAiDiscountResponse(String aiResponse) {
        // Simple JSON parsing - in production, use proper JSON parser
        Map<String, Object> result = new HashMap<>();
        
        // Default fallback values
        result.put("type", "percentage");
        result.put("value", 15.0);
        result.put("message", "Special discount just for you!");
        result.put("urgencyText", "Limited time offer!");
        
        // TODO: Implement proper JSON parsing of AI response
        // For now, return default values
        
        return result;
    }
    
    private Map<String, Object> createDiscountObject(String userId, String productId, 
            String discountCode, Map<String, Object> discountData) {
        
        Map<String, Object> discount = new HashMap<>();
        discount.put("code", discountCode);
        discount.put("userId", userId);
        discount.put("productId", productId);
        discount.put("type", discountData.get("type"));
        discount.put("value", discountData.get("value"));
        discount.put("message", discountData.get("message"));
        discount.put("active", true);
        discount.put("used", false);
        discount.put("createdAt", LocalDateTime.now());
        discount.put("expiresAt", LocalDateTime.now().plusMinutes(30));
        
        return discount;
    }
}
