#!/usr/bin/env node

/**
 * 🔥 Advanced Algolia-Integrated MCP Server
 * 
 * This implements a Model Context Protocol (MCP) server with deep Algolia integration
 * for AI-powered commerce applications. Features include:
 * - Smart discount generation using AI analysis
 * - Real-time user journey analytics
 * - Intelligent search optimization
 * - Conversion funnel tracking with AI insights
 * - Advanced product recommendations
 */

require('dotenv').config();
const express = require('express');
const cors = require('cors');
const algoliasearch = require('algoliasearch');
const winston = require('winston');

// Configure logging
const logger = winston.createLogger({
  level: process.env.LOG_LEVEL || 'info',
  format: winston.format.combine(
    winston.format.timestamp(),
    winston.format.colorize(),
    winston.format.simple()
  ),
  transports: [
    new winston.transports.Console()
  ]
});

class AlgoliaIntegratedMcpServer {
  constructor() {
    this.app = express();
    this.port = process.env.MCP_PORT || 3000;
    this.setupAlgolia();
    this.setupMiddleware();
    this.setupRoutes();
    this.setupErrorHandling();
  }

  setupAlgolia() {
    if (!process.env.ALGOLIA_APPLICATION_ID || !process.env.ALGOLIA_SEARCH_API_KEY) {
      logger.warn('⚠️ Algolia credentials not found, using mock mode');
      this.algoliaClient = null;
      this.searchIndex = null;
      return;
    }

    try {
      this.algoliaClient = algoliasearch(
        process.env.ALGOLIA_APPLICATION_ID,
        process.env.ALGOLIA_SEARCH_API_KEY
      );
      this.searchIndex = this.algoliaClient.initIndex('sdg_products');
      logger.info('✅ Algolia client initialized successfully');
    } catch (error) {
      logger.error('❌ Failed to initialize Algolia client:', error);
      this.algoliaClient = null;
      this.searchIndex = null;
    }
  }

  setupMiddleware() {
    this.app.use(cors());
    this.app.use(express.json({ limit: '10mb' }));
    this.app.use(express.urlencoded({ extended: true }));
    
    // Request logging
    this.app.use((req, res, next) => {
      logger.debug(`${req.method} ${req.path}`);
      next();
    });
  }

  setupRoutes() {
    // MCP Protocol endpoints
    this.app.post('/', this.handleMcpRequest.bind(this));
    this.app.post('/mcp', this.handleMcpRequest.bind(this));
    
    // Tool-specific endpoints for direct access
    this.app.post('/tools/generate_smart_discount', this.generateSmartDiscount.bind(this));
    this.app.post('/tools/analyze_user_journey', this.analyzeUserJourney.bind(this));
    this.app.post('/tools/optimize_search_results', this.optimizeSearchResults.bind(this));
    this.app.post('/tools/track_conversion_funnel', this.trackConversionFunnel.bind(this));
    
    // Health check
    this.app.get('/health', (req, res) => {
      res.json({ 
        status: 'healthy',
        service: 'algolia-integrated-mcp-server',
        version: '1.0.0',
        algolia_connected: !!this.algoliaClient,
        timestamp: new Date().toISOString()
      });
    });
  }

  setupErrorHandling() {
    this.app.use((err, req, res, next) => {
      logger.error('Unhandled error:', err);
      res.status(500).json({
        error: 'Internal server error',
        message: err.message
      });
    });
  }

  async handleMcpRequest(req, res) {
    const { jsonrpc, id, method, params } = req.body;
    
    logger.info(`🔧 MCP Request: ${method}`);
    
    try {
      let result;
      
      switch (method) {
        case 'initialize':
          result = this.handleInitialize();
          break;
        case 'tools/list':
          result = this.handleToolsList();
          break;
        case 'tools/call':
          result = await this.handleToolCall(params);
          break;
        default:
          throw new Error(`Unknown method: ${method}`);
      }
      
      res.json({
        jsonrpc: '2.0',
        id,
        result
      });
      
    } catch (error) {
      logger.error(`❌ MCP Error for ${method}:`, error);
      res.json({
        jsonrpc: '2.0',
        id,
        error: {
          code: -32000,
          message: error.message
        }
      });
    }
  }

