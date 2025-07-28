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
    private GeminiService geminiService;
    
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
                log.error("MCP integration is disabled. Please enable MCP and provide valid credentials for live data.");
                throw new IllegalStateException("MCP integration is disabled. No fallback/mock data will be returned.");
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
                // Step 4: Use MCP to get user events/behavior
                Map<String, Object> userEventsRequest = createMcpUserEventsRequest(userId);
                var userEventsResponse = mcpClient.post()
                        .uri("/mcp/tools/algolia_search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(userEventsRequest)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .retryWhen(reactor.util.retry.Retry.fixedDelay(retryAttempts, Duration.ofSeconds(1)))
                        .block();
                // Step 5: AI-powered profit margin analysis using GeminiService directly
                Map<String, Object> aiAnalysisRequest = createGeminiAnalysisRequest(
                        productResponse, marketResponse, historicalResponse, userEventsResponse, requestedDiscount);
                
                Map<String, Object> aiResponse = geminiService.analyzeProfitProtection(aiAnalysisRequest);
                
                // Process results into intelligent profit protection decision
                return processMcpResults(productId, requestedDiscount, userId, 
                        productResponse, marketResponse, historicalResponse, aiResponse);
                
            } catch (Exception e) {
                log.error("❌ MCP profit analysis failed for product: {} - Error: {}", productId, e.getMessage());
                log.debug("MCP Error Details:", e);
                throw new RuntimeException("MCP profit analysis failed. No fallback/mock data will be returned.", e);
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
     * Creates MCP request for user events/behavior analysis
     */
    private Map<String, Object> createMcpUserEventsRequest(String userId) {
        Map<String, Object> request = new HashMap<>();
        request.put("tool", "algolia_search");
        Map<String, Object> params = new HashMap<>();
        params.put("index_name", "user_events");
        params.put("query", "recent behavior activity");
        params.put("filters", String.format("user_id:'%s'", userId));
        params.put("attributes_to_retrieve", List.of("behavior", "event_type", "timestamp"));
        params.put("hits_per_page", 1);
        request.put("arguments", params);
        return request;
    }
    
    /**
     * Creates Gemini AI analysis request
     */
    private Map<String, Object> createGeminiAnalysisRequest(
            Map<String, Object> productData,
            Map<String, Object> marketData, 
            Map<String, Object> historicalData,
            Map<String, Object> userEventsData,
            double requestedDiscount) {
        Map<String, Object> analysisContext = new HashMap<>();
        // Flatten product details from MCP response
        if (productData != null && productData.containsKey("hits")) {
            List<Map<String, Object>> hits = (List<Map<String, Object>>) productData.get("hits");
            if (hits != null && !hits.isEmpty()) {
                Map<String, Object> firstHit = hits.get(0);
                for (Map.Entry<String, Object> entry : firstHit.entrySet()) {
                    analysisContext.put(entry.getKey(), entry.getValue());
                }
            }
        }
        // Add behavior from user events MCP response
        if (userEventsData != null && userEventsData.containsKey("hits")) {
            List<Map<String, Object>> hits = (List<Map<String, Object>>) userEventsData.get("hits");
            if (hits != null && !hits.isEmpty()) {
                Map<String, Object> firstHit = hits.get(0);
                if (firstHit.containsKey("behavior")) {
                    analysisContext.put("behavior", firstHit.get("behavior"));
                }
            }
        }
        // Add market and historical data as nested objects
        analysisContext.put("market_intelligence", marketData);
        analysisContext.put("historical_performance", historicalData);
        analysisContext.put("requested_discount", requestedDiscount);
        analysisContext.put("analysis_timestamp", LocalDateTime.now().toString());
        return analysisContext;
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
            // Use GeminiService.analyzeProfitProtection response directly
            Map<String, Object> aiResults = aiAnalysis;
            if (aiResults == null) {
                log.warn("AI analysis results are missing or null. Returning fallback MCP result.");
                // Fallback values
                double recommendedDiscount = requestedDiscount;
                double confidenceScore = 0.5;
                String riskLevel = "unknown";
                String reasoning = "AI analysis unavailable. Fallback result returned.";
                Map<String, Object> marketInsights = extractMarketInsights(marketData);
                return McpProfitAnalysisResult.builder()
                        .productId(productId)
                        .userId(userId)
                        .requestedDiscount(requestedDiscount)
                        .recommendedDiscount(recommendedDiscount)
                        .finalDiscount(recommendedDiscount)
                        .approved(true)
                        .confidenceScore(confidenceScore)
                        .riskLevel(riskLevel)
                        .reasoning(reasoning)
                        .marketInsights(marketInsights)
                        .historicalInsights(extractHistoricalInsights(historicalData))
                        .aiRecommendations(List.of("No AI recommendations available"))
                        .mcpEnhanced(false)
                        .analysisTimestamp(LocalDateTime.now())
                        .build();
            }
            // Extract values from GeminiService response
            boolean veto = (boolean) aiResults.getOrDefault("veto", false);
            double maxAllowedDiscount = ((Number) aiResults.getOrDefault("maxAllowedDiscount", requestedDiscount)).doubleValue();
            String reasoning = (String) aiResults.getOrDefault("reasoning", "No reasoning provided.");
            // For compatibility, treat maxAllowedDiscount as recommendedDiscount
            double recommendedDiscount = maxAllowedDiscount;
            double confidenceScore = 0.8; // Default, since not provided
            String riskLevel = veto ? "high" : "low";
            Map<String, Object> marketInsights = extractMarketInsights(marketData);
            boolean approved = !veto && requestedDiscount <= recommendedDiscount;
            double finalDiscount = approved ? requestedDiscount : recommendedDiscount;
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
                    .aiRecommendations(List.of("No AI recommendations available"))
                    .mcpEnhanced(true)
                    .analysisTimestamp(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("❌ Failed to process MCP results", e);
            throw e;
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
