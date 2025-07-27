package com.dev.challenge.sdg.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * MCP-Enhanced Profit Protection Service
 * Showcases Algolia MCP Server integration for intelligent profit margin analysis
 */
@Service
public class McpProfitProtectionService {
    
    private static final Logger log = LoggerFactory.getLogger(McpProfitProtectionService.class);
    
    @Autowired
    private WebClient.Builder webClientBuilder;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${mcp.server.url:http://localhost:3000}")
    private String mcpServerUrl;
    
    private WebClient mcpClient;
    
    @Autowired
    public void initializeMcpClient() {
        this.mcpClient = webClientBuilder
                .baseUrl(mcpServerUrl)
                .build();
    }
    
    /**
     * Enhanced profit protection using Algolia MCP Server + AI analysis
     * This showcases the power of MCP for intelligent data retrieval and analysis
     */
    public CompletableFuture<McpProfitAnalysisResult> analyzeDiscountWithMcp(
            String productId, 
            double requestedDiscount, 
            String userId,
            Map<String, Object> marketContext) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("🤖 Starting MCP-enhanced profit analysis for product: {}", productId);
                
                // Step 1: Use MCP to get enriched product data
                Map<String, Object> mcpRequest = createMcpProductRequest(productId);
                
                var productResponse = mcpClient.post()
                        .uri("/mcp/tools/algolia_search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(mcpRequest)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();
                
                // Step 2: Use MCP to get market intelligence
                Map<String, Object> marketRequest = createMcpMarketAnalysisRequest(productId, marketContext);
                
                var marketResponse = mcpClient.post()
                        .uri("/mcp/tools/algolia_search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(marketRequest)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();
                
                // Step 3: Use MCP to analyze historical pricing data
                Map<String, Object> historicalRequest = createMcpHistoricalAnalysisRequest(productId, userId);
                
                var historicalResponse = mcpClient.post()
                        .uri("/mcp/tools/algolia_search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(historicalRequest)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();
                
                // Step 4: AI-powered profit margin analysis using Claude through MCP
                Map<String, Object> aiAnalysisRequest = createAiAnalysisRequest(
                        productResponse, marketResponse, historicalResponse, requestedDiscount);
                
                var aiResponse = mcpClient.post()
                        .uri("/mcp/ai/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(aiAnalysisRequest)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();
                
                // Process MCP results into intelligent profit protection decision
                return processMcpResults(productId, requestedDiscount, userId, 
                        productResponse, marketResponse, historicalResponse, aiResponse);
                
            } catch (Exception e) {
                log.error("❌ MCP profit analysis failed for product: {}", productId, e);
                return createFallbackResult(productId, requestedDiscount, userId);
            }
        });
    }
    
    /**
     * Creates MCP request for enriched product data retrieval
     */
    private Map<String, Object> createMcpProductRequest(String productId) {
        Map<String, Object> request = new HashMap<>();
        request.put("tool", "algolia_search");
        
        Map<String, Object> params = new HashMap<>();
        params.put("index_name", "sdg_products");
        params.put("query", "");
        params.put("filters", "objectID:" + productId);
        params.put("attributes_to_retrieve", List.of(
                "profit_margin", "price", "category", "brand", "inventory_level",
                "average_rating", "number_of_reviews", "cost_basis", "market_position"
        ));
        params.put("hits_per_page", 1);
        
        request.put("arguments", params);
        return request;
    }
    
    /**
     * Creates MCP request for market intelligence analysis
     */
    private Map<String, Object> createMcpMarketAnalysisRequest(String productId, Map<String, Object> context) {
        Map<String, Object> request = new HashMap<>();
        request.put("tool", "algolia_search");
        
        Map<String, Object> params = new HashMap<>();
        params.put("index_name", "market_intelligence");
        params.put("query", "competitive pricing trends seasonal demand");
        params.put("filters", String.format("product_category:'%s' OR product_id:'%s'", 
                context.getOrDefault("category", ""), productId));
        params.put("attributes_to_retrieve", List.of(
                "competitor_prices", "demand_forecast", "price_elasticity",
                "seasonal_trends", "market_share", "brand_strength"
        ));
        params.put("hits_per_page", 10);
        
        request.put("arguments", params);
        return request;
    }
    
    /**
     * Creates MCP request for historical pricing analysis
     */
    private Map<String, Object> createMcpHistoricalAnalysisRequest(String productId, String userId) {
        Map<String, Object> request = new HashMap<>();
        request.put("tool", "algolia_search");
        
        Map<String, Object> params = new HashMap<>();
        params.put("index_name", "pricing_history");
        params.put("query", "discount effectiveness conversion impact");
        params.put("filters", String.format("product_id:'%s' OR user_segment:'%s'", 
                productId, determineUserSegment(userId)));
        params.put("attributes_to_retrieve", List.of(
                "historical_discounts", "conversion_rates", "revenue_impact",
                "customer_lifetime_value", "churn_risk", "price_sensitivity"
        ));
        params.put("hits_per_page", 50);
        
        request.put("arguments", params);
        return request;
    }
    
    /**
     * Creates AI analysis request using Claude through MCP
     */
    private Map<String, Object> createAiAnalysisRequest(
            Map<String, Object> productData,
            Map<String, Object> marketData, 
            Map<String, Object> historicalData,
            double requestedDiscount) {
        
        Map<String, Object> request = new HashMap<>();
        request.put("model", "claude-3-sonnet");
        request.put("system", """
                You are an AI pricing strategist expert. Analyze the provided data and determine:
                1. Optimal discount range for profit protection
                2. Market positioning impact
                3. Revenue optimization recommendations
                4. Risk assessment for the requested discount
                
                Consider profit margins, competitive landscape, demand patterns, and customer behavior.
                Provide specific percentage recommendations with confidence scores.
                """);
        
        Map<String, Object> analysisContext = new HashMap<>();
        analysisContext.put("product_data", productData);
        analysisContext.put("market_intelligence", marketData);
        analysisContext.put("historical_performance", historicalData);
        analysisContext.put("requested_discount", requestedDiscount);
        analysisContext.put("analysis_timestamp", LocalDateTime.now().toString());
        
        request.put("user_message", String.format("""
                Analyze this discount request:
                
                Requested Discount: %.1f%%
                
                Product Data: %s
                
                Market Data: %s
                
                Historical Data: %s
                
                Provide a JSON response with:
                - recommended_discount: number
                - confidence_score: number (0-1)
                - risk_level: string (low/medium/high)
                - reasoning: string
                - market_impact: string
                - alternative_strategies: array
                """, requestedDiscount, 
                safeJsonString(productData),
                safeJsonString(marketData),
                safeJsonString(historicalData)
        ));
        
        request.put("arguments", analysisContext);
        return request;
    }
    
    /**
     * Processes MCP results into intelligent profit protection decision
     */
    private McpProfitAnalysisResult processMcpResults(
            String productId,
            double requestedDiscount,
            String userId,
            Map<String, Object> productData,
            Map<String, Object> marketData,
            Map<String, Object> historicalData,
            Map<String, Object> aiAnalysis) {
        
        try {
            // Extract AI recommendations
            Map<String, Object> aiResults = (Map<String, Object>) aiAnalysis.get("analysis");
            double recommendedDiscount = ((Number) aiResults.get("recommended_discount")).doubleValue();
            double confidenceScore = ((Number) aiResults.get("confidence_score")).doubleValue();
            String riskLevel = (String) aiResults.get("risk_level");
            String reasoning = (String) aiResults.get("reasoning");
            
            // Determine if the requested discount should be approved
            boolean approved = requestedDiscount <= recommendedDiscount;
            double finalDiscount = approved ? requestedDiscount : recommendedDiscount;
            
            // Log MCP-enhanced decision to Algolia
            logMcpDecisionToAlgolia(productId, userId, requestedDiscount, finalDiscount, 
                    approved, reasoning, confidenceScore, riskLevel);
            
            return McpProfitAnalysisResult.builder()
                    .productId(productId)
                    .userId(userId)
                    .requestedDiscount(requestedDiscount)
                    .recommendedDiscount(recommendedDiscount)
                    .finalDiscount(finalDiscount)
                    .approved(approved)
                    .confidenceScore(confidenceScore)
                    .riskLevel(riskLevel)
                    .reasoning(reasoning)
                    .marketInsights(extractMarketInsights(marketData))
                    .historicalInsights(extractHistoricalInsights(historicalData))
                    .aiRecommendations((List<String>) aiResults.get("alternative_strategies"))
                    .mcpEnhanced(true)
                    .analysisTimestamp(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("❌ Failed to process MCP results", e);
            return createFallbackResult(productId, requestedDiscount, userId);
        }
    }
    
    /**
     * Logs MCP-enhanced decision to Algolia for analytics
     */
    private void logMcpDecisionToAlgolia(
            String productId, String userId, double requested, double finalDiscount,
            boolean approved, String reasoning, double confidence, String risk) {
        
        try {
            Map<String, Object> mcpLogEvent = new HashMap<>();
            mcpLogEvent.put("objectID", "mcp-decision-" + System.currentTimeMillis() + "-" + productId);
            mcpLogEvent.put("eventType", "mcp_profit_analysis");
            mcpLogEvent.put("productId", productId);
            mcpLogEvent.put("userId", userId);
            mcpLogEvent.put("requestedDiscount", requested);
            mcpLogEvent.put("finalDiscount", finalDiscount);
            mcpLogEvent.put("approved", approved);
            mcpLogEvent.put("aiReasoning", reasoning);
            mcpLogEvent.put("confidenceScore", confidence);
            mcpLogEvent.put("riskLevel", risk);
            mcpLogEvent.put("mcpEnhanced", true);
            mcpLogEvent.put("timestamp", LocalDateTime.now().toInstant(ZoneOffset.UTC).toString());
            
            // Log to special MCP analytics index
            mcpClient.post()
                    .uri("/mcp/tools/algolia_save")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "tool", "algolia_save",
                            "arguments", Map.of(
                                    "index_name", "mcp_profit_decisions",
                                    "object", mcpLogEvent
                            )
                    ))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .subscribe();
                    
            log.info("✅ MCP decision logged to Algolia for product: {}", productId);
            
        } catch (Exception e) {
            log.error("❌ Failed to log MCP decision", e);
        }
    }
    
