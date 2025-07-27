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

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    @Value("${gemini.api-key}")
    private String apiKey;
    
    @Value("${gemini.base-url}")
    private String baseUrl;
    
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
        if (behaviorSignals == null || behaviorSignals.isEmpty()) {
            prompt.append("- No specific behavior history available - user is directly requesting discount for this product\n");
            prompt.append("- This indicates strong purchase intent for the specific product\n");
        } else {
            for (UserEvent event : behaviorSignals) {
                prompt.append("- ").append(event.getEventType()).append(" at ").append(event.getTimestamp());
                if (event.getDetails() != null && !event.getDetails().isEmpty()) {
                    prompt.append(" (").append(event.getDetails()).append(")");
                }
                prompt.append("\n");
            }
        }
        
        prompt.append("\nCONSTRAINTS:\n");
        prompt.append("- Maximum discount: ").append(maxDiscountPercentage).append("%\n");
        prompt.append("- Minimum profit margin must remain: ").append(String.format("%.1f%%", minProfitMargin * 100)).append("\n\n");
        
        prompt.append("TASK:\n");
        if (behaviorSignals == null || behaviorSignals.isEmpty()) {
            prompt.append("Since the user is directly requesting a discount for this specific product, ");
            prompt.append("you should offer a discount (shouldOffer: true) to encourage purchase. ");
            prompt.append("Use the product name in both headline and message. ");
        }
        prompt.append("Based on the behavior signals, determine if a discount should be offered and generate:\n");
        prompt.append("1. Discount type: 'percentage', 'flat_amount', or 'free_shipping'\n");
        prompt.append("2. Discount value (numeric, e.g., 15 for 15% or 10 for $10)\n");
        prompt.append("3. Compelling headline including the product name (max 50 characters)\n");
        prompt.append("4. Personalized message mentioning the specific product (max 100 characters)\n");
        prompt.append("5. Brief reasoning explaining WHY this discount is offered (max 80 characters)\n\n");
        
        prompt.append("REASONING GUIDELINES:\n");
        prompt.append("- For cart abandonment: 'You left this item in your cart - here's a special offer to complete your purchase'\n");
        prompt.append("- For price hesitation: 'We noticed you're price-conscious - this discount makes it more affordable'\n");
        prompt.append("- For multiple views: 'You've shown interest in this product - here's an exclusive discount'\n");
        prompt.append("- For loyal customers: 'Thank you for being a valued customer - enjoy this special discount'\n");
        prompt.append("- For new customers: 'Welcome! Here's a discount to help you try our quality products'\n");
        prompt.append("- For direct requests: 'Based on your interest in this product, we're happy to offer this discount'\n\n");
        
        prompt.append("Respond in JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"shouldOffer\": true/false,\n");
        prompt.append("  \"type\": \"percentage|flat_amount|free_shipping\",\n");
        prompt.append("  \"value\": numeric_value,\n");
        prompt.append("  \"headline\": \"compelling headline\",\n");
        prompt.append("  \"message\": \"personalized message\",\n");
        prompt.append("  \"reasoning\": \"brief explanation why this discount is offered\"\n");
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
                .uri(baseUrl + "?key=" + apiKey)
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
                        String reasoning = discountNode.path("reasoning").asText("Personalized offer based on your activity");
                        
                        return Discount.builder()
                                .type(type)
                                .value(value)
                                .amount(formatDiscountAmount(type, value))
                                .headline(headline)
                                .message(message)
                                .reasoning(reasoning)
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
                        "text": "{\\"shouldOffer\\": true, \\"type\\": \\"percentage\\", \\"value\\": 15, \\"headline\\": \\"Special Product Offer!\\", \\"message\\": \\"Get 15% off this amazing product today!\\", \\"reasoning\\": \\"We're offering this discount to enhance your shopping experience based on your interest in this product.\\"}"
                      }]
                    }
                  }]
                }
                """;
    }
    
    private Discount getDefaultDiscount() {
        return Discount.builder()
                .type("percentage")
                .value(15.0)
                .amount("15% off")
                .headline("Special Product Offer!")
                .message("Get 15% off this amazing product today!")
                .reasoning("We're offering this discount to enhance your shopping experience and help you save on quality products.")
                .build();
    }
    
    /**
     * Generate AI chat response with product context
     */
    public CompletableFuture<String> generateChatResponse(
            String message,
            List<Map<String, Object>> chatHistory,
            List<Product> relevantProducts,
            Map<String, Object> userContext) {
        
        log.debug("Generating AI chat response for message: '{}'", message);
        
        String prompt = buildChatPrompt(message, chatHistory, relevantProducts, userContext);
        
        WebClient webClient = webClientBuilder
                .baseUrl(baseUrl)
                .build();
        
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );
        
        return webClient.post()
                .uri("/v1/models/gemini-pro:generateContent?key=" + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::extractChatResponseFromGemini)
                .doOnError(error -> log.error("Failed to generate chat response: {}", error.getMessage()))
                .onErrorReturn("I'm here to help you find great products! What are you looking for today?")
                .toFuture();
    }
    
    private String buildChatPrompt(String message, List<Map<String, Object>> chatHistory, 
                                  List<Product> relevantProducts, Map<String, Object> userContext) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are a helpful AI shopping assistant for an e-commerce platform. ");
        prompt.append("Provide friendly, informative responses to help customers find products and make purchase decisions.\n\n");
        
        // Add user context
        if (userContext != null && !userContext.isEmpty()) {
            Boolean isNewUser = (Boolean) userContext.get("newUser");
            if (Boolean.TRUE.equals(isNewUser)) {
                prompt.append("User Context: This is a new customer.\n");
            } else {
                prompt.append("User Context: Returning customer with previous shopping activity.\n");
            }
            
            @SuppressWarnings("unchecked")
            List<String> recentSearches = (List<String>) userContext.get("recentSearches");
            if (recentSearches != null && !recentSearches.isEmpty()) {
                prompt.append("Recent searches: ").append(String.join(", ", recentSearches)).append("\n");
            }
        }
        
        // Add relevant products if found
        if (relevantProducts != null && !relevantProducts.isEmpty()) {
            prompt.append("\nRelevant Products Found:\n");
            for (Product product : relevantProducts) {
                prompt.append("- ").append(product.getName())
                       .append(" ($").append(product.getPrice())
                       .append(", ").append(product.getCategory())
                       .append(", Rating: ").append(product.getAverageRating()).append("/5")
                       .append(")\n");
            }
        }
        
        // Add recent chat history for context
        if (chatHistory != null && !chatHistory.isEmpty()) {
            prompt.append("\nRecent conversation:\n");
            for (Map<String, Object> turn : chatHistory) {
                String role = (String) turn.get("role");
                String content = (String) turn.get("content");
                if (role != null && content != null) {
                    prompt.append(role).append(": ").append(content).append("\n");
                }
            }
        }
        
        prompt.append("\nCustomer message: ").append(message).append("\n\n");
        prompt.append("Provide a helpful response (max 150 words). ");
        prompt.append("If products were found, mention them naturally. ");
        prompt.append("If no products match, suggest alternatives or ask clarifying questions. ");
        prompt.append("Be conversational and helpful.");
        
        return prompt.toString();
    }
    
    private String extractChatResponseFromGemini(String response) {
        try {
            JsonNode jsonResponse = objectMapper.readTree(response);
            JsonNode candidates = jsonResponse.path("candidates");
            
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode content = firstCandidate.path("content");
                JsonNode parts = content.path("parts");
                
                if (parts.isArray() && parts.size() > 0) {
                    JsonNode firstPart = parts.get(0);
                    String text = firstPart.path("text").asText();
                    
                    if (!text.isEmpty()) {
                        return cleanupChatResponse(text);
                    }
                }
            }
            
            log.warn("No valid text found in Gemini chat response: {}", response);
            return "I'm here to help you find great products! What are you looking for today?";
            
        } catch (Exception e) {
            log.error("Failed to parse Gemini chat response: {}", e.getMessage());
            return "I'm here to help you find great products! What are you looking for today?";
        }
    }
    
    private String cleanupChatResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "I'm here to help you find great products! What are you looking for today?";
        }
        
        // Remove any unwanted formatting or markdown
        String cleaned = response.trim()
                .replaceAll("\\*\\*", "") // Remove bold markdown
                .replaceAll("\\*", "")    // Remove italic markdown
                .replaceAll("#+\\s*", "") // Remove header markdown
                .replaceAll("```[\\s\\S]*?```", "") // Remove code blocks
                .trim();
        
        // Ensure response isn't too long
        if (cleaned.length() > 500) {
            int lastSentence = cleaned.lastIndexOf('.', 500);
            if (lastSentence > 100) {
                cleaned = cleaned.substring(0, lastSentence + 1);
            } else {
                cleaned = cleaned.substring(0, 500) + "...";
            }
        }
        
        return cleaned;
    }
}
