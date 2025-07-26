package com.dev.challenge.mcp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountService {
    
    private static final Logger log = LoggerFactory.getLogger(DiscountService.class);
    
    @Value("${discount.default-expiry-minutes}")
    private Integer defaultExpiryMinutes;
    
    // In-memory storage for active discounts (in production, use Redis or database)
    private final Map<String, Map<String, Object>> activeDiscounts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    /**
     * Generates a unique discount code based on user ID and discount data
     */
    public String generateUniqueDiscountCode(String userId, Map<String, Object> discountData) {
        String discountType = (String) discountData.get("type");
        Double discountValue = ((Number) discountData.get("value")).doubleValue();
        
        String prefix = switch (discountType) {
            case "percentage" -> "SAVE";
            case "flat_amount" -> "OFF";
            case "free_shipping" -> "SHIP";
            default -> "DISC";
        };
        
        String uniqueId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String userSuffix = userId.substring(Math.max(0, userId.length() - 3)).toUpperCase();
        
        return String.format("%s%s-%s", prefix, Math.round(discountValue), userSuffix);
    }
    
    /**
     * Stores an active discount in memory with expiration handling
     */
    public void storeActiveDiscount(Map<String, Object> discount) {
        String discountCode = (String) discount.get("code");
        activeDiscounts.put(discountCode, discount);
        log.debug("Stored active discount: {}", discountCode);
        
        // Schedule cleanup of expired discount
        LocalDateTime expiresAt = (LocalDateTime) discount.get("expiresAt");
        if (expiresAt != null) {
            scheduleDiscountCleanup(discountCode, expiresAt);
        }
    }
    
    /**
     * Validates a discount code for a specific user
     */
    public boolean validateDiscount(String discountCode, String userId) {
        Map<String, Object> discount = activeDiscounts.get(discountCode);
        
        if (discount == null) {
            log.warn("Discount code not found: {}", discountCode);
            return false;
        }
        
        if (!discount.get("userId").equals(userId)) {
            log.warn("Discount code {} does not belong to user: {}", discountCode, userId);
            return false;
        }
        
        if (!(Boolean) discount.get("active")) {
            log.warn("Discount code {} is not active", discountCode);
            return false;
        }
        
        LocalDateTime expiresAt = (LocalDateTime) discount.get("expiresAt");
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            log.warn("Discount code {} has expired", discountCode);
            return false;
        }
        
        log.info("Discount code {} validated successfully for user: {}", discountCode, userId);
        return true;
    }
    
    /**
     * Marks a discount as used
     */
    public void markDiscountAsUsed(String discountCode) {
        Map<String, Object> discount = activeDiscounts.get(discountCode);
        if (discount != null) {
            discount.put("used", true);
            discount.put("usedAt", LocalDateTime.now());
            log.info("Marked discount as used: {}", discountCode);
        }
    }
    
    /**
     * Gets an active discount by code
     */
    public Map<String, Object> getActiveDiscount(String discountCode) {
        return activeDiscounts.get(discountCode);
    }
    
    /**
     * Schedules cleanup of expired discount
     */
    private void scheduleDiscountCleanup(String discountCode, LocalDateTime expiresAt) {
        long delayMinutes = java.time.Duration.between(LocalDateTime.now(), expiresAt).toMinutes();
        
        if (delayMinutes > 0) {
            scheduler.schedule(() -> {
                try {
                    activeDiscounts.remove(discountCode);
                    log.debug("Cleaned up expired discount: {}", discountCode);
                } catch (Exception e) {
                    log.error("Error cleaning up discount: {}", discountCode, e);
                }
            }, delayMinutes, TimeUnit.MINUTES);
        }
    }
    
    /**
     * Clears all expired discounts (maintenance method)
     */
    public void clearExpiredDiscounts() {
        LocalDateTime now = LocalDateTime.now();
        
        activeDiscounts.entrySet().removeIf(entry -> {
            Map<String, Object> discount = entry.getValue();
            LocalDateTime expiresAt = (LocalDateTime) discount.get("expiresAt");
            
            if (expiresAt != null && now.isAfter(expiresAt)) {
                log.debug("Removing expired discount: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }
}
