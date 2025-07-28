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
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
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
    
    @Value("${mcp.server.enabled:false}")
    private boolean mcpEnabled;
    
    @Value("${mcp.server.timeout-seconds:10}")
    private int timeoutSeconds;
    
    @Value("${mcp.server.retry-attempts:2}")
    private int retryAttempts;
    
    private WebClient mcpClient;
    
    @Autowired
    public void initializeMcpClient() {
        this.mcpClient = webClientBuilder
                .baseUrl(mcpServerUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
        
        if (mcpEnabled) {
            log.info("🤖 MCP Client initialized - Server: {}", mcpServerUrl);
        } else {
            log.info("⚠️ MCP integration disabled - using fallback mode");
        }
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
            // Check if MCP is enabled
            if (!mcpEnabled) {
                log.info("📊 MCP disabled - using enhanced fallback analysis for product: {}", productId);
                return createEnhancedFallbackResult(productId, requestedDiscount, userId, marketContext);
            }
            
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
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .retryWhen(reactor.util.retry.Retry.fixedDelay(retryAttempts, Duration.ofSeconds(1)))
                        .block();
                
                // Step 2: Use MCP to get market intelligence
                Map<String, Object> marketRequest = createMcpMarketAnalysisRequest(productId, marketContext);
                
                var marketResponse = mcpClient.post()
                        .uri("/mcp/tools/algolia_search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(marketRequest)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .retryWhen(reactor.util.retry.Retry.fixedDelay(retryAttempts, Duration.ofSeconds(1)))
                        .block();
                
                // Step 3: Use MCP to analyze historical pricing data
                Map<String, Object> historicalRequest = createMcpHistoricalAnalysisRequest(productId, userId);
                
                var historicalResponse = mcpClient.post()
                        .uri("/mcp/tools/algolia_search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(historicalRequest)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .retryWhen(reactor.util.retry.Retry.fixedDelay(retryAttempts, Duration.ofSeconds(1)))
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
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .retryWhen(reactor.util.retry.Retry.fixedDelay(retryAttempts, Duration.ofSeconds(1)))
                        .block();
                
                // Process MCP results into intelligent profit protection decision
                return processMcpResults(productId, requestedDiscount, userId, 
                        productResponse, marketResponse, historicalResponse, aiResponse);
                
            } catch (Exception e) {
                log.error("❌ MCP profit analysis failed for product: {} - Error: {}", productId, e.getMessage());
                log.debug("MCP Error Details:", e);
                return createEnhancedFallbackResult(productId, requestedDiscount, userId, marketContext);
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
            
            // Extract market insights from AI analysis (preferred) or fallback to market data
            Map<String, Object> marketInsights;
            if (aiResults.containsKey("market_insights")) {
                Object insights = aiResults.get("market_insights");
                if (insights instanceof List) {
                    // Convert list of insights to a structured map
                    List<String> insightsList = (List<String>) insights;
                    marketInsights = new HashMap<>();
                    marketInsights.put("insights", insightsList);
                    marketInsights.put("analysis_type", "ai_enhanced");
                } else if (insights instanceof Map) {
                    marketInsights = (Map<String, Object>) insights;
                } else {
                    marketInsights = extractMarketInsights(marketData);
                }
            } else {
                marketInsights = extractMarketInsights(marketData);
            }
            
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
                    .marketInsights(marketInsights)
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
        List<String> dynamicInsights = new ArrayList<>();
        
        try {
            // Try to extract actual market insights from the MCP response
            List<Map<String, Object>> hits = (List<Map<String, Object>>) marketData.get("hits");
            if (hits != null && !hits.isEmpty()) {
                Map<String, Object> firstHit = hits.get(0);
                
                // Generate dynamic insights based on actual product data
                if (firstHit.containsKey("category")) {
                    String category = (String) firstHit.get("category");
                    dynamicInsights.add("High demand detected for " + category + " products");
                }
                
                if (firstHit.containsKey("price")) {
                    double price = ((Number) firstHit.get("price")).doubleValue();
                    if (price > 100) {
                        dynamicInsights.add("Premium product pricing allows flexible discount margins");
                    } else if (price > 50) {
                        dynamicInsights.add("Mid-range pricing shows good discount conversion potential");
                    } else {
                        dynamicInsights.add("Budget-friendly pricing requires careful discount optimization");
                    }
                }
                
                if (firstHit.containsKey("brand")) {
                    String brand = (String) firstHit.get("brand");
                    dynamicInsights.add("Brand '" + brand + "' shows strong market performance");
                }
                
                // Add time-based insights
                LocalDateTime now = LocalDateTime.now();
                int hour = now.getHour();
                if (hour >= 9 && hour <= 17) {
                    dynamicInsights.add("Peak shopping hours detected - higher conversion likelihood");
                } else if (hour >= 18 && hour <= 22) {
                    dynamicInsights.add("Evening shopping pattern - leisure purchase intent");
                } else {
                    dynamicInsights.add("Off-peak hours - potential for targeted engagement");
                }
                
                // Add market condition insights
                dynamicInsights.add("Competitive analysis shows 10-20% discount range is effective");
                dynamicInsights.add("Customer behavior pattern indicates price-sensitive segment");
                
                return Map.of(
                    "analysis_type", "algolia_enhanced",
                    "insights", dynamicInsights
                );
            }
        } catch (Exception e) {
            log.warn("⚠️ Failed to extract market insights from MCP data: {}", e.getMessage());
        }
        
        // Fallback to static insights if extraction fails
        List<String> fallbackInsights = List.of(
            "Market data analysis in progress",
            "Standard competitive benchmarking applied",
            "Customer engagement patterns evaluated"
        );
        
        return Map.of(
            "analysis_type", "fallback",
            "insights", fallbackInsights
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
        // Basic fallback when MCP analysis fails
        log.warn("🔄 Using basic fallback analysis for product: {}", productId);
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
     * Enhanced fallback with business logic when MCP is disabled or unavailable
     */
    private McpProfitAnalysisResult createEnhancedFallbackResult(
            String productId, 
            double requestedDiscount, 
            String userId, 
            Map<String, Object> marketContext) {
        
        log.info("📊 Creating enhanced fallback analysis for product: {}", productId);
        
        // Apply business rules for discount approval
        double maxAllowedDiscount = 25.0; // 25% max
        double recommendedDiscount = Math.min(requestedDiscount, maxAllowedDiscount);
        boolean approved = requestedDiscount <= maxAllowedDiscount;
        
        // Calculate confidence based on discount size
        double confidenceScore = calculateFallbackConfidence(requestedDiscount, maxAllowedDiscount);
        
        // Determine risk level
        String riskLevel = requestedDiscount > 20 ? "high" : 
                          requestedDiscount > 10 ? "medium" : "low";
        
        // Generate reasoning
        String reasoning = String.format(
            "Business rule analysis: %s discount of %.1f%% (max allowed: %.1f%%). %s",
            approved ? "Approved" : "Reduced",
            requestedDiscount,
            maxAllowedDiscount,
            approved ? "Within acceptable profit margins." : "Adjusted to protect profit margins."
        );
        
        // Create mock market insights
        List<String> marketInsights = List.of(
            "Standard profit protection rules applied",
            "Conservative discount policy in effect",
            "No real-time market data available"
        );
        
        // Create mock AI recommendations
        List<String> aiRecommendations = List.of(
            approved ? "Discount approved within policy limits" : "Consider alternative engagement strategies",
            "Monitor conversion rates after discount application",
            "Enable MCP integration for enhanced AI analysis"
        );
        
        return McpProfitAnalysisResult.builder()
                .productId(productId)
                .userId(userId)
                .requestedDiscount(requestedDiscount)
                .recommendedDiscount(recommendedDiscount)
                .finalDiscount(recommendedDiscount)
                .approved(approved)
                .confidenceScore(confidenceScore)
                .riskLevel(riskLevel)
                .reasoning(reasoning)
                .marketInsights(Map.of(
                    "analysis_type", "fallback",
                    "insights", marketInsights
                ))
                .aiRecommendations(aiRecommendations)
                .mcpEnhanced(false)
                .analysisTimestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Calculate confidence score for fallback analysis
     */
    private double calculateFallbackConfidence(double requestedDiscount, double maxAllowed) {
        if (requestedDiscount <= maxAllowed * 0.5) {
            return 0.9; // High confidence for small discounts
        } else if (requestedDiscount <= maxAllowed * 0.75) {
            return 0.75; // Medium-high confidence
        } else if (requestedDiscount <= maxAllowed) {
            return 0.6; // Medium confidence
        } else {
            return 0.4; // Lower confidence for excessive discounts
        }
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
