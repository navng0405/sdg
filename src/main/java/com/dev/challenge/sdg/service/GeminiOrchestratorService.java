package com.dev.challenge.sdg.service;

import com.dev.challenge.sdg.dto.DiscountResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Gemini Orchestrator Service - Handles MCP-centric workflow
 * Orchestrates Gemini conversations with tool calls to Algolia MCP Server
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiOrchestratorService {
    
    private static final Logger log = LoggerFactory.getLogger(GeminiOrchestratorService.class);
    
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final McpClientService mcpClient;
    
    @Value("${gemini.api-key}")
    private String geminiApiKey;
    
    @Value("${gemini.model}")
    private String model;
    
    @Value("${gemini.base-url}")
    private String baseUrl;
    
    private WebClient webClient;
    
    /**
     * Main orchestration method - handles complete discount generation workflow
     * Uses Gemini with MCP tool definitions to call Algolia MCP Server
     */
    public CompletableFuture<DiscountResponse> orchestrateDiscountGeneration(String userId) {
        log.info("Starting MCP-centric discount generation for user: {}", userId);
        
        if (webClient == null) {
            webClient = webClientBuilder.baseUrl(baseUrl).build();
        }
        
        try {
            // Step 1: Create initial Gemini prompt with MCP tool definitions
            Map<String, Object> initialRequest = createInitialGeminiRequest(userId);
            
            // Step 2: Start conversation with Gemini
            return callGeminiWithTools(initialRequest)
                    .thenCompose(this::handleGeminiResponse)
                    .thenApply(this::parseDiscountResponse)
                    .exceptionally(throwable -> {
                        log.error("Error in discount generation orchestration", throwable);
                        return DiscountResponse.noOffer("Unable to generate discount at this time");
                    });
                    
        } catch (Exception e) {
            log.error("Failed to orchestrate discount generation", e);
            return CompletableFuture.completedFuture(
                DiscountResponse.noOffer("Service temporarily unavailable")
            );
        }
    }
    
    /**
     * Creates initial Gemini request with MCP tool definitions
     */
    private Map<String, Object> createInitialGeminiRequest(String userId) {
        String systemPrompt = String.format("""
            You are an AI assistant for a Smart Discount Generator system. Your role is to analyze user behavior 
            and generate personalized discount offers using the available MCP tools.
            
            Current user: %s
            
            Your workflow should be:
            1. First, call getUserHesitationData to analyze the user's behavior patterns
            2. Then, call getProductProfitMargin to get product financial data
            3. Finally, call generateSmartDiscount to create a personalized offer
            
            Always use the MCP tools to gather data before making decisions. Be data-driven and personalized.
            """, userId);
        
        List<Map<String, Object>> tools = createMcpToolDefinitions();
        
        Map<String, Object> content = Map.of(
            "parts", List.of(Map.of("text", systemPrompt))
        );
        
        return Map.of(
            "contents", List.of(content),
            "tools", tools,
            "toolConfig", Map.of(
                "functionCallingConfig", Map.of(
                    "mode", "AUTO"
                )
            )
        );
    }
    
    /**
     * Creates MCP tool definitions for Gemini
     */
    private List<Map<String, Object>> createMcpToolDefinitions() {
        return List.of(
            Map.of(
                "functionDeclarations", List.of(
                    Map.of(
                        "name", "getUserHesitationData",
                        "description", "Retrieves detailed user behavior data from Algolia to assess hesitation patterns",
                        "parameters", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                "userId", Map.of(
                                    "type", "string",
                                    "description", "User ID to analyze"
                                )
                            ),
                            "required", List.of("userId")
                        )
                    ),
                    Map.of(
                        "name", "getProductProfitMargin",
                        "description", "Retrieves product profit margin and inventory data from Algolia",
                        "parameters", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                "productId", Map.of(
                                    "type", "string",
                                    "description", "Product ID to query"
                                )
                            ),
                            "required", List.of("productId")
                        )
                    ),
                    Map.of(
                        "name", "generateSmartDiscount",
                        "description", "Generates AI-powered discount offer based on user behavior and product data",
                        "parameters", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                "userId", Map.of("type", "string", "description", "User ID"),
                                "productId", Map.of("type", "string", "description", "Product ID"),
                                "behaviorSummary", Map.of("type", "object", "description", "User behavior analysis"),
                                "profitMargin", Map.of("type", "number", "description", "Product profit margin"),
                                "userSegment", Map.of("type", "string", "description", "User segment classification")
                            ),
                            "required", List.of("userId", "productId", "behaviorSummary", "profitMargin")
                        )
                    ),
                    Map.of(
                        "name", "logDiscountConversion",
                        "description", "Logs discount conversion events for analytics and optimization",
                        "parameters", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                "discountCode", Map.of("type", "string", "description", "Discount code"),
                                "userId", Map.of("type", "string", "description", "User ID"),
                                "conversionStatus", Map.of("type", "string", "description", "Conversion status")
                            ),
                            "required", List.of("discountCode", "userId", "conversionStatus")
                        )
                    )
                )
            )
        );
    }
    
    /**
     * Calls Gemini API with tool definitions
     */
    private CompletableFuture<String> callGeminiWithTools(Map<String, Object> requestBody) {
        log.debug("Calling Gemini API with MCP tools");
        
        return webClient
                .post()
                .uri("/models/{model}:generateContent?key={apiKey}", model, geminiApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(response -> log.debug("Received Gemini response"))
                .doOnError(error -> log.error("Gemini API call failed: {}", error.getMessage()))
                .toFuture();
    }
    
    /**
     * Handles Gemini response and processes tool calls
     */
    private CompletableFuture<String> handleGeminiResponse(String geminiResponse) {
        try {
            JsonNode response = objectMapper.readTree(geminiResponse);
            JsonNode candidates = response.get("candidates");
            
            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                JsonNode candidate = candidates.get(0);
                JsonNode content = candidate.get("content");
                
                if (content != null) {
                    JsonNode parts = content.get("parts");
                    
                    if (parts != null && parts.isArray()) {
                        for (JsonNode part : parts) {
                            // Check if this part contains a function call
                            JsonNode functionCall = part.get("functionCall");
                            if (functionCall != null) {
                                return processFunctionCall(functionCall, geminiResponse);
                            }
                            
                            // Check for text response
                            JsonNode text = part.get("text");
                            if (text != null) {
                                return CompletableFuture.completedFuture(text.asText());
                            }
                        }
                    }
                }
            }
            
            log.warn("No valid content found in Gemini response");
            return CompletableFuture.completedFuture("No discount offer available");
            
        } catch (Exception e) {
            log.error("Error handling Gemini response", e);
            return CompletableFuture.completedFuture("Error processing response");
        }
    }
    
    /**
     * Processes function call from Gemini by forwarding to MCP Server
     */
    private CompletableFuture<String> processFunctionCall(JsonNode functionCall, String originalResponse) {
        try {
            String functionName = functionCall.get("name").asText();
            JsonNode args = functionCall.get("args");
            
            log.info("Processing Gemini function call: {}", functionName);
            
            // Convert args to Map
            Map<String, Object> parameters = objectMapper.convertValue(args, Map.class);
            
            // Forward tool call to MCP Server
            return mcpClient.callMcpTool(functionName, parameters)
                    .thenCompose(toolResult -> {
                        // Send tool result back to Gemini for final response
                        return sendToolResultToGemini(functionName, toolResult, originalResponse);
                    });
                    
        } catch (Exception e) {
            log.error("Error processing function call", e);
            return CompletableFuture.completedFuture("Error executing tool");
        }
    }
    
    /**
     * Sends tool result back to Gemini for final natural language response
     */
    private CompletableFuture<String> sendToolResultToGemini(String toolName, Map<String, Object> toolResult, String originalResponse) {
        try {
            Map<String, Object> toolResponse = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(
                        Map.of("functionResponse", Map.of(
                            "name", toolName,
                            "response", toolResult
                        ))
                    ))
                )
            );
            
            return callGeminiWithTools(toolResponse)
                    .thenCompose(this::handleGeminiResponse);
                    
        } catch (Exception e) {
            log.error("Error sending tool result to Gemini", e);
            return CompletableFuture.completedFuture("Error processing tool result");
        }
    }
    
    /**
     * Parses final Gemini response into DiscountResponse
     */
    private DiscountResponse parseDiscountResponse(String finalResponse) {
        try {
            // Try to parse as JSON first
            if (finalResponse.trim().startsWith("{")) {
                JsonNode json = objectMapper.readTree(finalResponse);
                
                return DiscountResponse.withOffer(
                    json.get("discountCode").asText(),
                    json.get("discountType").asText(),
                    json.get("discountValue").asDouble(),
                    json.get("message").asText(),
                    json.get("expiresInSeconds").asInt()
                );
            }
            
            // Fallback: treat as plain text message
            return DiscountResponse.noOffer(finalResponse);
            
        } catch (Exception e) {
            log.error("Error parsing discount response", e);
            return DiscountResponse.noOffer("Generated discount offer");
        }
    }
    
    // Additional methods for other API Gateway endpoints
    
    public CompletableFuture<Void> forwardUserBehaviorToMcp(Map<String, Object> behaviorData) {
        return mcpClient.callMcpTool("storeUserEvent", behaviorData)
                .thenApply(result -> null);
    }
    
    public CompletableFuture<Boolean> validateDiscountCode(String code, String userId) {
        Map<String, Object> params = Map.of("discountCode", code, "userId", userId);
        return mcpClient.callMcpTool("validateDiscount", params)
                .thenApply(result -> (Boolean) result.get("valid"));
    }
    
    public CompletableFuture<Boolean> applyDiscountCode(String code, String userId) {
        Map<String, Object> params = Map.of("discountCode", code, "userId", userId);
        return mcpClient.callMcpTool("applyDiscount", params)
                .thenApply(result -> (Boolean) result.get("applied"));
    }
    
    public CompletableFuture<Void> logDiscountConversion(String code, String userId, String status) {
        Map<String, Object> params = Map.of("discountCode", code, "userId", userId, "conversionStatus", status);
        return mcpClient.callMcpTool("logDiscountConversion", params)
                .thenApply(result -> null);
    }
}