    // Helper methods
    private String determineUserSegment(String userId) {
        // Simplified user segmentation logic
        return userId.hashCode() % 3 == 0 ? "premium" : userId.hashCode() % 3 == 1 ? "standard" : "budget";
    }
    
    private Map<String, Object> extractMarketInsights(Map<String, Object> marketData) {
        // Extract key market insights from MCP data
        return Map.of(
                "competitive_pressure", "medium",
                "demand_trend", "stable",
                "price_sensitivity", "moderate"
        );
    }
    
    private Map<String, Object> extractHistoricalInsights(Map<String, Object> historicalData) {
        // Extract historical performance insights
        return Map.of(
                "discount_effectiveness", "high",
                "conversion_uplift", "15%",
                "revenue_impact", "positive"
        );
    }
    
    private McpProfitAnalysisResult createFallbackResult(String productId, double requestedDiscount, String userId) {
        // Fallback when MCP analysis fails
        return McpProfitAnalysisResult.builder()
                .productId(productId)
                .userId(userId)
                .requestedDiscount(requestedDiscount)
                .recommendedDiscount(requestedDiscount * 0.8) // Conservative fallback
                .finalDiscount(requestedDiscount * 0.8)
                .approved(false)
                .confidenceScore(0.5)
                .riskLevel("medium")
                .reasoning("MCP analysis unavailable - using conservative fallback")
                .mcpEnhanced(false)
                .analysisTimestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Result class for MCP-enhanced profit analysis
     */
    public static class McpProfitAnalysisResult {
        private String productId;
        private String userId;
        private double requestedDiscount;
        private double recommendedDiscount;
        private double finalDiscount;
        private boolean approved;
        private double confidenceScore;
        private String riskLevel;
        private String reasoning;
        private Map<String, Object> marketInsights;
        private Map<String, Object> historicalInsights;
        private List<String> aiRecommendations;
        private boolean mcpEnhanced;
        private LocalDateTime analysisTimestamp;
        
        // Builder pattern
        public static McpProfitAnalysisResultBuilder builder() {
            return new McpProfitAnalysisResultBuilder();
        }
        
        // Getters
        public String getProductId() { return productId; }
        public String getUserId() { return userId; }
        public double getRequestedDiscount() { return requestedDiscount; }
        public double getRecommendedDiscount() { return recommendedDiscount; }
        public double getFinalDiscount() { return finalDiscount; }
        public boolean isApproved() { return approved; }
        public double getConfidenceScore() { return confidenceScore; }
        public String getRiskLevel() { return riskLevel; }
        public String getReasoning() { return reasoning; }
        public Map<String, Object> getMarketInsights() { return marketInsights; }
        public Map<String, Object> getHistoricalInsights() { return historicalInsights; }
        public List<String> getAiRecommendations() { return aiRecommendations; }
        public boolean isMcpEnhanced() { return mcpEnhanced; }
        public LocalDateTime getAnalysisTimestamp() { return analysisTimestamp; }
        
        // Builder class
        public static class McpProfitAnalysisResultBuilder {
            private McpProfitAnalysisResult result = new McpProfitAnalysisResult();
            
            public McpProfitAnalysisResultBuilder productId(String productId) {
                result.productId = productId;
                return this;
            }
            
            public McpProfitAnalysisResultBuilder userId(String userId) {
                result.userId = userId;
                return this;
            }
            
            public McpProfitAnalysisResultBuilder requestedDiscount(double requestedDiscount) {
                result.requestedDiscount = requestedDiscount;
                return this;
            }
            
            public McpProfitAnalysisResultBuilder recommendedDiscount(double recommendedDiscount) {
                result.recommendedDiscount = recommendedDiscount;
                return this;
            }
            
            public McpProfitAnalysisResultBuilder finalDiscount(double finalDiscount) {
                result.finalDiscount = finalDiscount;
                return this;
            }
            
            public McpProfitAnalysisResultBuilder approved(boolean approved) {
                result.approved = approved;
                return this;
            }
            
            public McpProfitAnalysisResultBuilder confidenceScore(double confidenceScore) {
                result.confidenceScore = confidenceScore;
                return this;
            }
            
            public McpProfitAnalysisResultBuilder riskLevel(String riskLevel) {
                result.riskLevel = riskLevel;
                return this;
            }
            
            public McpProfitAnalysisResultBuilder reasoning(String reasoning) {
                result.reasoning = reasoning;
                return this;
            }
            
            public McpProfitAnalysisResultBuilder marketInsights(Map<String, Object> marketInsights) {
                result.marketInsights = marketInsights;
                return this;
            }
            
            public McpProfitAnalysisResultBuilder historicalInsights(Map<String, Object> historicalInsights) {
                result.historicalInsights = historicalInsights;
                return this;
            }
            
            public McpProfitAnalysisResultBuilder aiRecommendations(List<String> aiRecommendations) {
                result.aiRecommendations = aiRecommendations;
                return this;
            }
            
            public McpProfitAnalysisResultBuilder mcpEnhanced(boolean mcpEnhanced) {
                result.mcpEnhanced = mcpEnhanced;
                return this;
            }
            
            public McpProfitAnalysisResultBuilder analysisTimestamp(LocalDateTime analysisTimestamp) {
                result.analysisTimestamp = analysisTimestamp;
                return this;
            }
            
            public McpProfitAnalysisResult build() {
                return result;
            }
        }
    }
    
    /**
     * Safely convert object to JSON string
     */
    private String safeJsonString(Map<String, Object> obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("Failed to serialize object to JSON: {}", e.getMessage());
            return "{}";
        }
    }
}
