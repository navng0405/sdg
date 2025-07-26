package com.dev.challenge.mcp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class McpRequest {
    private String method;
    private Map<String, Object> params;
    private String id;
    
    // For tool_code requests
    private String type;
    private String name;
    private Map<String, Object> parameters;
}
