package com.dev.challenge.sdg.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Discount {
    
    private String code;
    private String userId;
    private String type; // "percentage", "flat_amount", "free_shipping"
    private String amount; // "15% off", "$10 off", "Free Shipping"
    private Double value; // numeric value for calculations
    private String headline;
    private String message;
    private String reasoning; // AI-generated explanation for why this discount is offered
    private String description; // Enhanced AI-driven description
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Integer expiresInSeconds;
    private boolean active;
    private String productId;
    
    // Profit Protection fields
    private boolean profitProtected; // Whether this discount was adjusted by profit protection
    private Double originalRequestedDiscount; // Original discount percentage before profit protection
    private String protectionMessage; // Message explaining profit protection adjustment
    private Double percentage; // Discount percentage for easier access
    
    // AI Enhancement fields
    private boolean urgent; // Whether this discount should be presented with urgency
    private List<String> crossSellSuggestions; // AI-suggested cross-sell products
    private Double aiConfidenceScore; // AI confidence in the discount recommendation (0.0-1.0)
    private String marketInsight; // AI-generated market analysis insight
    
    public enum DiscountType {
        PERCENTAGE("percentage"),
        FLAT_AMOUNT("flat_amount"),
        FREE_SHIPPING("free_shipping");
        
        private final String value;
        
        DiscountType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
}
