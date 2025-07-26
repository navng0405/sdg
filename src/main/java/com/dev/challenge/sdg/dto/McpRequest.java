package com.dev.challenge.sdg.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * MCP Request DTO - Represents incoming MCP JSON-RPC requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class McpRequest {
    
    private String jsonrpc = "2.0";
    private String id;
    private String method;
    private Map<String, Object> params;
    
    // Additional fields for tool_code requests
    private String tool_code;
    private Map<String, Object> tool_params;
}
