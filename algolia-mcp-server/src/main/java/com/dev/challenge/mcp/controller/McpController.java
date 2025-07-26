package com.dev.challenge.mcp.controller;

import com.dev.challenge.mcp.dto.McpRequest;
import com.dev.challenge.mcp.dto.McpResponse;
import com.dev.challenge.mcp.service.McpToolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class McpController {
    
    private static final Logger log = LoggerFactory.getLogger(McpController.class);
    private final McpToolService toolService;
    
    @PostMapping
    public ResponseEntity<McpResponse> handleMcpRequest(@RequestBody McpRequest request) {
        log.info("Received MCP request: method={}, type={}, name={}", 
                request.getMethod(), request.getType(), request.getName());
        
        try {
            McpResponse response = switch (request.getMethod()) {
                case "initialize" -> handleInitialize(request);
                case "tools/list" -> handleToolsList(request);
                case "tools/call" -> handleToolCall(request);
                default -> {
                    if ("tool_code".equals(request.getType())) {
                        yield handleToolCode(request);
                    }
                    yield createErrorResponse(request.getId(), -32601, "Method not found");
                }
            };
            
            log.debug("Sending MCP response: {}", response);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error handling MCP request", e);
            return ResponseEntity.ok(createErrorResponse(request.getId(), -32603, "Internal error: " + e.getMessage()));
        }
    }
    
    private McpResponse handleInitialize(McpRequest request) {
        log.info("Initializing Algolia MCP Server");
        
        Map<String, Object> result = Map.of(
            "protocolVersion", "2024-11-05",
            "capabilities", Map.of(
                "tools", Map.of("listChanged", true)
            ),
            "serverInfo", Map.of(
                "name", "algolia-mcp-server",
                "version", "1.0.0",
                "description", "Algolia MCP Server for Smart Discount Generator"
            )
        );
        
        return McpResponse.builder()
                .id(request.getId())
                .result(result)
                .build();
    }
    
    private McpResponse handleToolsList(McpRequest request) {
        log.info("Listing available MCP tools");
        
        List<McpResponse.ToolDefinition> tools = List.of(
            McpResponse.ToolDefinition.builder()
                .name("getUserHesitationData")
                .description("Retrieves detailed user behavior data from Algolia to assess hesitation patterns")
                .inputSchema(Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "userId", Map.of("type", "string", "description", "User ID to analyze")
                    ),
                    "required", List.of("userId")
                ))
                .build(),
                
            McpResponse.ToolDefinition.builder()
                .name("getProductProfitMargin")
                .description("Retrieves product profit margin and inventory data from Algolia")
                .inputSchema(Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "productId", Map.of("type", "string", "description", "Product ID to query")
                    ),
                    "required", List.of("productId")
                ))
                .build(),
                
            McpResponse.ToolDefinition.builder()
                .name("generateSmartDiscount")
                .description("Generates AI-powered discount offer based on user behavior and product data")
                .inputSchema(Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "userId", Map.of("type", "string", "description", "User ID"),
                        "productId", Map.of("type", "string", "description", "Product ID"),
                        "behaviorSummary", Map.of("type", "object", "description", "User behavior analysis"),
                        "profitMargin", Map.of("type", "number", "description", "Product profit margin"),
                        "userSegment", Map.of("type", "string", "description", "User segment classification")
                    ),
                    "required", List.of("userId", "productId", "behaviorSummary", "profitMargin")
                ))
                .build(),
                
            McpResponse.ToolDefinition.builder()
                .name("logDiscountConversion")
                .description("Logs discount conversion events for analytics and optimization")
                .inputSchema(Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "discountCode", Map.of("type", "string", "description", "Discount code"),
                        "userId", Map.of("type", "string", "description", "User ID"),
                        "conversionStatus", Map.of("type", "string", "description", "Conversion status: applied, expired, rejected")
                    ),
                    "required", List.of("discountCode", "userId", "conversionStatus")
                ))
                .build()
        );
        
        McpResponse.ToolsListResult result = McpResponse.ToolsListResult.builder()
                .tools(tools)
                .build();
        
        return McpResponse.builder()
                .id(request.getId())
                .result(result)
                .build();
    }
    
    private McpResponse handleToolCall(McpRequest request) {
        return handleToolCode(request);
    }
    
    private McpResponse handleToolCode(McpRequest request) {
        String toolName = request.getName();
        Map<String, Object> parameters = request.getParameters();
        
        log.info("Executing MCP tool: {} with parameters: {}", toolName, parameters);
        
        try {
            Object toolResult = switch (toolName) {
                case "getUserHesitationData" -> toolService.getUserHesitationData(parameters);
                case "getProductProfitMargin" -> toolService.getProductProfitMargin(parameters);
                case "generateSmartDiscount" -> toolService.generateSmartDiscount(parameters);
                case "logDiscountConversion" -> toolService.logDiscountConversion(parameters);
                default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
            };
            
            McpResponse.ToolResult result = McpResponse.ToolResult.builder()
                    .content(toolResult.toString())
                    .isError(false)
                    .data(toolResult instanceof Map ? (Map<String, Object>) toolResult : Map.of("result", toolResult))
                    .build();
            
            return McpResponse.builder()
                    .id(request.getId())
                    .result(result)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error executing tool: {}", toolName, e);
            
            McpResponse.ToolResult result = McpResponse.ToolResult.builder()
                    .content("Error executing tool: " + e.getMessage())
                    .isError(true)
                    .data(Map.of("error", e.getMessage()))
                    .build();
            
            return McpResponse.builder()
                    .id(request.getId())
                    .result(result)
                    .build();
        }
    }
    
    private McpResponse createErrorResponse(String id, int code, String message) {
        McpResponse.McpError error = McpResponse.McpError.builder()
                .code(code)
                .message(message)
                .build();
        
        return McpResponse.builder()
                .id(id)
                .error(error)
                .build();
    }
}
