package com.dev.challenge.sdg.service;

import com.dev.challenge.sdg.dto.McpResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


/**
 * MCP Tool Service - Implements business logic for MCP tools
 * Handles all MCP tool execution with direct service injection (no HTTP calls)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpToolService {
    

    
    private final AlgoliaService algoliaService;
    private final GeminiService geminiService;
    private final DiscountService discountService;
    
    /**
     * Returns list of available MCP tools with their definitions
     */
    public List<McpResponse.ToolDefinition> getAvailableTools() {
        return List.of(
            createToolDefinition("getUserHesitationData", 
                "Retrieves detailed user behavior data from Algolia to assess hesitation patterns",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "userId", Map.of("type", "string", "description", "User ID to analyze")
                    ),
                    "required", List.of("userId")
                )),
            
            createToolDefinition("getProductProfitMargin",
                "Retrieves product profit margin and inventory data from Algolia",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "productId", Map.of("type", "string", "description", "Product ID to query")
                    ),
                    "required", List.of("productId")
                )),
            
            createToolDefinition("generateSmartDiscount",
                "Generates AI-powered discount offer based on user behavior and product data",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "userId", Map.of("type", "string", "description", "User ID"),
                        "productId", Map.of("type", "string", "description", "Product ID"),
                        "behaviorSummary", Map.of("type", "object", "description", "User behavior analysis"),
                        "profitMargin", Map.of("type", "number", "description", "Product profit margin"),
                        "userSegment", Map.of("type", "string", "description", "User segment classification")
                    ),
                    "required", List.of("userId", "productId", "behaviorSummary", "profitMargin")
                )),
            
            createToolDefinition("logDiscountConversion",
                "Logs discount conversion events for analytics and optimization",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "discountCode", Map.of("type", "string", "description", "Discount code"),
                        "userId", Map.of("type", "string", "description", "User ID"),
                        "conversionStatus", Map.of("type", "string", "description", "Conversion status")
                    ),
                    "required", List.of("discountCode", "userId", "conversionStatus")
                ))
        );
    }
    
    /**
     * Executes MCP tool by name with given arguments
     */
    public Map<String, Object> executeTool(String toolName, Map<String, Object> arguments) {
        log.info("Executing MCP tool: {} with arguments: {}", toolName, arguments);
        
        try {
            return switch (toolName) {
                case "getUserHesitationData" -> executeGetUserHesitationData(arguments);
                case "getProductProfitMargin" -> executeGetProductProfitMargin(arguments);
                case "generateSmartDiscount" -> executeGenerateSmartDiscount(arguments);
                case "logDiscountConversion" -> executeLogDiscountConversion(arguments);
                default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
            };
        } catch (Exception e) {
            log.error("Error executing tool: {}", toolName, e);
            throw new RuntimeException("Tool execution failed: " + e.getMessage());
        }
    }
    
    /**
     * Tool: getUserHesitationData - Analyzes user behavior patterns
     */
    private Map<String, Object> executeGetUserHesitationData(Map<String, Object> arguments) {
        String userId = (String) arguments.get("userId");
        log.info("Analyzing hesitation data for user: {}", userId);
        
        try {
            CompletableFuture<List<com.dev.challenge.sdg.model.UserEvent>> behaviorFuture = 
                algoliaService.getUserBehaviorHistory(userId, 50);
            List<com.dev.challenge.sdg.model.UserEvent> userEvents = behaviorFuture.get();
            
            // Convert UserEvent objects to Map format for analysis
            List<Map<String, Object>> behaviorHistory = userEvents.stream()
                .map(this::convertUserEventToMap)
                .toList();
            
            // Analyze behavior patterns
            Map<String, Object> analysis = analyzeBehaviorPatterns(behaviorHistory);
            
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("behaviorHistory", behaviorHistory);
            result.put("hesitationAnalysis", analysis);
            result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            log.info("Completed hesitation analysis for user: {}", userId);
            return result;
            
        } catch (Exception e) {
            log.error("Error analyzing user hesitation data", e);
            throw new RuntimeException("Failed to analyze user behavior: " + e.getMessage());
        }
    }
    
    /**
     * Tool: getProductProfitMargin - Retrieves product financial data
     */
    private Map<String, Object> executeGetProductProfitMargin(Map<String, Object> arguments) {
        String productId = (String) arguments.get("productId");
        log.info("Retrieving profit margin for product: {}", productId);
        
        try {
            CompletableFuture<com.dev.challenge.sdg.model.Product> productFuture = algoliaService.getProduct(productId);
            com.dev.challenge.sdg.model.Product product = productFuture.get();
            
            if (product == null) {
                throw new RuntimeException("Product not found: " + productId);
            }
            
            // Calculate profit margin
            double price = product.getPrice().doubleValue();
            double cost = price * 0.7; // Assume 30% cost ratio if not available
            double profitMargin = product.getProfitMargin() != null ? product.getProfitMargin() : (price - cost) / price;
            
            Map<String, Object> result = new HashMap<>();
            result.put("productId", productId);
            result.put("productName", product.getName());
            result.put("price", price);
            result.put("cost", cost);
            result.put("profitMargin", profitMargin);
            result.put("inventory", product.getInventoryLevel() != null ? product.getInventoryLevel() : 100);
            result.put("category", product.getCategory());
            
            log.info("Retrieved profit margin for product: {} = {}", productId, profitMargin);
            return result;
            
        } catch (Exception e) {
            log.error("Error retrieving product profit margin", e);
            throw new RuntimeException("Failed to get product data: " + e.getMessage());
        }
    }
    
    /**
     * Tool: generateSmartDiscount - Creates AI-powered discount offer
     */
    private Map<String, Object> executeGenerateSmartDiscount(Map<String, Object> arguments) {
        String userId = (String) arguments.get("userId");
        String productId = (String) arguments.get("productId");
        @SuppressWarnings("unchecked")
        Map<String, Object> behaviorSummary = (Map<String, Object>) arguments.get("behaviorSummary");
        double profitMargin = ((Number) arguments.get("profitMargin")).doubleValue();
        
        log.info("Generating smart discount for user: {} and product: {}", userId, productId);
        
        try {
            // For now, create a simple discount without calling Gemini API
            // TODO: Implement proper Gemini integration with correct method signature
            
            // Create discount data
            Map<String, Object> discountData = createDefaultDiscountData(behaviorSummary, profitMargin);
            
            // Create Discount object
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);
            com.dev.challenge.sdg.model.Discount discount = com.dev.challenge.sdg.model.Discount.builder()
                .userId(userId)
                .productId(productId)
                .type((String) discountData.get("type"))
                .value((Double) discountData.get("value"))
                .message((String) discountData.get("message"))
                .createdAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .expiresInSeconds(1800)
                .active(true)
                .build();
            
            // Generate discount code
            String discountCode = discountService.generateUniqueDiscountCode(userId, discount);
            discount.setCode(discountCode);
            
            // Store the discount
            discountService.storeActiveDiscount(discount);
            
            log.info("Generated smart discount: {} for user: {}", discountCode, userId);
            
            // Convert Discount object back to Map for MCP response
            Map<String, Object> result = new HashMap<>();
            result.put("code", discount.getCode());
            result.put("userId", discount.getUserId());
            result.put("productId", discount.getProductId());
            result.put("type", discount.getType());
            result.put("value", discount.getValue());
            result.put("message", discount.getMessage());
            result.put("urgencyText", discountData.get("urgencyText"));
            result.put("expiresAt", discount.getExpiresAt());
            result.put("expiresInSeconds", discount.getExpiresInSeconds());
            result.put("active", discount.isActive());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error generating smart discount", e);
            throw new RuntimeException("Failed to generate discount: " + e.getMessage());
        }
    }
    
    /**
     * Tool: logDiscountConversion - Tracks conversion events
     */
    private Map<String, Object> executeLogDiscountConversion(Map<String, Object> arguments) {
        String discountCode = (String) arguments.get("discountCode");
        String userId = (String) arguments.get("userId");
        String conversionStatus = (String) arguments.get("conversionStatus");
        
        log.info("Logging discount conversion: code={}, user={}, status={}", 
                discountCode, userId, conversionStatus);
        
        try {
            Map<String, Object> conversionLog = new HashMap<>();
            conversionLog.put("discountCode", discountCode);
            conversionLog.put("userId", userId);
            conversionLog.put("conversionStatus", conversionStatus);
            conversionLog.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            conversionLog.put("eventType", "discount_conversion");
            
            // Create UserEvent for conversion tracking
            UserEvent conversionEvent = UserEvent.builder()
                    .objectId(UUID.randomUUID().toString())
                    .userId(userId)
                    .eventType("discount_conversion")
                    .productId(null) // Not applicable for conversion events
                    .query(null)
                    .timestamp(LocalDateTime.now())
                    .details(Map.of(
                            "discountCode", discountCode,
                            "conversionStatus", conversionStatus,
                            "eventType", "discount_conversion"
                    ))
                    .build();
            
            // Store conversion event in Algolia
            algoliaService.storeUserEvent(conversionEvent);
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "logged");
            result.put("conversionId", conversionLog.get("objectID"));
            result.put("timestamp", conversionLog.get("timestamp"));
            
            log.info("Successfully logged discount conversion: {}", discountCode);
            return result;
            
        } catch (Exception e) {
            log.error("Error logging discount conversion", e);
            throw new RuntimeException("Failed to log conversion: " + e.getMessage());
        }
    }
    
    // Helper methods
    
    /**
     * Converts UserEvent object to Map format for analysis
     */
    private Map<String, Object> convertUserEventToMap(com.dev.challenge.sdg.model.UserEvent userEvent) {
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("objectId", userEvent.getObjectId());
        eventMap.put("userId", userEvent.getUserId());
        eventMap.put("eventType", userEvent.getEventType());
        eventMap.put("timestamp", userEvent.getTimestamp());
        eventMap.put("productId", userEvent.getProductId());
        eventMap.put("query", userEvent.getQuery());
        eventMap.put("details", userEvent.getDetails());
        return eventMap;
    }
    
    /**
     * Creates default discount data based on behavior analysis and profit margin
     */
    private Map<String, Object> createDefaultDiscountData(Map<String, Object> behaviorSummary, double profitMargin) {
        Map<String, Object> discountData = new HashMap<>();
        
        // Determine discount based on hesitation score and profit margin
        double hesitationScore = (Double) behaviorSummary.getOrDefault("hesitationScore", 0.0);
        int cartAbandonments = (Integer) behaviorSummary.getOrDefault("cartAbandonments", 0);
        
        // Calculate discount percentage based on behavior and profit constraints
        double discountPercentage;
        if (hesitationScore > 0.7 || cartAbandonments > 2) {
            // High hesitation - offer higher discount but respect profit margin
            discountPercentage = Math.min(20.0, profitMargin * 50); // Max 20% or half the profit margin
        } else if (hesitationScore > 0.3 || cartAbandonments > 0) {
            // Medium hesitation - moderate discount
            discountPercentage = Math.min(15.0, profitMargin * 40);
        } else {
            // Low hesitation - small discount
            discountPercentage = Math.min(10.0, profitMargin * 30);
        }
        
        // Ensure minimum discount of 5%
        discountPercentage = Math.max(5.0, discountPercentage);
        
        discountData.put("type", "percentage");
        discountData.put("value", discountPercentage);
        discountData.put("message", "Special discount based on your interest!");
        discountData.put("urgencyText", "Limited time offer - don't miss out!");
        
        return discountData;
    }
    
    private McpResponse.ToolDefinition createToolDefinition(String name, String description, 
                                                          Map<String, Object> inputSchema) {
        McpResponse.ToolDefinition tool = new McpResponse.ToolDefinition();
        tool.setName(name);
        tool.setDescription(description);
        tool.setInputSchema(inputSchema);
        return tool;
    }
    
    private Map<String, Object> analyzeBehaviorPatterns(List<Map<String, Object>> behaviorHistory) {
        Map<String, Object> analysis = new HashMap<>();
        
        int cartAbandonments = 0;
        int productViews = 0;
        int searches = 0;
        
        for (Map<String, Object> event : behaviorHistory) {
            String eventType = (String) event.get("eventType");
            switch (eventType) {
                case "cart_abandonment" -> cartAbandonments++;
                case "product_view" -> productViews++;
                case "search" -> searches++;
            }
        }
        
        analysis.put("totalEvents", behaviorHistory.size());
        analysis.put("cartAbandonments", cartAbandonments);
        analysis.put("productViews", productViews);
        analysis.put("searches", searches);
        analysis.put("hesitationScore", calculateHesitationScore(cartAbandonments, productViews));
        analysis.put("userSegment", determineUserSegment(cartAbandonments, productViews, searches));
        
        return analysis;
    }
    
    private double calculateHesitationScore(int cartAbandonments, int productViews) {
        if (productViews == 0) return 0.0;
        return Math.min(1.0, (double) cartAbandonments / productViews);
    }
    
    private String determineUserSegment(int cartAbandonments, int productViews, int searches) {
        if (cartAbandonments > 2) return "high_hesitation";
        if (productViews > 5) return "browser";
        if (searches > 3) return "searcher";
        return "casual";
    }
    
    private String buildDiscountPrompt(String userId, String productId, 
                                     Map<String, Object> behaviorSummary, double profitMargin) {
        return String.format("""
            Generate a personalized discount offer based on the following data:
            
            User ID: %s
            Product ID: %s
            User Behavior: %s
            Product Profit Margin: %.2f
            
            Create a JSON response with:
            - type: "percentage", "flat_amount", or "free_shipping"
            - value: discount amount
            - message: personalized message
            - urgencyText: urgency message
            
            Consider the user's hesitation patterns and ensure the discount is profitable.
            """, userId, productId, behaviorSummary, profitMargin);
    }
    
    private Map<String, Object> parseAiResponse(String aiResponse) {
        // Simple fallback parsing - in production, use proper JSON parsing
        Map<String, Object> discount = new HashMap<>();
        discount.put("type", "percentage");
        discount.put("value", 15.0);
        discount.put("message", "Special discount just for you!");
        discount.put("urgencyText", "Limited time offer - don't miss out!");
        return discount;
    }
}
