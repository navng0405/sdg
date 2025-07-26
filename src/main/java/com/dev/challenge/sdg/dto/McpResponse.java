package com.dev.challenge.sdg.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * MCP Response DTO - Represents outgoing MCP JSON-RPC responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class McpResponse {
    
    private String jsonrpc = "2.0";
    private String id;
    private Object result;
    private ErrorInfo error;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorInfo {
        private int code;
        private String message;
        private Object data;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InitializeResult {
        private String protocolVersion;
        private Map<String, Object> serverInfo;
        private Map<String, Object> capabilities;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolsListResult {
        private List<ToolDefinition> tools;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolDefinition {
        private String name;
        private String description;
        private Map<String, Object> inputSchema;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolResult {
        private Map<String, Object> content;
        private boolean isError;
        private String errorMessage;
    }
}