  handleInitialize() {
    return {
      protocolVersion: '2024-11-05',
      serverInfo: {
        name: 'algolia-integrated-mcp-server',
        version: '1.0.0',
        description: 'Advanced Algolia-integrated MCP Server for AI-powered commerce'
      },
      capabilities: {
        tools: { listChanged: true },
        logging: {},
        experimental: {
          sampling: true
        }
      }
    };
  }

  handleToolsList() {
    return {
      tools: [
        {
          name: 'generate_smart_discount',
          description: 'Generate AI-powered discount recommendations based on user behavior and product data',
          inputSchema: {
            type: 'object',
            properties: {
              userId: { type: 'string', description: 'User identifier' },
              productId: { type: 'string', description: 'Product identifier' },
              userBehavior: { type: 'object', description: 'User behavior data' },
              requestedDiscount: { type: 'number', description: 'Requested discount percentage' }
            },
            required: ['userId', 'productId']
          }
        },
        {
          name: 'analyze_user_journey',
          description: 'Analyze user search and browsing patterns for personalization',
          inputSchema: {
            type: 'object',
            properties: {
              userId: { type: 'string', description: 'User identifier' },
              sessionData: { type: 'object', description: 'Current session data' },
              timeRange: { type: 'string', description: 'Time range for analysis' }
            },
            required: ['userId']
          }
        },
        {
          name: 'optimize_search_results',
          description: 'Enhance search results with AI-powered personalization and business rules',
          inputSchema: {
            type: 'object',
            properties: {
              query: { type: 'string', description: 'Search query' },
              userId: { type: 'string', description: 'User identifier' },
              context: { type: 'object', description: 'Additional context' }
            },
            required: ['query']
          }
        },
        {
          name: 'track_conversion_funnel',
          description: 'Track and analyze conversion funnel performance with AI insights',
          inputSchema: {
            type: 'object',
            properties: {
              eventType: { type: 'string', description: 'Type of event' },
              userId: { type: 'string', description: 'User identifier' },
              productId: { type: 'string', description: 'Product identifier' },
              metadata: { type: 'object', description: 'Additional event metadata' }
            },
            required: ['eventType']
          }
        }
      ]
    };
  }

