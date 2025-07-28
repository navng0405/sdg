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
const axios = require('axios');

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

  async start() {
    return new Promise((resolve, reject) => {
      try {
        this.app.listen(this.port, () => {
          logger.info(`🚀 Algolia MCP Server started on port ${this.port}`);
          logger.info(`📡 Health check: http://localhost:${this.port}/health`);
          resolve();
        });
      } catch (error) {
        logger.error('❌ Failed to start server:', error);
        reject(error);
      }
    });
  }

  // Tool method implementations
  async generateSmartDiscount(req, res) {
    try {
      const { userId, productId, userBehavior, requestedDiscount, aiProvider } = req.body || {};
      logger.info(`🎯 Generating smart discount for user ${userId}, product ${productId}`);

      // Choose AI provider (default: Claude, fallback: Gemini)
      let aiResult;
      const prompt = `Generate a discount recommendation for user ${userId} on product ${productId} with requested discount ${requestedDiscount}. Consider user behavior: ${JSON.stringify(userBehavior)}`;
      if (aiProvider === 'gemini' && GEMINI_API_KEY) {
        aiResult = await callGemini(prompt);
      } else if (ANTHROPIC_API_KEY) {
        aiResult = await callClaude(prompt);
      } else if (GEMINI_API_KEY) {
        aiResult = await callGemini(prompt);
      } else {
        throw new Error('No AI API key available');
      }

      // Example: parse AI result (customize as needed)
      const discountValue = requestedDiscount || 15;
      const discountData = {
        userId,
        productId,
        discount: {
          type: 'percentage',
          value: discountValue,
          code: `SMART${Math.random().toString(36).substr(2, 6).toUpperCase()}`,
          expiresIn: 1800,
          reason: 'AI-generated recommendation'
        },
        aiProvider: aiProvider || (ANTHROPIC_API_KEY ? 'claude' : 'gemini'),
        aiResult
      };
      if (res) {
        res.json({ success: true, data: discountData });
      }
      return discountData;
    } catch (error) {
      logger.error('❌ Error generating smart discount:', error);
      const errorResult = { success: false, error: error.message };
      if (res) res.status(500).json(errorResult);
      return errorResult;
    }
  }

  async analyzeUserJourney(req, res) {
    try {
      const { userId, sessionData, timeRange } = req.body || {};
      
      logger.info(`📊 Analyzing user journey for ${userId}`);
      
      // Mock user journey analysis
      const journeyData = {
        userId,
        analysis: {
          sessionDuration: '8m 34s',
          pagesViewed: 12,
          searchQueries: ['wireless headphones', 'bluetooth speakers', 'audio equipment'],
          conversionProbability: 0.73,
          recommendedActions: ['Show price comparison', 'Highlight free shipping', 'Display customer reviews']
        },
        insights: {
          userType: 'price_conscious_researcher',
          buyingStage: 'consideration',
          riskFactors: ['price_sensitivity', 'comparison_shopping']
        }
      };
      
      if (res) {
        res.json({ success: true, data: journeyData });
      }
      return journeyData;
      
    } catch (error) {
      logger.error('❌ Error analyzing user journey:', error);
      const errorResult = { success: false, error: error.message };
      if (res) res.status(500).json(errorResult);
      return errorResult;
    }
  }

  async optimizeSearchResults(req, res) {
    try {
      const { query, userId, context } = req.body || {};
      
      logger.info(`🔍 Optimizing search results for query: "${query}"`);
      
      // Mock search optimization
      const optimizedResults = {
        query,
        userId,
        optimization: {
          originalResultCount: 156,
          optimizedResultCount: 24,
          personalizationApplied: true,
          boostFactors: ['user_preferences', 'purchase_history', 'trending_items']
        },
        suggestions: {
          queryRefinements: ['wireless bluetooth headphones', 'noise cancelling headphones'],
          categoryFilters: ['Electronics > Audio', 'Electronics > Headphones'],
          priceRange: { min: 50, max: 300 }
        }
      };
      
      if (res) {
        res.json({ success: true, data: optimizedResults });
      }
      return optimizedResults;
      
    } catch (error) {
      logger.error('❌ Error optimizing search results:', error);
      const errorResult = { success: false, error: error.message };
      if (res) res.status(500).json(errorResult);
      return errorResult;
    }
  }

  async trackConversionFunnel(req, res) {
    try {
      const { eventType, userId, productId, metadata } = req.body || {};
      
      logger.info(`📈 Tracking conversion event: ${eventType} for user ${userId}`);
      
      // Mock conversion tracking
      const trackingData = {
        eventType,
        userId,
        productId,
        timestamp: new Date().toISOString(),
        funnelStage: this.determineFunnelStage(eventType),
        conversionMetrics: {
          stageConversionRate: 0.68,
          timeInStage: '2m 15s',
          dropOffRisk: 'medium',
          nextBestAction: 'show_social_proof'
        },
        recommendations: {
          interventions: ['display_urgency_banner', 'show_similar_products'],
          timing: 'immediate',
          priority: 'high'
        }
      };
      
      if (res) {
        res.json({ success: true, data: trackingData });
      }
      return trackingData;
      
    } catch (error) {
      logger.error('❌ Error tracking conversion funnel:', error);
      const errorResult = { success: false, error: error.message };
      if (res) res.status(500).json(errorResult);
      return errorResult;
    }
  }

  // Helper method for funnel stage determination
  determineFunnelStage(eventType) {
    const stageMap = {
      'page_view': 'awareness',
      'product_view': 'interest',
      'add_to_cart': 'consideration',
      'checkout_start': 'intent',
      'purchase': 'conversion',
      'cart_abandon': 'abandonment'
    };
    return stageMap[eventType] || 'unknown';
  }

  async stop() {
    logger.info('🛑 Stopping Algolia MCP Server...');
  }
}

// Read AI keys from environment
const ANTHROPIC_API_KEY = process.env.ANTHROPIC_API_KEY;
const GEMINI_API_KEY = process.env.GEMINI_API_KEY;

// Helper: Claude API call
async function callClaude(prompt) {
  if (!ANTHROPIC_API_KEY) throw new Error('Claude API key not set');
  const response = await axios.post(
    'https://api.anthropic.com/v1/messages',
    {
      model: 'claude-3-sonnet-20240229',
      max_tokens: 512,
      messages: [{ role: 'user', content: prompt }]
    },
    {
      headers: {
        'x-api-key': ANTHROPIC_API_KEY,
        'content-type': 'application/json',
        'anthropic-version': '2023-06-01'
      }
    }
  );
  return response.data;
}

// Helper: Gemini API call
async function callGemini(prompt) {
  if (!GEMINI_API_KEY) throw new Error('Gemini API key not set');
  const response = await axios.post(
    'https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=' + GEMINI_API_KEY,
    {
      contents: [{ parts: [{ text: prompt }] }]
    },
    {
      headers: { 'content-type': 'application/json' }
    }
  );
  return response.data;
}

// Configure Algolia MCP Server with your credentials
const server = new AlgoliaIntegratedMcpServer();

// Start the server
server.start().catch(error => {
  logger.error('❌ Failed to start MCP server:', error);
  process.exit(1);
});
