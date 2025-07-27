package com.dev.challenge.sdg.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 🔗 MCP Client Service for Algolia MCP Server Integration
 * 
 * This service provides direct communication with the Algolia MCP Server,
 * enabling real-time AI-powered data enrichment and intelligent responses.
 */
@Service
public class McpClient {
    
    private static final Logger log = LoggerFactory.getLogger(McpClient.class);
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${mcp.server.url:http://localhost:3000}")
    private String mcpServerUrl;
    
    @Value("${mcp.server.timeout:5000}")
    private int timeoutMs;
    
    public McpClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder
                .baseUrl(mcpServerUrl)
                .build();
    }
    
    /**
     * 🛠️ Call MCP tool with error handling and fallback
     */
    public CompletableFuture<Map<String, Object>> callTool(Map<String, Object> request) {
        return webClient.post()
                .uri("/mcp/call")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(java.time.Duration.ofMillis(timeoutMs))
                .doOnNext(response -> log.debug("✅ MCP call successful: {}", response))
                .doOnError(error -> log.error("❌ MCP call failed: {}", error.getMessage()))
                .onErrorReturn(createErrorResponse("MCP server unavailable"))
                .toFuture();
    }
    
    /**
     * 🔍 Search Algolia via MCP Server
     */
    public CompletableFuture<Map<String, Object>> searchAlgolia(String indexName, String query, Map<String, Object> params) {
        Map<String, Object> searchRequest = Map.of(
                "method", "tools/call",
                "params", Map.of(
                        "name", "algolia_search",
                        "arguments", Map.of(
                                "index_name", indexName,
                                "query", query,
                                "hitsPerPage", params.getOrDefault("limit", 10),
                                "attributesToRetrieve", params.getOrDefault("attributes", "*"),
                                "filters", params.getOrDefault("filters", "")
                        )
                )
        );
        
        return callTool(searchRequest);
    }
    
    /**
     * 🤖 Analyze with Claude via MCP Server
     */
    public CompletableFuture<Map<String, Object>> analyzeWithClaude(String prompt, Map<String, Object> context) {
        Map<String, Object> claudeRequest = Map.of(
                "method", "tools/call",
                "params", Map.of(
                        "name", "claude_analyze",
                        "arguments", Map.of(
                                "prompt", prompt,
                                "context", context,
                                "max_tokens", 1000
                        )
                )
        );
        
        return callTool(claudeRequest);
    }
    
    /**
     * 💾 Save data to Algolia via MCP Server
     */
    public CompletableFuture<Map<String, Object>> saveToAlgolia(String indexName, Map<String, Object> data) {
        Map<String, Object> saveRequest = Map.of(
                "method", "tools/call",
                "params", Map.of(
                        "name", "algolia_save",
                        "arguments", Map.of(
                                "index_name", indexName,
                                "object", data
                        )
                )
        );
        
        return callTool(saveRequest);
    }
    
    /**
     * 📊 Get analytics via MCP Server
     */
    public CompletableFuture<Map<String, Object>> getAnalytics(String indexName, String metric, Map<String, Object> filters) {
        Map<String, Object> analyticsRequest = Map.of(
                "method", "tools/call",
                "params", Map.of(
                        "name", "algolia_analytics",
                        "arguments", Map.of(
                                "index_name", indexName,
                                "metric", metric,
                                "filters", filters,
                                "period", "last_30_days"
                        )
                )
        );
        
        return callTool(analyticsRequest);
    }
    
    /**
     * 🛡️ Create error response for fallback
     */
    private Map<String, Object> createErrorResponse(String message) {
        return Map.of(
                "error", true,
                "message", message,
                "timestamp", java.time.Instant.now().toString(),
                "fallback", true
        );
    }
    
    /**
     * 🎯 Check MCP Server health
     */
    public CompletableFuture<Boolean> healthCheck() {
        return webClient.get()
                .uri("/health")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> "healthy".equals(response.get("status")))
                .timeout(java.time.Duration.ofSeconds(5))
                .doOnNext(healthy -> log.info("🏥 MCP Server health: {}", healthy ? "HEALTHY" : "UNHEALTHY"))
                .onErrorReturn(false)
                .toFuture();
    }
}