  async handleToolCall(params) {
    const { name, arguments: args } = params;
    
    logger.info(`🛠️ Executing tool: ${name}`);
    
    switch (name) {
      case 'generate_smart_discount':
        return await this.generateSmartDiscount({ body: args });
      case 'analyze_user_journey':
        return await this.analyzeUserJourney({ body: args });
      case 'optimize_search_results':
        return await this.optimizeSearchResults({ body: args });
      case 'track_conversion_funnel':
        return await this.trackConversionFunnel({ body: args });
      default:
        throw new Error(`Unknown tool: ${name}`);
    }
  }

// Configure Algolia MCP Server with your credentials
const server = new AlgoliaMCPServer({
  // Algolia Configuration
  algolia: {
    appId: process.env.ALGOLIA_APPLICATION_ID,
    apiKey: process.env.ALGOLIA_ADMIN_API_KEY,
    searchKey: process.env.ALGOLIA_SEARCH_API_KEY
  },
  
  // MCP Server Configuration
  server: {
    name: 'smart-discount-generator-mcp',
    version: '1.0.0',
    description: 'Smart Discount Generator powered by Algolia MCP Server',
    port: process.env.MCP_PORT || 3000
  },
  
  // Enable advanced features
  features: {
    search: true,
    analytics: true,
    indexing: true,
    ai_insights: true,
    recommendations: true
  },
  
  // Custom tool definitions for our app
  tools: [
    {
      name: 'generate_smart_discount',
      description: 'Generate AI-powered discount recommendations based on user behavior and product data',
      inputSchema: {
        type: 'object',
        properties: {
          userId: { type: 'string' },
          productId: { type: 'string' },
          userBehavior: { type: 'object' },
          requestedDiscount: { type: 'number' }
        },
        required: ['userId', 'productId']
      }
    },
    {
      name: 'analyze_user_journey',
      description: 'Analyze user search and browsing patterns for personalization',
      inputSchema: {
        type: 'object',
        properties: {
          userId: { type: 'string' },
          sessionData: { type: 'object' },
          timeRange: { type: 'string' }
        },
        required: ['userId']
      }
    },
    {
      name: 'optimize_search_results',
      description: 'Enhance search results with AI-powered personalization and business rules',
      inputSchema: {
        type: 'object',
        properties: {
          query: { type: 'string' },
          userId: { type: 'string' },
          context: { type: 'object' }
        },
        required: ['query']
      }
    },
    {
      name: 'track_conversion_funnel',
      description: 'Track and analyze conversion funnel performance with AI insights',
      inputSchema: {
        type: 'object',
        properties: {
          eventType: { type: 'string' },
          userId: { type: 'string' },
          productId: { type: 'string' },
          metadata: { type: 'object' }
        },
        required: ['eventType']
      }
    }
  ]
});

// Enhanced tool implementations
server.addToolHandler('generate_smart_discount', async (params) => {
  const { userId, productId, userBehavior = {}, requestedDiscount = 10 } = params;
  
  try {
    // Search for product data using Algolia MCP
    const productData = await server.algolia.search('sdg_products', '', {
      filters: `objectID:${productId}`,
      hitsPerPage: 1
    });
    
    // Search for user behavior data
    const userEvents = await server.algolia.search('sdg_user_events', '', {
      filters: `userId:${userId}`,
      hitsPerPage: 50
    });
    
    // AI-powered discount analysis
    const discountAnalysis = await server.ai.analyze({
      prompt: `Analyze the discount request for user ${userId} and product ${productId}`,
      context: {
        product: productData.hits[0] || {},
        userBehavior: userEvents.hits || [],
        requestedDiscount,
        marketConditions: 'normal'
      },
      tools: ['profit_analysis', 'market_comparison', 'user_segmentation']
    });
    
    return {
      success: true,
      discount: {
        percentage: discountAnalysis.recommendedDiscount || requestedDiscount,
        reasoning: discountAnalysis.reasoning,
        confidence: discountAnalysis.confidence,
        expiryMinutes: 30,
        conditions: discountAnalysis.conditions || []
      },
      aiInsights: discountAnalysis.insights,
      metadata: {
        timestamp: new Date().toISOString(),
        mcpVersion: server.version
      }
    };
  } catch (error) {
    console.error('Error generating smart discount:', error);
    return {
      success: false,
      error: error.message,
      fallback: {
        percentage: Math.min(requestedDiscount, 15),
        reasoning: 'Applied conservative discount due to analysis error'
      }
    };
  }
});

server.addToolHandler('analyze_user_journey', async (params) => {
  const { userId, sessionData = {}, timeRange = '7d' } = params;
  
  try {
    // Get user search analytics
    const searchAnalytics = await server.algolia.getAnalytics({
      index: 'sdg_products',
      filters: { userId },
      timeRange
    });
    
    // Get user event data
    const userEvents = await server.algolia.search('sdg_user_events', '', {
      filters: `userId:${userId}`,
      hitsPerPage: 100
    });
    
    // AI analysis of user journey
    const journeyAnalysis = await server.ai.analyze({
      prompt: `Analyze the user journey and behavior patterns for user ${userId}`,
      context: {
        searchAnalytics,
        userEvents: userEvents.hits,
        sessionData,
        timeRange
      },
      tools: ['pattern_recognition', 'intent_analysis', 'personalization']
    });
    
    return {
      success: true,
      userProfile: {
        searchPreferences: journeyAnalysis.searchPreferences,
        categoryAffinity: journeyAnalysis.categoryAffinity,
        priceRange: journeyAnalysis.priceRange,
        conversionProbability: journeyAnalysis.conversionProbability
      },
      recommendations: journeyAnalysis.recommendations,
      aiInsights: journeyAnalysis.insights
    };
  } catch (error) {
    console.error('Error analyzing user journey:', error);
    return { success: false, error: error.message };
  }
});

server.addToolHandler('optimize_search_results', async (params) => {
  const { query, userId, context = {} } = params;
  
  try {
    // Get base search results
    const baseResults = await server.algolia.search('sdg_products', query, {
      hitsPerPage: 20
    });
    
    // Get user preferences if userId provided
    let userPreferences = {};
    if (userId) {
      const userEvents = await server.algolia.search('sdg_user_events', '', {
        filters: `userId:${userId}`,
        hitsPerPage: 50
      });
      userPreferences = { events: userEvents.hits };
    }
    
    // AI-powered result optimization
    const optimization = await server.ai.analyze({
      prompt: `Optimize search results for query "${query}" based on user preferences and business rules`,
      context: {
        originalResults: baseResults.hits,
        userPreferences,
        query,
        ...context
      },
      tools: ['ranking_optimization', 'personalization', 'business_rules']
    });
    
    return {
      success: true,
      optimizedResults: optimization.rerankedResults || baseResults.hits,
      personalizations: optimization.personalizations || [],
      aiInsights: optimization.insights,
      metadata: {
        originalCount: baseResults.hits.length,
        optimizedCount: optimization.rerankedResults?.length || baseResults.hits.length,
        processingTimeMs: optimization.processingTime
      }
    };
  } catch (error) {
    console.error('Error optimizing search results:', error);
    return { success: false, error: error.message };
  }
});

server.addToolHandler('track_conversion_funnel', async (params) => {
  const { eventType, userId, productId, metadata = {} } = params;
  
  try {
    // Store event in Algolia
    const eventData = {
      objectID: `${userId}_${Date.now()}`,
      userId,
      productId,
      eventType,
      timestamp: new Date().toISOString(),
      ...metadata
    };
    
    await server.algolia.saveObject('sdg_user_events', eventData);
    
    // Get funnel analytics
    const funnelData = await server.algolia.search('sdg_user_events', '', {
      filters: `userId:${userId}`,
      hitsPerPage: 100
    });
    
    // AI analysis of conversion funnel
    const funnelAnalysis = await server.ai.analyze({
      prompt: `Analyze conversion funnel performance for user ${userId} after ${eventType} event`,
      context: {
        currentEvent: eventData,
        userHistory: funnelData.hits,
        productId
      },
      tools: ['funnel_analysis', 'conversion_prediction', 'intervention_recommendations']
    });
    
    return {
      success: true,
      event: eventData,
      funnelInsights: funnelAnalysis.funnelMetrics,
      conversionProbability: funnelAnalysis.conversionProbability,
      recommendations: funnelAnalysis.interventionRecommendations,
      aiInsights: funnelAnalysis.insights
    };
  } catch (error) {
    console.error('Error tracking conversion funnel:', error);
    return { success: false, error: error.message };
  }
});

// Start the server
async function startServer() {
  try {
    await server.start();
    console.log(`
🚀 Algolia MCP Server started successfully!
🔗 Server URL: http://localhost:${server.config.server.port}
🤖 AI Features: ${Object.keys(server.config.features).filter(k => server.config.features[k]).join(', ')}
🛠️  Available Tools: ${server.tools.map(t => t.name).join(', ')}
    `);
  } catch (error) {
    console.error('Failed to start Algolia MCP Server:', error);
    process.exit(1);
  }
}

// Handle graceful shutdown
process.on('SIGINT', async () => {
  console.log('\n🛑 Shutting down Algolia MCP Server...');
  await server.stop();
  process.exit(0);
});

startServer();
