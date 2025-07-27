package com.dev.challenge.sdg.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class ProfitProtectionService {
    
    private static final Logger log = LoggerFactory.getLogger(ProfitProtectionService.class);
    private static final double PROFIT_PROTECTION_THRESHOLD = 0.8; // 80% of profit margin
    
    @Autowired
    private AlgoliaService algoliaService;
    
    /**
     * Validates if a discount exceeds the profit protection threshold
     * @param productId The product ID
     * @param requestedDiscountPercentage The requested discount percentage (e.g., 15 for 15%)
     * @param userId The user ID for logging
     * @return ProfitProtectionResult containing validation result and details
     */
    public CompletableFuture<ProfitProtectionResult> validateDiscount(String productId, double requestedDiscountPercentage, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Validating discount protection for product: {}, requested: {}%", productId, requestedDiscountPercentage);
                
                // Get profit margin from Algolia
                CompletableFuture<Double> profitMarginFuture = algoliaService.getProductProfitMargin(productId);
                Double profitMargin = profitMarginFuture.get();
                
                if (profitMargin == null) {
                    log.warn("No profit margin found for product: {}, allowing discount", productId);
                    return new ProfitProtectionResult(true, requestedDiscountPercentage, 0.0, "No profit margin data available");
                }
                
                // Convert profit margin to percentage if it's in decimal format
                double profitMarginPercentage = profitMargin > 1.0 ? profitMargin : profitMargin * 100;
                
                // Calculate maximum allowed discount (80% of profit margin)
                double maxAllowedDiscount = profitMarginPercentage * PROFIT_PROTECTION_THRESHOLD;
                
                boolean isAllowed = requestedDiscountPercentage <= maxAllowedDiscount;
                
                log.info("Profit protection check - Product: {}, Profit Margin: {}%, Max Allowed: {}%, Requested: {}%, Allowed: {}", 
                        productId, profitMarginPercentage, maxAllowedDiscount, requestedDiscountPercentage, isAllowed);
                
                if (!isAllowed) {
                    // Log veto decision to Algolia
                    logVetoDecision(productId, requestedDiscountPercentage, maxAllowedDiscount, userId, profitMarginPercentage);
                    
                    return new ProfitProtectionResult(false, maxAllowedDiscount, profitMarginPercentage, 
                            String.format("Discount blocked: Requested %.1f%% exceeds maximum allowed %.1f%% (80%% of %.1f%% profit margin)", 
                                    requestedDiscountPercentage, maxAllowedDiscount, profitMarginPercentage));
                }
                
                return new ProfitProtectionResult(true, requestedDiscountPercentage, profitMarginPercentage, "Discount approved");
                
            } catch (Exception e) {
                log.error("Error in profit protection validation for product: " + productId, e);
                // In case of error, allow the discount but log the issue
                return new ProfitProtectionResult(true, requestedDiscountPercentage, 0.0, "Error in profit validation - discount allowed");
            }
        });
    }
    
    /**
     * Logs a veto decision to Algolia for analytics and audit purposes
     */
    private void logVetoDecision(String productId, double requestedDiscount, double maxAllowedDiscount, String userId, double profitMargin) {
        try {
            Map<String, Object> vetoEvent = new HashMap<>();
            vetoEvent.put("objectID", "veto-" + System.currentTimeMillis() + "-" + productId);
            vetoEvent.put("eventType", "discount_veto");
            vetoEvent.put("productId", productId);
            vetoEvent.put("userId", userId);
            vetoEvent.put("requestedDiscount", requestedDiscount);
            vetoEvent.put("maxAllowedDiscount", maxAllowedDiscount);
            vetoEvent.put("profitMargin", profitMargin);
            vetoEvent.put("protectionThreshold", PROFIT_PROTECTION_THRESHOLD);
            vetoEvent.put("timestamp", LocalDateTime.now().toInstant(ZoneOffset.UTC).toString());
            vetoEvent.put("reason", "Discount exceeds profit protection threshold");
            
            // Log to Algolia asynchronously
            algoliaService.logUserEvent("veto_decisions", vetoEvent);
            
            log.info("Logged veto decision to Algolia for product: {}, user: {}", productId, userId);
            
        } catch (Exception e) {
            log.error("Failed to log veto decision to Algolia", e);
        }
    }
    
    /**
     * Gets recent veto decisions for analytics
     */
    public CompletableFuture<java.util.List<Map<String, Object>>> getRecentVetoDecisions(int limit) {
        return algoliaService.searchUserEvents("veto_decisions", "discount_veto", limit);
    }
    
    /**
     * Result class for profit protection validation
     */
    public static class ProfitProtectionResult {
        private final boolean allowed;
        private final double approvedDiscount;
        private final double profitMargin;
        private final String message;
        
        public ProfitProtectionResult(boolean allowed, double approvedDiscount, double profitMargin, String message) {
            this.allowed = allowed;
            this.approvedDiscount = approvedDiscount;
            this.profitMargin = profitMargin;
            this.message = message;
        }
        
        public boolean isAllowed() { return allowed; }
        public double getApprovedDiscount() { return approvedDiscount; }
        public double getProfitMargin() { return profitMargin; }
        public String getMessage() { return message; }
        
        @Override
        public String toString() {
            return String.format("ProfitProtectionResult{allowed=%s, approvedDiscount=%.1f%%, profitMargin=%.1f%%, message='%s'}", 
                    allowed, approvedDiscount, profitMargin, message);
        }
    }
}
