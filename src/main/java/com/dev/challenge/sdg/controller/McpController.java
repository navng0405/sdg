package com.dev.challenge.sdg.controller;

import com.dev.challenge.sdg.dto.McpRequest;
import com.dev.challenge.sdg.dto.McpResponse;
import com.dev.challenge.sdg.service.McpToolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * MCP Controller - Handles MCP protocol requests at /mcp endpoint
 * Provides MCP specification compliance for Gemini AI tool calls
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class McpController {
    
    private static final Logger log = LoggerFactory.getLogger(McpController.class);
    private final McpToolService mcpToolService;
    
    /**
     * Main MCP endpoint - handles all MCP JSON-RPC requests
     */
    @PostMapping
    public ResponseEntity<McpResponse> handleMcpRequest(@RequestBody McpRequest request) {
        log.info("Received MCP request: method={}, id={}", request.getMethod(), request.getId());
        
        try {
            McpResponse response = switch (request.getMethod()) {
                case "initialize" -> handleInitialize(request);
                case "tools/list" -> handleToolsList(request);
                case "tools/call" -> handleToolCall(request);
                default -> createErrorResponse(request.getId(), -32601, 
                    "Method not found: " + request.getMethod());
            };
            
            log.debug("Sending MCP response for request: {}", request.getId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing MCP request: {}", request.getId(), e);
            McpResponse errorResponse = createErrorResponse(request.getId(), -32603, 
                "Internal error: " + e.getMessage());
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /**
     * Handles MCP initialize request
     */
    private McpResponse handleInitialize(McpRequest request) {
        log.info("Initializing MCP connection");
        
        McpResponse response = new McpResponse();
        response.setJsonrpc("2.0");
        response.setId(request.getId());
        
        McpResponse.InitializeResult result = new McpResponse.InitializeResult();
        result.setProtocolVersion("2024-11-05");
        result.setServerInfo(Map.of(
            "name", "Smart Discount Generator MCP Server",
            "version", "1.0.0"
        ));
        result.setCapabilities(Map.of(
            "tools", Map.of("listChanged", true)
        ));
        
        response.setResult(result);
        log.info("MCP initialization completed successfully");
        return response;
    }
    
    /**
     * Handles tools/list request - returns available MCP tools
     */
    private McpResponse handleToolsList(McpRequest request) {
        log.info("Listing available MCP tools");
        
        McpResponse response = new McpResponse();
        response.setJsonrpc("2.0");
        response.setId(request.getId());
        
        McpResponse.ToolsListResult result = new McpResponse.ToolsListResult();
        result.setTools(mcpToolService.getAvailableTools());
        
        response.setResult(result);
        log.info("Returned {} available MCP tools", result.getTools().size());
        return response;
    }
    
    /**
     * Handles tools/call request - executes MCP tool
     */
    private McpResponse handleToolCall(McpRequest request) {
        Map<String, Object> params = request.getParams();
        String toolName = (String) params.get("name");
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
        
        log.info("Executing MCP tool: {} with arguments: {}", toolName, arguments);
        
        try {
            Map<String, Object> toolResult = mcpToolService.executeTool(toolName, arguments);
            
            McpResponse response = new McpResponse();
            response.setJsonrpc("2.0");
            response.setId(request.getId());
            
            McpResponse.ToolResult result = new McpResponse.ToolResult();
            result.setContent(toolResult);
            result.setIsError(false);
            
            response.setResult(result);
            log.info("Successfully executed MCP tool: {}", toolName);
            return response;
            
        } catch (Exception e) {
            log.error("Error executing MCP tool: {}", toolName, e);
            return createErrorResponse(request.getId(), -32000, 
                "Tool execution failed: " + e.getMessage());
        }
    }
    
    /**
     * Creates error response for MCP protocol
     */
    private McpResponse createErrorResponse(String id, int code, String message) {
        McpResponse response = new McpResponse();
        response.setJsonrpc("2.0");
        response.setId(id);
        
        McpResponse.ErrorInfo error = new McpResponse.ErrorInfo();
        error.setCode(code);
        error.setMessage(message);
        
        response.setError(error);
        return response;
    }
}
