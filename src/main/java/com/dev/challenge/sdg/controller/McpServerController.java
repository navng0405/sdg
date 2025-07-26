package com.dev.challenge.sdg.controller;

import com.dev.challenge.sdg.service.AlgoliaService;
import com.dev.challenge.sdg.service.DiscountService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class McpServerController {
    
    private final AlgoliaService algoliaService;
    private final DiscountService discountService;
    private final ObjectMapper objectMapper;
    
    @Value("${mcp.server.name}")
    private String serverName;
    
    @Value("${mcp.server.version}")
    private String serverVersion;
    
    @Value("${mcp.server.description}")
    private String serverDescription;
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> handleMcpRequest(@RequestBody Map<String, Object> request) {
        log.debug("Received MCP request: {}", request);
        
        try {
            String method = (String) request.get("method");
            if (method == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("Missing method"));
            }
            
            switch (method) {
                case "initialize":
                    return ResponseEntity.ok(handleInitialize(request));
                case "tools/list":
                    return ResponseEntity.ok(handleToolsList());
                case "tools/call":
                    return ResponseEntity.ok(handleToolCall(request));
                default:
                    return ResponseEntity.badRequest().body(createErrorResponse("Unknown method: " + method));
            }
        } catch (Exception e) {
            log.error("Error handling MCP request", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("Internal server error"));
        }
    }
    
    private Map<String, Object> handleInitialize(Map<String, Object> request) {
        log.info("Initializing MCP server: {}", serverName);
        
        return Map.of(
                "protocolVersion", "2024-11-05",
                "capabilities", Map.of(
                        "tools", Map.of()
                ),
                "serverInfo", Map.of(
                        "name", serverName,
                        "version", serverVersion,
                        "description", serverDescription
                )
        );
    }
    
    private Map<String, Object> handleToolsList() {
        log.debug("Listing available MCP tools");
        
        List<Map<String, Object>> tools = List.of(
                Map.of(
                        "name", "algolia.analytics.getBehavioralSignals",
                        "description", "Retrieve user behavioral signals from Algolia analytics",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "userId", Map.of("type", "string", "description", "User ID to get behavior for"),
                                        "limit", Map.of("type", "integer", "description", "Maximum number of events to retrieve", "default", 20)
                                ),
                                "required", List.of("userId")
                        )
                ),
                Map.of(
                        "name", "product.getDetails",
                        "description", "Get detailed product information from Algolia",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "productId", Map.of("type", "string", "description", "Product ID to retrieve")
                                ),
                                "required", List.of("productId")
                        )
                ),
                Map.of(
                        "name", "discount.generate",
                        "description", "Generate a personalized discount for a user",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "userId", Map.of("type", "string", "description", "User ID to generate discount for")
                                ),
                                "required", List.of("userId")
                        )
                ),
                Map.of(
                        "name", "discount.validate",
                        "description", "Validate a discount code for a user",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "discountCode", Map.of("type", "string", "description", "Discount code to validate"),
                                        "userId", Map.of("type", "string", "description", "User ID attempting to use the discount")
                                ),
                                "required", List.of("discountCode", "userId")
                        )
                )
        );
        
        return Map.of("tools", tools);
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> handleToolCall(Map<String, Object> request) {
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        if (params == null) {
            return createErrorResponse("Missing params");
        }
        
        String toolName = (String) params.get("name");
        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
        
        if (toolName == null || arguments == null) {
            return createErrorResponse("Missing tool name or arguments");
        }
        
        log.debug("Calling MCP tool: {} with arguments: {}", toolName, arguments);
        
        try {
            switch (toolName) {
                case "algolia.analytics.getBehavioralSignals":
                    return handleGetBehavioralSignals(arguments);
                case "product.getDetails":
                    return handleGetProductDetails(arguments);
                case "discount.generate":
                    return handleGenerateDiscount(arguments);
                case "discount.validate":
                    return handleValidateDiscount(arguments);
                default:
                    return createErrorResponse("Unknown tool: " + toolName);
            }
        } catch (Exception e) {
            log.error("Error executing tool: " + toolName, e);
            return createErrorResponse("Tool execution failed: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handleGetBehavioralSignals(Map<String, Object> arguments) {
        String userId = (String) arguments.get("userId");
        Integer limit = (Integer) arguments.getOrDefault("limit", 20);
        
        try {
            var behaviorHistory = algoliaService.getUserBehaviorHistory(userId, limit).get();
            
            return Map.of(
                    "content", List.of(
                            Map.of(
                                    "type", "text",
                                    "text", objectMapper.writeValueAsString(Map.of(
                                            "userId", userId,
                                            "behaviorSignals", behaviorHistory,
                                            "totalEvents", behaviorHistory.size()
                                    ))
                            )
                    )
            );
        } catch (Exception e) {
            log.error("Error retrieving behavioral signals for user: " + userId, e);
            return createErrorResponse("Failed to retrieve behavioral signals");
        }
    }
    
    private Map<String, Object> handleGetProductDetails(Map<String, Object> arguments) {
        String productId = (String) arguments.get("productId");
        
        try {
            var product = algoliaService.getProduct(productId).get();
            
            return Map.of(
                    "content", List.of(
                            Map.of(
                                    "type", "text",
                                    "text", objectMapper.writeValueAsString(product)
                            )
                    )
            );
        } catch (Exception e) {
            log.error("Error retrieving product details for: " + productId, e);
            return createErrorResponse("Failed to retrieve product details");
        }
    }
    
    private Map<String, Object> handleGenerateDiscount(Map<String, Object> arguments) {
        String userId = (String) arguments.get("userId");
        
        try {
            var discount = discountService.generatePersonalizedDiscount(userId).get();
            
            if (discount == null) {
                return Map.of(
                        "content", List.of(
                                Map.of(
                                        "type", "text",
                                        "text", objectMapper.writeValueAsString(Map.of(
                                                "status", "no_offer",
                                                "message", "No discount generated for this user"
                                        ))
                                )
                        )
                );
            }
            
            return Map.of(
                    "content", List.of(
                            Map.of(
                                    "type", "text",
                                    "text", objectMapper.writeValueAsString(Map.of(
                                            "status", "offer_generated",
                                            "discount", discount
                                    ))
                            )
                    )
            );
        } catch (Exception e) {
            log.error("Error generating discount for user: " + userId, e);
            return createErrorResponse("Failed to generate discount");
        }
    }
    
    private Map<String, Object> handleValidateDiscount(Map<String, Object> arguments) {
        String discountCode = (String) arguments.get("discountCode");
        String userId = (String) arguments.get("userId");
        
        try {
            boolean isValid = discountService.validateDiscount(discountCode, userId);
            var discount = discountService.getActiveDiscount(discountCode);
            
            return Map.of(
                    "content", List.of(
                            Map.of(
                                    "type", "text",
                                    "text", objectMapper.writeValueAsString(Map.of(
                                            "valid", isValid,
                                            "discount", discount
                                    ))
                            )
                    )
            );
        } catch (Exception e) {
            log.error("Error validating discount: " + discountCode, e);
            return createErrorResponse("Failed to validate discount");
        }
    }
    
    private Map<String, Object> createErrorResponse(String message) {
        return Map.of(
                "error", Map.of(
                        "code", -1,
                        "message", message
                )
        );
    }
}
