package com.dev.challenge.sdg.service;

import com.dev.challenge.sdg.model.Discount;
import com.dev.challenge.sdg.model.Product;
import com.dev.challenge.sdg.model.UserEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {
    
    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    @Value("${gemini.api-key}")
    private String apiKey;
    
    @Value("${gemini.api-url}")
    private String apiUrl;
    
    @Value("${discount.max-discount-percentage}")
    private Integer maxDiscountPercentage;
    
    @Value("${discount.min-profit-margin}")
    private Double minProfitMargin;
    
    public CompletableFuture<Discount> generateDiscountSuggestion(
            String userId, 
            List<UserEvent> behaviorSignals, 
            Product product) {
        
        log.debug("Generating discount suggestion for user: {} and product: {}", userId, product.getObjectId());
        
        String prompt = buildDiscountPrompt(userId, behaviorSignals, product);
        
        return callGeminiAPI(prompt)
                .map(this::parseDiscountResponse)
                .toFuture()
                .thenApply(discount -> {
                    // Set additional properties
                    discount.setUserId(userId);
                    discount.setProductId(product.getObjectId());
                    discount.setCreatedAt(LocalDateTime.now());
                    discount.setExpiresAt(LocalDateTime.now().plusMinutes(30));
                    discount.setExpiresInSeconds(1800);
                    discount.setActive(true);
                    
                    log.info("Generated discount suggestion: {} for user: {}", discount.getAmount(), userId);
                    return discount;
                });
    }
    
    private String buildDiscountPrompt(String userId, List<UserEvent> behaviorSignals, Product product) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI discount optimization expert for an e-commerce platform. ");
        prompt.append("Analyze the user behavior and product information to generate a personalized discount offer.\n\n");
        
        prompt.append("PRODUCT INFORMATION:\n");
        prompt.append("- Name: ").append(product.getName()).append("\n");
        prompt.append("- Price: $").append(product.getPrice()).append("\n");
        prompt.append("- Category: ").append(product.getCategory()).append("\n");
        prompt.append("- Profit Margin: ").append(String.format("%.1f%%", product.getProfitMargin() * 100)).append("\n");
        prompt.append("- Average Rating: ").append(product.getAverageRating()).append("/5\n");
        prompt.append("- Reviews: ").append(product.getNumberOfReviews()).append("\n\n");
        
        prompt.append("USER BEHAVIOR SIGNALS:\n");
        for (UserEvent event : behaviorSignals) {
            prompt.append("- ").append(event.getEventType()).append(" at ").append(event.getTimestamp());
            if (event.getDetails() != null && !event.getDetails().isEmpty()) {
                prompt.append(" (").append(event.getDetails()).append(")");
            }
            prompt.append("\n");
        }
        
        prompt.append("\nCONSTRAINTS:\n");
        prompt.append("- Maximum discount: ").append(maxDiscountPercentage).append("%\n");
        prompt.append("- Minimum profit margin must remain: ").append(String.format("%.1f%%", minProfitMargin * 100)).append("\n\n");
        
        prompt.append("TASK:\n");
        prompt.append("Based on the behavior signals, determine if a discount should be offered and generate:\n");
        prompt.append("1. Discount type: 'percentage', 'flat_amount', or 'free_shipping'\n");
        prompt.append("2. Discount value (numeric, e.g., 15 for 15% or 10 for $10)\n");
        prompt.append("3. Compelling headline (max 50 characters)\n");
        prompt.append("4. Personalized message (max 100 characters)\n\n");
        
        prompt.append("Respond in JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"shouldOffer\": true/false,\n");
        prompt.append("  \"type\": \"percentage|flat_amount|free_shipping\",\n");
        prompt.append("  \"value\": numeric_value,\n");
        prompt.append("  \"headline\": \"compelling headline\",\n");
        prompt.append("  \"message\": \"personalized message\",\n");
        prompt.append("  \"reasoning\": \"brief explanation\"\n");
        prompt.append("}");
        
        return prompt.toString();
    }
    
    private Mono<String> callGeminiAPI(String prompt) {
        WebClient webClient = webClientBuilder.build();
        
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );
        
        return webClient.post()
                .uri(apiUrl + "?key=" + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(response -> log.debug("Gemini API response: {}", response))
                .onErrorResume(error -> {
                    log.error("Error calling Gemini API", error);
                    return Mono.just(getDefaultDiscountResponse());
                });
    }
    
    private Discount parseDiscountResponse(String response) {
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode candidatesNode = rootNode.path("candidates");
            
            if (candidatesNode.isArray() && candidatesNode.size() > 0) {
                JsonNode contentNode = candidatesNode.get(0).path("content").path("parts");
                if (contentNode.isArray() && contentNode.size() > 0) {
                    String textContent = contentNode.get(0).path("text").asText();
                    
                    // Extract JSON from the text content
                    int jsonStart = textContent.indexOf("{");
                    int jsonEnd = textContent.lastIndexOf("}") + 1;
                    
                    if (jsonStart >= 0 && jsonEnd > jsonStart) {
                        String jsonContent = textContent.substring(jsonStart, jsonEnd);
                        JsonNode discountNode = objectMapper.readTree(jsonContent);
                        
                        boolean shouldOffer = discountNode.path("shouldOffer").asBoolean(false);
                        if (!shouldOffer) {
                            return null; // No discount should be offered
                        }
                        
                        String type = discountNode.path("type").asText("percentage");
                        double value = discountNode.path("value").asDouble(10.0);
                        String headline = discountNode.path("headline").asText("Special Offer!");
                        String message = discountNode.path("message").asText("Limited time discount just for you!");
                        
                        return Discount.builder()
                                .type(type)
                                .value(value)
                                .amount(formatDiscountAmount(type, value))
                                .headline(headline)
                                .message(message)
                                .build();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing Gemini response", e);
        }
        
        // Return default discount if parsing fails
        return getDefaultDiscount();
    }
    
    private String formatDiscountAmount(String type, double value) {
        switch (type) {
            case "percentage":
                return String.format("%.0f%% off", value);
            case "flat_amount":
                return String.format("$%.0f off", value);
            case "free_shipping":
                return "Free Shipping";
            default:
                return String.format("%.0f%% off", value);
        }
    }
    
    private String getDefaultDiscountResponse() {
        return """
                {
                  "candidates": [{
                    "content": {
                      "parts": [{
                        "text": "{\\"shouldOffer\\": true, \\"type\\": \\"percentage\\", \\"value\\": 10, \\"headline\\": \\"Special Offer!\\", \\"message\\": \\"Get 10% off your purchase today!\\", \\"reasoning\\": \\"Default fallback discount\\"}"
                      }]
                    }
                  }]
                }
                """;
    }
    
    private Discount getDefaultDiscount() {
        return Discount.builder()
                .type("percentage")
                .value(10.0)
                .amount("10% off")
                .headline("Special Offer!")
                .message("Get 10% off your purchase today!")
                .build();
    }
}
