# MCP Configuration Guide

## MCP Service Setup

This application can integrate with an Algolia MCP (Model Context Protocol) server for enhanced AI-powered discount analysis. 

### Configuration Options

#### Application Properties (application.yml)

```yaml
mcp:
  server:
    # Enable/disable MCP integration (default: false)
    enabled: true
    
    # MCP server URL (default: http://localhost:3000)
    url: "http://localhost:3000"
    
    # Request timeout in seconds (default: 10)
    timeout-seconds: 10
    
    # Number of retry attempts (default: 2)
    retry-attempts: 2
```

#### Environment Variables

You can also configure using environment variables:

```bash
export MCP_ENABLED=true
export MCP_SERVER_URL=http://your-mcp-server:3000
```

### Running with MCP Disabled (Default)

By default, MCP is **disabled** and the application will use intelligent fallback logic:

- Business rule-based discount analysis
- Conservative profit protection 
- Mock market insights
- Standard recommendations

### Running with MCP Enabled

To enable MCP integration:

1. **Start your MCP server** on the configured port (default: 3000)
2. **Set the configuration** in application.yml:
   ```yaml
   mcp:
     server:
       enabled: true
       url: "http://localhost:3000"
   ```
3. **Restart the application**

### MCP Server Requirements

Your MCP server should provide these endpoints:

- `POST /mcp/tools/algolia_search` - For data retrieval
- `POST /mcp/ai/analyze` - For AI analysis

### Troubleshooting

#### Connection Refused Error
If you see `Connection refused: localhost:3000`, either:
1. Start your MCP server on port 3000, or
2. Set `mcp.server.enabled: false` to use fallback mode

#### Service Unavailable
The application will automatically fall back to business rule analysis if MCP is unreachable.

### Development Mode

For development without MCP:
```yaml
mcp:
  server:
    enabled: false  # This is the default
```

The application will work perfectly with enhanced fallback analysis.
