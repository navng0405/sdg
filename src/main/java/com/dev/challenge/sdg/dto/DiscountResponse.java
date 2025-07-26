package com.dev.challenge.sdg.dto;

import com.dev.challenge.sdg.model.Discount;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscountResponse {
    
    private String status; // "offer_generated" or "no_offer"
    private String message;
    private Discount discount;
    
    public static DiscountResponse offerGenerated(Discount discount) {
        DiscountResponse response = new DiscountResponse();
        response.setStatus("offer_generated");
        response.setDiscount(discount);
        return response;
    }
    
    public static DiscountResponse noOffer(String message) {
        DiscountResponse response = new DiscountResponse();
        response.setStatus("no_offer");
        response.setMessage(message);
        return response;
    }
}
