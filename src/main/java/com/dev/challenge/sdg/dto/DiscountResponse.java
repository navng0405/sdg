package com.dev.challenge.sdg.dto;

import com.dev.challenge.sdg.model.Discount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountResponse {
    
    private String status; // "offer_generated" or "no_offer"
    private String message;
    private Discount discount;
    
    public static DiscountResponse offerGenerated(Discount discount) {
        return DiscountResponse.builder()
                .status("offer_generated")
                .discount(discount)
                .build();
    }
    
    public static DiscountResponse noOffer(String message) {
        return DiscountResponse.builder()
                .status("no_offer")
                .message(message)
                .build();
    }
}
