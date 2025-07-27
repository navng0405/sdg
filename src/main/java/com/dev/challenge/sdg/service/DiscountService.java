package com.dev.challenge.sdg.service;

import com.dev.challenge.sdg.model.Discount;
import com.dev.challenge.sdg.model.Product;
import com.dev.challenge.sdg.model.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountService {

    private final AlgoliaService algoliaService;
    private final GeminiService geminiService;
    private final ProfitProtectionService profitProtectionService;
    private final McpDataEnrichmentService mcpDataEnrichmentService;
    
    @Value("${discount.default-expiry-minutes}")
    private Integer defaultExpiryMinutes;
    
    // In-memory storage for active discounts (in production, use Redis or database)
    private final Map<String, Discount> activeDiscounts = new ConcurrentHashMap<>();
    
    public CompletableFuture<Discount> generatePersonalizedDiscount(String userId) {
        return generatePersonalizedDiscount(userId, null);
    }
    
    public CompletableFuture<Discount> generatePersonalizedDiscount(String userId, String productId) {
        log.debug("Generating personalized discount for user: {} and product: {}", userId, productId);
        
        return algoliaService.getUserBehaviorHistory(userId, 20)
                .thenCompose(behaviorHistory -> {
                    // If a specific product is requested, use it
                    if (productId != null && !productId.trim().isEmpty()) {
                        return algoliaService.getProduct(productId)
                                .thenCompose(product -> {
                                    if (product == null) {
                                        log.warn("Product not found: {}", productId);
                                        return CompletableFuture.completedFuture(null);
                                    }
                                    
                                    // Generate discount for the specific product with profit protection
                                    return generateDiscountWithProfitProtection(userId, behaviorHistory, product)
                                            .thenApply(discount -> {
                                                if (discount != null) {
                                                    // Generate unique discount code
                                                    String discountCode = generateUniqueDiscountCode(userId, discount);
                                                    discount.setCode(discountCode);
                                                    
                                                    // Store active discount
                                                    storeActiveDiscount(discount);
                                                    
                                                    log.info("Generated personalized discount: {} for user: {} and product: {}", 
                                                            discountCode, userId, productId);
                                                }
                                                return discount;
                                            });
                                });
                    }
                    
                    // Original logic for behavior-based discount generation
                    if (behaviorHistory.isEmpty()) {
                        log.info("No behavior history found for user: {}", userId);
                        return CompletableFuture.completedFuture(null);
                    }
                    
                    // Analyze behavior to determine if discount should be offered
                    if (!shouldOfferDiscount(behaviorHistory)) {
                        log.info("Discount criteria not met for user: {}", userId);
                        return CompletableFuture.completedFuture(null);
                    }
                    
                    // Get the most relevant product from recent behavior
                    return getRelevantProduct(behaviorHistory)
                            .thenCompose(product -> {
                                if (product == null) {
                                    return CompletableFuture.completedFuture(null);
                                }
                                
                                return generateDiscountWithProfitProtection(userId, behaviorHistory, product)
                                        .thenApply(discount -> {
                                            if (discount != null) {
                                                // Generate unique discount code
                                                String discountCode = generateUniqueDiscountCode(userId, discount);
                                                discount.setCode(discountCode);
                                                
                                                // Store active discount
                                                storeActiveDiscount(discount);
                                                
                                                log.info("Generated personalized discount: {} for user: {}", 
                                                        discountCode, userId);
                                            }
                                            return discount;
                                        });
                            });
                });
    }
    
    private boolean shouldOfferDiscount(List<UserEvent> behaviorHistory) {
        // Analyze behavior patterns to determine if discount should be offered
        long cartAbandonments = behaviorHistory.stream()
                .filter(event -> UserEvent.EventType.CART_ABANDON.getValue().equals(event.getEventType()))
                .count();
        
        long priceHovers = behaviorHistory.stream()
                .filter(event -> UserEvent.EventType.PRICE_HOVER.getValue().equals(event.getEventType()))
                .count();
        
        long multipleViews = behaviorHistory.stream()
                .filter(event -> UserEvent.EventType.MULTIPLE_PRODUCT_VIEWS.getValue().equals(event.getEventType()))
                .count();
        
        long noResultsSearches = behaviorHistory.stream()
                .filter(event -> UserEvent.EventType.NO_RESULTS_SEARCH.getValue().equals(event.getEventType()))
                .count();
        
        // Offer discount if user shows hesitation signals
        boolean hasHesitationSignals = cartAbandonments > 0 || priceHovers >= 2 || multipleViews >= 3;
        boolean hasSearchFrustration = noResultsSearches > 0;
        
        log.debug("Discount eligibility - Cart abandonments: {}, Price hovers: {}, Multiple views: {}, No results: {}", 
                cartAbandonments, priceHovers, multipleViews, noResultsSearches);
        
        return hasHesitationSignals || hasSearchFrustration;
    }
    
    private CompletableFuture<Product> getRelevantProduct(List<UserEvent> behaviorHistory) {
        // Find the most recent product interaction
        return behaviorHistory.stream()
                .filter(event -> event.getProductId() != null)
                .findFirst()
                .map(event -> algoliaService.getProduct(event.getProductId()))
                .orElse(CompletableFuture.completedFuture(null));
    }
    
    public String generateUniqueDiscountCode(String userId, Discount discount) {
        String prefix = discount.getType().equals("percentage") ? "SAVE" : 
                       discount.getType().equals("flat_amount") ? "OFF" : "SHIP";
        
        String uniqueId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String userSuffix = userId.substring(Math.max(0, userId.length() - 3)).toUpperCase();
        
        return String.format("%s%s-%s", prefix, Math.round(discount.getValue()), userSuffix);
    }
    
    public void storeActiveDiscount(Discount discount) {
        activeDiscounts.put(discount.getCode(), discount);
        log.debug("Stored active discount: {}", discount.getCode());
        
        // Schedule cleanup of expired discounts (simplified approach)
        scheduleDiscountCleanup(discount.getCode(), discount.getExpiresAt());
    }
    
    public boolean validateDiscount(String discountCode, String userId) {
        Discount discount = activeDiscounts.get(discountCode);
        
        if (discount == null) {
            log.warn("Discount code not found: {}", discountCode);
            return false;
        }
        
        if (!discount.getUserId().equals(userId)) {
            log.warn("Discount code {} does not belong to user: {}", discountCode, userId);
            return false;
        }
        
        if (!discount.isActive()) {
            log.warn("Discount code {} is not active", discountCode);
            return false;
        }
        
        if (discount.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Discount code {} has expired", discountCode);
            discount.setActive(false);
            return false;
        }
        
        log.info("Discount code {} validated successfully for user: {}", discountCode, userId);
        return true;
    }
    
    public Discount getActiveDiscount(String discountCode) {
        return activeDiscounts.get(discountCode);
    }
    
    public void markDiscountAsUsed(String discountCode) {
        Discount discount = activeDiscounts.get(discountCode);
        if (discount != null) {
            discount.setActive(false);
            log.info("Marked discount as used: {}", discountCode);
        }
    }
    
    private void scheduleDiscountCleanup(String discountCode, LocalDateTime expiresAt) {
        // In a production environment, you would use a proper scheduler like @Scheduled or Quartz
        // For now, we'll use a simple approach
        CompletableFuture.runAsync(() -> {
            try {
                long delayMillis = java.time.Duration.between(LocalDateTime.now(), expiresAt).toMillis();
                if (delayMillis > 0) {
                    Thread.sleep(delayMillis);
                    activeDiscounts.remove(discountCode);
                    log.debug("Cleaned up expired discount: {}", discountCode);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Discount cleanup interrupted for: {}", discountCode);
            }
        });
    }
    
    public Map<String, Discount> getAllActiveDiscounts() {
        return Map.copyOf(activeDiscounts);
    }
    
    public void clearExpiredDiscounts() {
        LocalDateTime now = LocalDateTime.now();
        activeDiscounts.entrySet().removeIf(entry -> {
            Discount discount = entry.getValue();
            boolean expired = discount.getExpiresAt().isBefore(now);
            if (expired) {
                log.debug("Removing expired discount: {}", entry.getKey());
            }
            return expired;
        });
    }
    
    /**
     * Generate AI-powered recommendations based on analytics data
     */
    public CompletableFuture<Map<String, Object>> generateAIRecommendations(
            Map<String, Object> searchAnalytics,
            Map<String, Object> behaviorInsights,
            Map<String, Object> productPerformance) {
        
        log.debug("Generating AI recommendations based on analytics data");
        
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> recommendations = new HashMap<>();
            List<Map<String, Object>> insights = new ArrayList<>();
            
            // Analyze search success rate
            Double searchSuccessRate = (Double) searchAnalytics.get("searchSuccessRate");
            if (searchSuccessRate != null && searchSuccessRate < 80.0) {
                insights.add(Map.of(
                        "type", "search_optimization",
                        "priority", "high",
                        "title", "Improve Search Experience",
                        "description", "Search success rate is " + searchSuccessRate.intValue() + "%. Consider improving product tagging and search algorithms.",
                        "action", "Review zero-result queries and enhance product metadata"
                ));
            }
            
            // Analyze conversion rate
            Double conversionRate = (Double) behaviorInsights.get("conversionRate");
            if (conversionRate != null && conversionRate < 5.0) {
                insights.add(Map.of(
                        "type", "conversion_optimization",
                        "priority", "medium",
                        "title", "Boost Conversion Rate",
                        "description", "Current conversion rate is " + conversionRate.intValue() + "%. Consider implementing dynamic pricing or targeted promotions.",
                        "action", "Launch personalized discount campaigns for cart abandoners"
                ));
            }
            
            // Analyze product performance
            Long totalViewsLong = (Long) productPerformance.get("totalProductViews");
            int totalViews = totalViewsLong != null ? totalViewsLong.intValue() : 0;
            if (totalViews > 1000) {
                insights.add(Map.of(
                        "type", "inventory_management",
                        "priority", "low",
                        "title", "High Engagement Products",
                        "description", "Strong product engagement with " + totalViews + " views. Consider expanding inventory for top performers.",
                        "action", "Increase stock levels for high-performing products"
                ));
            }
            
            // User engagement insights
            Object totalEventsObj = behaviorInsights.get("totalEvents");
            int totalEvents = totalEventsObj instanceof Integer ? (Integer) totalEventsObj : 
                             totalEventsObj instanceof Long ? ((Long) totalEventsObj).intValue() : 0;
            if (totalEvents > 0) {
                if (totalEvents < 100) {
                    insights.add(Map.of(
                            "type", "engagement",
                            "priority", "high",
                            "title", "Low User Engagement",
                            "description", "Limited user activity detected. Focus on user acquisition and engagement strategies.",
                            "action", "Implement welcome campaigns and onboarding flows"
                    ));
                } else if (totalEvents > 500) {
                    insights.add(Map.of(
                            "type", "engagement",
                            "priority", "low",
                            "title", "High User Engagement",
                            "description", "Excellent user engagement! Consider premium features or loyalty programs.",
                            "action", "Launch VIP customer program"
                    ));
                }
            }
            
            // Generate overall score
            int overallScore = calculateOverallScore(searchAnalytics, behaviorInsights, productPerformance);
            
            recommendations.put("insights", insights);
            recommendations.put("overallScore", overallScore);
            recommendations.put("scoreCategory", getScoreCategory(overallScore));
            recommendations.put("nextSteps", generateNextSteps(insights));
            recommendations.put("generatedAt", Instant.now().toString());
            
            log.info("Generated {} AI recommendations with overall score: {}", insights.size(), overallScore);
            return recommendations;
        });
    }
    
    /**
     * Personalize search results based on user behavior
     */
    public CompletableFuture<List<Product>> personalizeSearchResults(List<Product> searchResults, String userId) {
        log.debug("Personalizing search results for user: {}", userId);
        
        if (userId == null || searchResults.isEmpty()) {
            return CompletableFuture.completedFuture(searchResults);
        }
        
        return algoliaService.getUserBehaviorHistory(userId, 50)
                .thenApply(userEvents -> {
                    if (userEvents.isEmpty()) {
                        return searchResults;
                    }
                    
                    // Get user's preferred categories from behavior
                    Map<String, Long> categoryPreferences = userEvents.stream()
                            .filter(event -> event.getProductId() != null)
                            .collect(java.util.stream.Collectors.groupingBy(
                                    event -> getCategoryFromProductId(event.getProductId()),
                                    java.util.stream.Collectors.counting()));
                    
                    // Sort results by user preferences
                    return searchResults.stream()
                            .sorted((p1, p2) -> {
                                Long p1Score = categoryPreferences.getOrDefault(p1.getCategory(), 0L);
                                Long p2Score = categoryPreferences.getOrDefault(p2.getCategory(), 0L);
                                
                                // Primary sort by category preference
                                int categoryComparison = Long.compare(p2Score, p1Score);
                                if (categoryComparison != 0) {
                                    return categoryComparison;
                                }
                                
                                // Secondary sort by rating
                                return Double.compare(p2.getAverageRating(), p1.getAverageRating());
                            })
                            .collect(java.util.stream.Collectors.toList());
                });
    }
    
    /**
     * Generate AI chat response with context
     */
    public CompletableFuture<Map<String, Object>> generateAIChatResponse(
            String message,
            List<Map<String, Object>> chatHistory,
            List<Product> relevantProducts,
            Map<String, Object> userContext) {
        
        log.debug("Generating AI chat response for message: '{}'", message);
        
        return geminiService.generateChatResponse(message, chatHistory, relevantProducts, userContext)
                .thenApply(aiResponse -> {
                    Map<String, Object> response = new HashMap<>();
                    
                    // Determine response type based on message content
                    String responseType = determineResponseType(message, relevantProducts);
                    
                    response.put("message", aiResponse);
                    response.put("type", responseType);
                    response.put("confidence", calculateResponseConfidence(message, relevantProducts));
                    response.put("suggestedActions", generateSuggestedActions(message, relevantProducts));
                    
                    return response;
                })
                .exceptionally(throwable -> {
                    log.error("Failed to generate AI chat response: {}", throwable.getMessage());
                    return createFallbackChatResponse(message, relevantProducts);
                });
    }
    
    // Helper methods
    private int calculateOverallScore(Map<String, Object> searchAnalytics, 
                                     Map<String, Object> behaviorInsights, 
                                     Map<String, Object> productPerformance) {
        int score = 50; // Base score
        
        // Search analytics contribution (30%)
        Double searchSuccessRate = (Double) searchAnalytics.get("searchSuccessRate");
        if (searchSuccessRate != null) {
            score += (int) (searchSuccessRate * 0.3);
        }
        
        // Conversion rate contribution (40%)
        Double conversionRate = (Double) behaviorInsights.get("conversionRate");
        if (conversionRate != null) {
            score += Math.min(40, (int) (conversionRate * 4)); // Cap at 40 points
        }
        
        // Engagement contribution (30%)
        Object totalEventsObj = behaviorInsights.get("totalEvents");
        int totalEvents = totalEventsObj instanceof Integer ? (Integer) totalEventsObj : 
                         totalEventsObj instanceof Long ? ((Long) totalEventsObj).intValue() : 0;
        if (totalEvents > 0) {
            score += Math.min(30, totalEvents / 10); // 1 point per 10 events, cap at 30
        }
        
        return Math.min(100, Math.max(0, score)); // Ensure score is between 0-100
    }
    
    private String getScoreCategory(int score) {
        if (score >= 80) return "excellent";
        if (score >= 60) return "good";
        if (score >= 40) return "average";
        return "needs_improvement";
    }
    
    private List<String> generateNextSteps(List<Map<String, Object>> insights) {
        return insights.stream()
                .filter(insight -> "high".equals(insight.get("priority")))
                .map(insight -> (String) insight.get("action"))
                .limit(3)
                .collect(java.util.stream.Collectors.toList());
    }
    
    private String getCategoryFromProductId(String productId) {
        // Simple mapping - in a real app, you'd look this up from the product database
        if (productId != null) {
            if (productId.contains("PROD001") || productId.contains("PROD003")) return "Electronics";
            if (productId.contains("PROD002")) return "Sports";
        }
        return "General";
    }
    
    private String determineResponseType(String message, List<Product> relevantProducts) {
        String lowerMessage = message.toLowerCase();
        
        if (lowerMessage.contains("help") || lowerMessage.contains("assist")) {
            return "help";
        } else if (lowerMessage.contains("search") || lowerMessage.contains("find")) {
            return "search";
        } else if (lowerMessage.contains("recommend") || lowerMessage.contains("suggest")) {
            return "recommendation";
        } else if (!relevantProducts.isEmpty()) {
            return "product_info";
        } else {
            return "general";
        }
    }
    
    private double calculateResponseConfidence(String message, List<Product> relevantProducts) {
        double confidence = 0.5; // Base confidence
        
        if (!relevantProducts.isEmpty()) {
            confidence += 0.3; // Higher confidence with relevant products
        }
        
        if (message.length() > 10) {
            confidence += 0.2; // Higher confidence with detailed messages
        }
        
        return Math.min(1.0, confidence);
    }
    
    private List<String> generateSuggestedActions(String message, List<Product> relevantProducts) {
        List<String> actions = new ArrayList<>();
        
        if (!relevantProducts.isEmpty()) {
            actions.add("View product details");
            actions.add("Add to cart");
            actions.add("Compare products");
        }
        
        if (message.toLowerCase().contains("discount") || message.toLowerCase().contains("sale")) {
            actions.add("Check for available discounts");
        }
        
        actions.add("Continue browsing");
        return actions;
    }
    
    private Map<String, Object> createFallbackChatResponse(String message, List<Product> relevantProducts) {
        String fallbackMessage;
        
        if (!relevantProducts.isEmpty()) {
            fallbackMessage = "I found some products that might interest you! Let me know if you'd like more details about any of them.";
        } else {
            fallbackMessage = "I'm here to help you find the perfect products. Could you tell me more about what you're looking for?";
        }
        
        return Map.of(
                "message", fallbackMessage,
                "type", "fallback",
                "confidence", 0.7,
                "suggestedActions", List.of("Browse catalog", "Search products", "Get recommendations")
        );
    }
    
    /**
     * 🚀 Generates a discount with MCP-powered AI enhancement and profit protection validation
     */
    private CompletableFuture<Discount> generateDiscountWithProfitProtection(String userId, List<UserEvent> behaviorHistory, Product product) {
        log.info("🎯 Generating AI-enhanced discount for product: {} and user: {}", product.getObjectId(), userId);
        
        // Step 1: Enrich product data with AI insights
        return mcpDataEnrichmentService.enrichProductData(product.getObjectId(), buildEnrichmentContext(userId, behaviorHistory))
                .thenCompose(enrichedData -> {
                    log.info("📊 Product enrichment completed for: {}", product.getObjectId());
                    
                    // Step 2: Generate AI-powered discount using enriched data
                    return generateAiEnhancedDiscount(userId, behaviorHistory, product, enrichedData);
                })
                .thenCompose(discount -> {
                    if (discount == null) {
                        return CompletableFuture.completedFuture(null);
                    }
                    
                    // Step 3: Extract discount percentage from the discount object
                    double requestedDiscountPercentage = extractDiscountPercentage(discount);
                    
                    // Step 4: Validate with profit protection
                    return profitProtectionService.validateDiscount(product.getObjectId(), requestedDiscountPercentage, userId)
                            .thenApply(protectionResult -> {
                                if (!protectionResult.isAllowed()) {
                                    log.warn("🛡️ Discount blocked by profit protection: {}", protectionResult.getMessage());
                                    
                                    // Create a reduced discount within profit limits
                                    if (protectionResult.getApprovedDiscount() > 0) {
                                        Discount adjustedDiscount = createAdjustedDiscount(discount, protectionResult);
                                        log.info("✅ Created adjusted discount: {}% instead of {}%", 
                                                protectionResult.getApprovedDiscount(), requestedDiscountPercentage);
                                        
                                        // Add profit protection info to the discount
                                        adjustedDiscount.setProfitProtected(true);
                                        adjustedDiscount.setOriginalRequestedDiscount(requestedDiscountPercentage);
                                        adjustedDiscount.setProtectionMessage(protectionResult.getMessage());
                                        
                                        return adjustedDiscount;
                                    } else {
                                        // No discount can be offered
                                        log.info("❌ No discount can be offered due to profit protection for product: {}", product.getObjectId());
                                        return null;
                                    }
                                }
                                
                                log.info("✅ AI-enhanced discount approved by profit protection: {}%", requestedDiscountPercentage);
                                return discount;
                            });
                })
                .exceptionally(ex -> {
                    log.error("🚨 Error in MCP-enhanced discount generation, falling back to standard: {}", ex.getMessage());
                    // Fallback to standard discount generation
                    return fallbackToStandardDiscount(userId, behaviorHistory, product);
                });
    }
    
    
    /**
     * 🧠 Generate AI-enhanced discount using enriched product data
     */
    private CompletableFuture<Discount> generateAiEnhancedDiscount(String userId, List<UserEvent> behaviorHistory, Product product, Map<String, Object> enrichedData) {
        return geminiService.generateDiscountSuggestion(userId, behaviorHistory, product)
                .thenApply(discount -> {
                    if (discount == null) {
                        return null;
                    }
                    
                    // Enhance discount with AI insights from enriched data
                    enhanceDiscountWithAiInsights(discount, enrichedData);
                    
                    return discount;
                });
    }
    
    /**
     * 🔧 Build context for MCP data enrichment
     */
    private Map<String, Object> buildEnrichmentContext(String userId, List<UserEvent> behaviorHistory) {
        Map<String, Object> context = new HashMap<>();
        context.put("user_id", userId);
        context.put("behavior_events", behaviorHistory.size());
        
        // Analyze behavior patterns
        long viewEvents = behaviorHistory.stream()
                .filter(event -> "view".equals(event.getEventType()))
                .count();
        long cartEvents = behaviorHistory.stream()
                .filter(event -> "add_to_cart".equals(event.getEventType()))
                .count();
        long abandonEvents = behaviorHistory.stream()
                .filter(event -> "cart_abandon".equals(event.getEventType()))
                .count();
        
        context.put("user_engagement", Map.of(
            "view_events", viewEvents,
            "cart_events", cartEvents,
            "abandon_events", abandonEvents,
            "engagement_score", calculateEngagementScore(viewEvents, cartEvents, abandonEvents)
        ));
        
        return context;
    }
    
    /**
     * 📈 Calculate user engagement score
     */
    private double calculateEngagementScore(long views, long carts, long abandons) {
        if (views == 0) return 0.0;
        
        double cartRate = (double) carts / views;
        double abandonRate = carts > 0 ? (double) abandons / carts : 0.0;
        
        return Math.max(0.0, Math.min(1.0, cartRate * (1.0 - abandonRate)));
    }
    
    /**
     * ✨ Enhance discount with AI insights
     */
    private void enhanceDiscountWithAiInsights(Discount discount, Map<String, Object> enrichedData) {
        try {
            Map<String, Object> aiInsights = (Map<String, Object>) enrichedData.get("ai_enrichment");
            if (aiInsights != null) {
                // Add AI-driven description
                String aiDescription = (String) aiInsights.get("discount_reasoning");
                if (aiDescription != null) {
                    discount.setDescription(aiDescription);
                }
                
                // Add urgency if recommended
                Boolean urgencyRecommended = (Boolean) aiInsights.get("urgency_recommended");
                if (Boolean.TRUE.equals(urgencyRecommended)) {
                    discount.setUrgent(true);
                }
                
                // Add cross-sell opportunities
                List<String> crossSell = (List<String>) aiInsights.get("cross_sell_opportunities");
                if (crossSell != null && !crossSell.isEmpty()) {
                    discount.setCrossSellSuggestions(crossSell);
                }
                
                // Store AI confidence score
                Double confidenceScore = (Double) aiInsights.get("confidence_score");
                if (confidenceScore != null) {
                    discount.setAiConfidenceScore(confidenceScore);
                }
            }
        } catch (Exception e) {
            log.warn("Could not enhance discount with AI insights: {}", e.getMessage());
        }
    }
    
    /**
     * 🛡️ Fallback to standard discount generation
     */
    private Discount fallbackToStandardDiscount(String userId, List<UserEvent> behaviorHistory, Product product) {
        try {
            return geminiService.generateDiscountSuggestion(userId, behaviorHistory, product).get();
        } catch (Exception e) {
            log.error("Fallback discount generation also failed: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extracts the discount percentage from a Discount object
     */
    private double extractDiscountPercentage(Discount discount) {
        if (discount.getPercentage() != null) {
            return discount.getPercentage();
        }
        
        // Try to parse from amount string (e.g., "15% OFF" -> 15.0)
        String amount = discount.getAmount();
        if (amount != null && amount.contains("%")) {
            try {
                return Double.parseDouble(amount.replaceAll("[^0-9.]", ""));
            } catch (NumberFormatException e) {
                log.warn("Could not parse discount percentage from amount: {}", amount);
            }
        }
        
        // Default to 10% if unable to determine
        log.warn("Unable to determine discount percentage, defaulting to 10%");
        return 10.0;
    }
    
    /**
     * Creates an adjusted discount based on profit protection limits
     */
    private Discount createAdjustedDiscount(Discount originalDiscount, ProfitProtectionService.ProfitProtectionResult protectionResult) {
        Discount adjustedDiscount = new Discount();
        
        // Copy original properties
        adjustedDiscount.setUserId(originalDiscount.getUserId());
        adjustedDiscount.setProductId(originalDiscount.getProductId());
        adjustedDiscount.setHeadline(originalDiscount.getHeadline());
        adjustedDiscount.setReasoning(originalDiscount.getReasoning() + " (Adjusted for profit protection)");
        adjustedDiscount.setExpiresAt(originalDiscount.getExpiresAt());
        adjustedDiscount.setExpiresInSeconds(originalDiscount.getExpiresInSeconds());
        
        // Adjust the discount amount
        double approvedPercentage = Math.floor(protectionResult.getApprovedDiscount() * 10) / 10; // Round down to 1 decimal
        adjustedDiscount.setPercentage(approvedPercentage);
        adjustedDiscount.setAmount(String.format("%.0f%% OFF", approvedPercentage));
        
        // Update message to reflect profit protection
        String originalMessage = originalDiscount.getMessage();
        adjustedDiscount.setMessage(originalMessage + " (Discount adjusted for sustainable pricing)");
        
        return adjustedDiscount;
    }
}
