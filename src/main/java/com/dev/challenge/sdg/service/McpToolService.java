package com.dev.challenge.sdg.service;

import com.dev.challenge.sdg.dto.McpResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * MCP Tool Service - Implements business logic for MCP tools
 * Handles all MCP tool execution with direct service injection (no HTTP calls)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpToolService {
    
    private static final Logger log = LoggerFactory.getLogger(McpToolService.class);
    
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
            CompletableFuture<List<Map<String, Object>>> behaviorFuture = 
                algoliaService.getUserBehaviorHistory(userId, 50);
            List<Map<String, Object>> behaviorHistory = behaviorFuture.get();
            
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
            CompletableFuture<Map<String, Object>> productFuture = algoliaService.getProduct(productId);
            Map<String, Object> product = productFuture.get();
            
            if (product == null) {
                throw new RuntimeException("Product not found: " + productId);
            }
            
            // Calculate profit margin
            double price = ((Number) product.getOrDefault("price", 0)).doubleValue();
            double cost = ((Number) product.getOrDefault("cost", price * 0.7)).doubleValue();
            double profitMargin = (price - cost) / price;
            
            Map<String, Object> result = new HashMap<>();
            result.put("productId", productId);
            result.put("productName", product.get("name"));
            result.put("price", price);
            result.put("cost", cost);
            result.put("profitMargin", profitMargin);
            result.put("inventory", product.getOrDefault("inventory", 100));
            result.put("category", product.get("category"));
            
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
            // Build AI prompt for discount generation
            String prompt = buildDiscountPrompt(userId, productId, behaviorSummary, profitMargin);
            
            // Get AI-generated discount suggestion
            CompletableFuture<String> aiResponseFuture = geminiService.generateDiscountOffer(prompt);
            String aiResponse = aiResponseFuture.get();
            
            // Generate discount code
            Map<String, Object> discountData = parseAiResponse(aiResponse);
            String discountCode = discountService.generateUniqueDiscountCode(userId, discountData);
            
            // Create complete discount offer
            Map<String, Object> discount = new HashMap<>();
            discount.put("code", discountCode);
            discount.put("userId", userId);
            discount.put("productId", productId);
            discount.put("type", discountData.get("type"));
            discount.put("value", discountData.get("value"));
            discount.put("message", discountData.get("message"));
            discount.put("urgencyText", discountData.get("urgencyText"));
            discount.put("expiresAt", LocalDateTime.now().plusMinutes(30));
            discount.put("expiresInSeconds", 1800);
            discount.put("active", true);
            
            // Store the discount
            discountService.storeActiveDiscount(discount);
            
            log.info("Generated smart discount: {} for user: {}", discountCode, userId);
            return discount;
            
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
            
            // Store conversion event in Algolia
            algoliaService.storeConversionEvent(conversionLog);
            
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
