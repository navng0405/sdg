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

import java.time.LocalDateTime;
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
    
    @Value("${discount.default-expiry-minutes}")
    private Integer defaultExpiryMinutes;
    
    // In-memory storage for active discounts (in production, use Redis or database)
    private final Map<String, Discount> activeDiscounts = new ConcurrentHashMap<>();
    
    public CompletableFuture<Discount> generatePersonalizedDiscount(String userId) {
        log.debug("Generating personalized discount for user: {}", userId);
        
        return algoliaService.getUserBehaviorHistory(userId, 20)
                .thenCompose(behaviorHistory -> {
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
                                
                                return geminiService.generateDiscountSuggestion(userId, behaviorHistory, product)
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
}
