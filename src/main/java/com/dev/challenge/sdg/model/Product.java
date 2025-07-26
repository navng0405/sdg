package com.dev.challenge.sdg.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore fields not in this model
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
    
    // Additional fields from your actual Algolia data
    private String brand;
    private List<String> tags;
    private Map<String, Object> specifications;
}
