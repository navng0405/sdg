package com.dev.challenge.sdg.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * MCP Client Service - Handles HTTP communication with Algolia MCP Server
 * Forwards tool_code requests and processes tool_output responses
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpClientService {
    
    private static final Logger log = LoggerFactory.getLogger(McpClientService.class);
    
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    @Value("${mcp.server.base-url}")
    private String mcpServerBaseUrl;
    
    private WebClient webClient;
    
    /**
     * Calls an MCP tool on the Algolia MCP Server
     */
    public CompletableFuture<Map<String, Object>> callMcpTool(String toolName, Map<String, Object> parameters) {
        log.info("Calling MCP tool: {} with parameters: {}", toolName, parameters);
        
        if (webClient == null) {
            webClient = webClientBuilder.baseUrl(mcpServerBaseUrl).build();
        }
        
        try {
            // Create MCP request according to JSON-RPC specification
            Map<String, Object> mcpRequest = Map.of(
                "jsonrpc", "2.0",
                "id", UUID.randomUUID().toString(),
                "method", "tools/call",
                "params", Map.of(
                    "name", toolName,
                    "arguments", parameters
                )
            );
            
            return webClient
                    .post()
                    .uri("/mcp")
                    .header("Content-Type", "application/json")
                    .bodyValue(mcpRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .handle((response, sink) -> {
                        try {
                            log.debug("Received MCP response: {}", response);
                            
                            @SuppressWarnings("unchecked")
                            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
                            
                            // Check for JSON-RPC error
                            if (responseMap.containsKey("error")) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> error = (Map<String, Object>) responseMap.get("error");
                                String errorMessage = (String) error.get("message");
                                log.error("MCP tool call failed: {}", errorMessage);
                                sink.error(new RuntimeException("MCP tool error: " + errorMessage));
                                return;
                            }
                            
                            // Extract result from JSON-RPC response
                            if (responseMap.containsKey("result")) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> result = (Map<String, Object>) responseMap.get("result");
                                
                                if (result.containsKey("content")) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> content = (Map<String, Object>) result.get("content");
                                    log.info("Successfully executed MCP tool: {}", toolName);
                                    sink.next(content);
                                } else {
                                    log.info("Successfully executed MCP tool: {} (no content)", toolName);
                                    sink.next(result);
                                }
                            } else {
                                log.warn("No result in MCP response for tool: {}", toolName);
                                sink.next(Map.of("status", "completed", "message", "Tool executed successfully"));
                            }
                            
                        } catch (Exception e) {
                            log.error("Error parsing MCP response for tool: {}", toolName, e);
                            sink.error(new RuntimeException("Failed to parse MCP response: " + e.getMessage()));
                        }
                    })
                    .toFuture();
                    
        } catch (Exception e) {
            log.error("Error calling MCP tool: {}", toolName, e);
            return CompletableFuture.failedFuture(
                new RuntimeException("Failed to call MCP tool: " + e.getMessage())
            );
        }
    }
    
    /**
     * Initializes connection with MCP Server
     */
    public CompletableFuture<Map<String, Object>> initializeMcpConnection() {
        log.info("Initializing MCP connection to server: {}", mcpServerBaseUrl);
        
        if (webClient == null) {
            webClient = webClientBuilder.baseUrl(mcpServerBaseUrl).build();
        }
        
        Map<String, Object> initRequest = Map.of(
            "jsonrpc", "2.0",
            "id", UUID.randomUUID().toString(),
            "method", "initialize",
            "params", Map.of(
                "protocolVersion", "2024-11-05",
                "capabilities", Map.of(
                    "tools", Map.of("listChanged", true)
                ),
                "clientInfo", Map.of(
                    "name", "SDG-API-Gateway",
                    "version", "1.0.0"
                )
            )
        );
        
        return webClient
                .post()
                .uri("/mcp")
                .header("Content-Type", "application/json")
                .bodyValue(initRequest)
                .retrieve()
                .bodyToMono(String.class)
                .handle((response, sink) -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
                        
                        if (responseMap.containsKey("error")) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> error = (Map<String, Object>) responseMap.get("error");
                            log.error("MCP initialization failed: {}", error.get("message"));
                            sink.error(new RuntimeException("MCP initialization failed"));
                        } else {
                            log.info("Successfully initialized MCP connection");
                            sink.next(responseMap);
                        }
                        
                    } catch (Exception e) {
                        log.error("Error parsing MCP initialization response", e);
                        sink.error(e);
                    }
                })
                .toFuture();
    }
    
    /**
     * Lists available tools from MCP Server
     */
    public CompletableFuture<Map<String, Object>> listMcpTools() {
        log.debug("Listing available MCP tools");
        
        if (webClient == null) {
            webClient = webClientBuilder.baseUrl(mcpServerBaseUrl).build();
        }
        
        Map<String, Object> listRequest = Map.of(
            "jsonrpc", "2.0",
            "id", UUID.randomUUID().toString(),
            "method", "tools/list",
            "params", Map.of()
        );
        
        return webClient
                .post()
                .uri("/mcp")
                .header("Content-Type", "application/json")
                .bodyValue(listRequest)
                .retrieve()
                .bodyToMono(String.class)
                .handle((response, sink) -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
                        
                        if (responseMap.containsKey("result")) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> result = (Map<String, Object>) responseMap.get("result");
                            log.info("Retrieved MCP tools list");
                            sink.next(result);
                        } else {
                            log.warn("No tools list in MCP response");
                            sink.next(Map.of("tools", Map.of()));
                        }
                        
                    } catch (Exception e) {
                        log.error("Error parsing MCP tools list response", e);
                        sink.error(e);
                    }
                })
                .toFuture();
    }
}
