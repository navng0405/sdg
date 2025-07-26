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
    
    public static DiscountResponse withOffer(String discountCode, String discountType, 
                                           double discountValue, String message, int expiresInSeconds) {
        DiscountResponse response = new DiscountResponse();
        response.setStatus("offer_generated");
        response.setMessage(message);
        
        // Create discount object
        Discount discount = new Discount();
        discount.setCode(discountCode);
        discount.setType(discountType);
        discount.setValue(discountValue);
        discount.setMessage(message);
        discount.setExpiresInSeconds(expiresInSeconds);
        
        response.setDiscount(discount);
        return response;
    }
}
