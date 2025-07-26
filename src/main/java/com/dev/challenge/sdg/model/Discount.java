package com.dev.challenge.sdg.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Integer expiresInSeconds;
    private boolean active;
    private String productId;
    
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
