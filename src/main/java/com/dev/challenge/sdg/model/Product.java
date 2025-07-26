package com.dev.challenge.sdg.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    
    @JsonProperty("objectID")
    private String objectId;
    
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    
    @JsonProperty("profit_margin")
    private Double profitMargin;
    
    @JsonProperty("inventory_level")
    private Integer inventoryLevel;
    
    @JsonProperty("image_url")
    private String imageUrl;
    
    @JsonProperty("average_rating")
    private Double averageRating;
    
    @JsonProperty("number_of_reviews")
    private Integer numberOfReviews;
}
