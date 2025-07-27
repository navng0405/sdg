package com.dev.challenge.sdg.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserEvent {
    
    @JsonProperty("objectID")
    private String objectId;
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;
    
    @JsonProperty("productId")
    private String productId;
    
    private String query;
    
    private Map<String, Object> details;
    
    public enum EventType {
        CART_ABANDON("cart_abandon"),
        PRODUCT_VIEW("product_view"),
        SEARCH_QUERY("search_query"),
        NO_RESULTS_SEARCH("no_results_search"),
        PRICE_HOVER("price_hover"),
        MULTIPLE_PRODUCT_VIEWS("multiple_product_views");
        
        private final String value;
        
        EventType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
}
