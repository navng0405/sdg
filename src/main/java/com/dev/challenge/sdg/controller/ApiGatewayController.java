package com.dev.challenge.sdg.controller;

import com.dev.challenge.sdg.service.GeminiOrchestratorService;
import com.dev.challenge.sdg.dto.DiscountResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * API Gateway Controller - Thin orchestrator that forwards requests to Gemini
 * and handles tool_code forwarding to the Algolia MCP Server
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ApiGatewayController {
    
    private static final Logger log = LoggerFactory.getLogger(ApiGatewayController.class);
    private final GeminiOrchestratorService geminiOrchestrator;
    
    /**
     * Main endpoint for discount generation - orchestrates Gemini conversation with MCP tools
     */
    @GetMapping("/get-discount")
    public CompletableFuture<ResponseEntity<DiscountResponse>> getPersonalizedDiscount(
            @RequestParam String userId) {
        
        log.info("Received discount request for user: {}", userId);
        
        return geminiOrchestrator.orchestrateDiscountGeneration(userId)
                .thenApply(discountResponse -> {
                    log.info("Successfully generated discount for user: {}", userId);
                    return ResponseEntity.ok(discountResponse);
                })
                .exceptionally(throwable -> {
                    log.error("Error generating discount for user: {}", userId, throwable);
                    return ResponseEntity.ok(DiscountResponse.noOffer(
                        "Unable to generate personalized discount at this time. Please try again later."
                    ));
                });
    }
    
    /**
     * User behavior tracking endpoint - forwards to MCP Server for storage
     */
    @PostMapping("/user-behavior")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> trackUserBehavior(
            @RequestBody Map<String, Object> behaviorData) {
        
        String userId = (String) behaviorData.get("userId");
        String eventType = (String) behaviorData.get("eventType");
        
        log.info("Tracking user behavior: user={}, event={}", userId, eventType);
        
        return geminiOrchestrator.forwardUserBehaviorToMcp(behaviorData)
                .thenApply(result -> {
                    log.info("Successfully tracked user behavior for user: {}", userId);
                    return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "User behavior tracked successfully"
                    ));
                })
                .exceptionally(throwable -> {
                    log.error("Error tracking user behavior for user: {}", userId, throwable);
                    return ResponseEntity.ok(Map.of(
                        "status", "error",
                        "message", "Failed to track user behavior"
                    ));
                });
    }
    
    /**
     * Discount validation endpoint
     */
    @GetMapping("/validate-discount")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> validateDiscount(
            @RequestParam String code,
            @RequestParam String userId) {
        
        log.info("Validating discount code: {} for user: {}", code, userId);
        
        return geminiOrchestrator.validateDiscountCode(code, userId)
                .thenApply(isValid -> {
                    Map<String, Object> response = Map.of(
                        "valid", isValid,
                        "code", code,
                        "userId", userId
                    );
                    return ResponseEntity.ok(response);
                })
                .exceptionally(throwable -> {
                    log.error("Error validating discount code: {}", code, throwable);
                    return ResponseEntity.ok(Map.of(
                        "valid", false,
                        "error", "Validation failed"
                    ));
                });
    }
    
    /**
     * Apply discount endpoint
     */
    @PostMapping("/apply-discount")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> applyDiscount(
            @RequestBody Map<String, Object> applyRequest) {
        
        String code = (String) applyRequest.get("code");
        String userId = (String) applyRequest.get("userId");
        
        log.info("Applying discount code: {} for user: {}", code, userId);
        
        return geminiOrchestrator.applyDiscountCode(code, userId)
                .thenApply(applied -> {
                    if (applied) {
                        // Log conversion event via MCP Server
                        geminiOrchestrator.logDiscountConversion(code, userId, "applied");
                        
                        return ResponseEntity.ok(Map.of(
                            "status", "success",
                            "message", "Discount applied successfully",
                            "code", code
                        ));
                    } else {
                        return ResponseEntity.ok(Map.of(
                            "status", "error",
                            "message", "Failed to apply discount code"
                        ));
                    }
                })
                .exceptionally(throwable -> {
                    log.error("Error applying discount code: {}", code, throwable);
                    return ResponseEntity.ok(Map.of(
                        "status", "error",
                        "message", "Failed to apply discount"
                    ));
                });
    }
}
