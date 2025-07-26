package com.dev.challenge.mcp.service;

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
    private String geminiApiKey;
    
    @Value("${gemini.model}")
    private String model;
    
    @Value("${gemini.base-url}")
    private String baseUrl;
    
    private WebClient webClient;
    
    /**
     * Generates discount offer using Gemini AI based on user behavior and context
     */
    public CompletableFuture<String> generateDiscountOffer(String prompt) {
        log.debug("Generating discount offer with Gemini AI");
        
        if (webClient == null) {
            webClient = webClientBuilder
                    .baseUrl(baseUrl)
                    .build();
        }
        
        try {
            Map<String, Object> requestBody = Map.of(
                "contents", Map.of(
                    "parts", Map.of(
                        "text", prompt
                    )
                )
            );
            
            Mono<String> responseMono = webClient
                    .post()
                    .uri("/models/{model}:generateContent?key={apiKey}", model, geminiApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnNext(response -> log.debug("Gemini API response received"))
                    .doOnError(error -> log.error("Gemini API call failed: {}", error.getMessage()));
            
            return responseMono.toFuture()
                    .thenApply(this::extractTextFromResponse)
                    .exceptionally(throwable -> {
                        log.error("Error calling Gemini API", throwable);
                        return createFallbackDiscountResponse();
                    });
                    
        } catch (Exception e) {
            log.error("Failed to call Gemini API", e);
            return CompletableFuture.completedFuture(createFallbackDiscountResponse());
        }
    }
    
    /**
     * Extracts text content from Gemini API response
     */
    private String extractTextFromResponse(String response) {
        try {
            JsonNode jsonResponse = objectMapper.readTree(response);
            JsonNode candidates = jsonResponse.get("candidates");
            
            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode content = firstCandidate.get("content");
                
                if (content != null) {
                    JsonNode parts = content.get("parts");
                    if (parts != null && parts.isArray() && parts.size() > 0) {
                        JsonNode firstPart = parts.get(0);
                        JsonNode text = firstPart.get("text");
                        
                        if (text != null) {
                            String extractedText = text.asText();
                            log.info("Successfully extracted text from Gemini response");
                            return extractedText;
                        }
                    }
                }
            }
            
            log.warn("Could not extract text from Gemini response, using fallback");
            return createFallbackDiscountResponse();
            
        } catch (Exception e) {
            log.error("Error parsing Gemini response", e);
            return createFallbackDiscountResponse();
        }
    }
    
    /**
     * Creates a fallback discount response when Gemini API fails
     */
    private String createFallbackDiscountResponse() {
        return """
            {
                "type": "percentage",
                "value": 15,
                "message": "Special discount just for you!",
                "urgencyText": "Limited time offer - don't miss out!"
            }
            """;
    }
}
