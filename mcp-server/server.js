const express = require('express');
const cors = require('cors');

const app = express();
const PORT = 3000;

// Middleware
app.use(cors());
app.use(express.json());

// Mock data with variations
const mockProducts = {
  'PROD018': {
    objectID: 'PROD018',
    name: 'Waterproof Hiking Backpack',
    price: 89.99,
    profit_margin: 0.35,
    category: 'Sports',
    brand: 'OutdoorPro',
    inventory_level: 150,
    average_rating: 4.6,
    number_of_reviews: 342,
    cost_basis: 58.49,
    market_position: 'premium'
  },
  'PROD017': {
    objectID: 'PROD017',
    name: 'High-Performance Running Shoes',
    price: 129.99,
    profit_margin: 0.42,
    category: 'Sports',
    brand: 'RunTech',
    inventory_level: 85,
    average_rating: 4.8,
    number_of_reviews: 567,
    cost_basis: 75.39,
    market_position: 'premium'
  },
  'PROD016': {
    objectID: 'PROD016',
    name: 'Professional DSLR Camera Kit',
    price: 899.99,
    profit_margin: 0.28,
    category: 'Electronics',
    brand: 'PhotoPro',
    inventory_level: 25,
    average_rating: 4.4,
    number_of_reviews: 189,
    cost_basis: 647.99,
    market_position: 'luxury'
  },
  'PROD015': {
    objectID: 'PROD015',
    name: 'Wireless Bluetooth Headphones',
    price: 199.99,
    profit_margin: 0.45,
    category: 'Electronics',
    brand: 'AudioMax',
    inventory_level: 200,
    average_rating: 4.7,
    number_of_reviews: 423,
    cost_basis: 109.99,
    market_position: 'premium'
  },
  'PROD014': {
    objectID: 'PROD014',
    name: 'Smart Fitness Watch',
    price: 249.99,
    profit_margin: 0.38,
    category: 'Wearables',
    brand: 'FitTech',
    inventory_level: 120,
    average_rating: 4.5,
    number_of_reviews: 298,
    cost_basis: 154.99,
    market_position: 'premium'
  },
  'PROD013': {
    objectID: 'PROD013',
    name: 'Premium Coffee Maker',
    price: 159.99,
    profit_margin: 0.40,
    category: 'Home & Kitchen',
    brand: 'BrewMaster',
    inventory_level: 75,
    average_rating: 4.3,
    number_of_reviews: 156,
    cost_basis: 95.99,
    market_position: 'premium'
  }
};

const mockMarketData = {
  'Sports': [
    {
      competitor_prices: [79.99, 95.99, 84.99, 119.99],
      demand_forecast: 'high',
      price_elasticity: 0.8,
      seasonal_trends: 'peak_hiking_season',
      market_share: 0.12,
      brand_strength: 'strong'
    }
  ],
  'Electronics': [
    {
      competitor_prices: [799.99, 999.99, 849.99, 1099.99],
      demand_forecast: 'medium',
      price_elasticity: 0.6,
      seasonal_trends: 'holiday_approach',
      market_share: 0.08,
      brand_strength: 'moderate'
    }
  ]
};

const mockHistoricalData = {
  'premium': [
    {
      historical_discounts: [8, 12, 18],
      conversion_rates: [0.15, 0.22, 0.28],
      revenue_impact: 'positive',
      customer_lifetime_value: 285.50,
      churn_risk: 'low',
      price_sensitivity: 'low'
    }
  ],
  'luxury': [
    {
      historical_discounts: [5, 10, 15],
      conversion_rates: [0.08, 0.14, 0.19],
      revenue_impact: 'mixed',
      customer_lifetime_value: 450.75,
      churn_risk: 'very_low',
      price_sensitivity: 'very_low'
    }
  ]
};

// MCP Endpoints
app.post('/mcp/tools/algolia_search', (req, res) => {
  console.log('🔍 MCP Algolia Search Request:', JSON.stringify(req.body, null, 2));
  
  const { arguments: args } = req.body;
  const { index_name, filters } = args;
  
  let response = { hits: [] };
  
  if (index_name === 'sdg_products') {
    // Extract product ID from filters
    const productId = filters?.match(/objectID:(\w+)/)?.[1];
    if (productId && mockProducts[productId]) {
      response.hits = [mockProducts[productId]];
    } else {
      // Return a random product if specific one not found
      const productIds = Object.keys(mockProducts);
      const randomId = productIds[Math.floor(Math.random() * productIds.length)];
      response.hits = [mockProducts[randomId]];
    }
  } else if (index_name === 'market_intelligence') {
    // Get market data based on category
    const categoryMatch = filters?.match(/product_category:'([^']+)'/);
    const category = categoryMatch?.[1] || 'Sports';
    
    if (mockMarketData[category]) {
      response.hits = mockMarketData[category];
    } else {
      // Default to Sports data with some randomization
      const baseData = mockMarketData.Sports[0];
      response.hits = [{
        ...baseData,
        demand_forecast: ['high', 'medium', 'low'][Math.floor(Math.random() * 3)],
        seasonal_trends: ['peak_season', 'normal', 'off_season'][Math.floor(Math.random() * 3)]
      }];
    }
  } else if (index_name === 'pricing_history') {
    // Get historical data based on market position
    const productId = filters?.match(/product_id:'(\w+)'/)?.[1];
    const product = mockProducts[productId];
    const marketPosition = product?.market_position || 'premium';
    
    if (mockHistoricalData[marketPosition]) {
      response.hits = mockHistoricalData[marketPosition];
    } else {
      // Generate dynamic historical data
      response.hits = [{
        historical_discounts: [5 + Math.floor(Math.random() * 10), 10 + Math.floor(Math.random() * 10), 15 + Math.floor(Math.random() * 10)],
        conversion_rates: [0.08 + Math.random() * 0.10, 0.15 + Math.random() * 0.10, 0.20 + Math.random() * 0.15],
        revenue_impact: ['positive', 'mixed', 'negative'][Math.floor(Math.random() * 3)],
        customer_lifetime_value: 200 + Math.random() * 300,
        churn_risk: ['low', 'medium', 'high'][Math.floor(Math.random() * 3)],
        price_sensitivity: ['low', 'medium', 'high'][Math.floor(Math.random() * 3)]
      }];
    }
  }
  
  console.log('📊 MCP Response:', JSON.stringify(response, null, 2));
  res.json(response);
});

app.post('/mcp/ai/analyze', (req, res) => {
  console.log('🤖 MCP AI Analysis Request:', JSON.stringify(req.body, null, 2));
  
  const { user_message, arguments: args } = req.body;
  const requestedDiscount = parseFloat(user_message.match(/Requested Discount: ([\d.]+)%/)?.[1] || '15');
  
  // Extract product data for dynamic analysis
  const productData = args?.product_data?.hits?.[0] || {};
  const marketData = args?.market_intelligence?.hits?.[0] || {};
  const historicalData = args?.historical_performance?.hits?.[0] || {};
  
  // Dynamic AI analysis based on actual data
  const profitMargin = productData.profit_margin || 0.35;
  const rating = productData.average_rating || 4.0;
  const inventory = productData.inventory_level || 100;
  const competitorPrices = marketData.competitor_prices || [79.99, 95.99, 84.99];
  const demandForecast = marketData.demand_forecast || 'medium';
  const seasonalTrends = marketData.seasonal_trends || 'normal';
  
  // Calculate dynamic recommended discount
  let recommendedDiscount = requestedDiscount;
  let confidenceScore = 0.8;
  let riskLevel = 'medium';
  
  // Risk assessment based on profit margin
  const maxSafeDiscount = Math.floor(profitMargin * 60); // 60% of profit margin
  
  if (requestedDiscount > maxSafeDiscount) {
    recommendedDiscount = maxSafeDiscount;
    confidenceScore = 0.6;
    riskLevel = 'high';
  } else if (requestedDiscount <= maxSafeDiscount * 0.7) {
    confidenceScore = 0.95;
    riskLevel = 'low';
  } else {
    confidenceScore = 0.75;
    riskLevel = 'medium';
  }
  
  // Adjust for market conditions
  if (demandForecast === 'high') {
    confidenceScore += 0.05;
    recommendedDiscount = Math.min(recommendedDiscount + 2, requestedDiscount);
  } else if (demandForecast === 'low') {
    confidenceScore -= 0.1;
    recommendedDiscount = Math.max(recommendedDiscount - 3, 5);
  }
  
  // Seasonal adjustments
  if (seasonalTrends === 'peak_hiking_season' || seasonalTrends === 'peak_season') {
    confidenceScore += 0.03;
  }
  
  // Rating influence
  if (rating >= 4.5) {
    confidenceScore += 0.02;
  } else if (rating < 4.0) {
    confidenceScore -= 0.05;
  }
  
  // Adjust for user type
  const userId = args?.user_id || '';
  if (userId.includes('premium') || userId.includes('vip')) {
    confidenceScore += 0.05;
    recommendedDiscount = Math.min(recommendedDiscount + 2, requestedDiscount);
    reasons.push('Premium customer status supports enhanced discount eligibility');
  } else if (userId.includes('new') || userId.includes('first-time')) {
    confidenceScore += 0.03;
    reasons.push('New customer acquisition strategy supports competitive pricing');
  }
  
  // Time-based variations
  const hour = new Date().getHours();
  if (hour >= 9 && hour <= 17) {
    // Business hours - more conservative
    confidenceScore -= 0.02;
  } else {
    // Off hours - slightly more aggressive
    confidenceScore += 0.01;
  }
  
  // Cap confidence at 0.95
  confidenceScore = Math.min(confidenceScore, 0.95);
  
  // Generate dynamic reasoning
  const reasons = [];
  reasons.push(`${requestedDiscount}% discount ${requestedDiscount <= recommendedDiscount ? 'approved' : 'optimized to ' + recommendedDiscount + '%'} based on ${Math.round(profitMargin * 100)}% profit margin`);
  
  if (rating >= 4.5) {
    reasons.push(`Strong product performance (${rating}/5 stars) supports discount viability`);
  }
  
  if (demandForecast === 'high') {
    reasons.push('High market demand provides pricing flexibility');
  } else if (demandForecast === 'low') {
    reasons.push('Conservative approach due to lower demand forecast');
  }
  
  if (inventory < 50) {
    reasons.push('Limited inventory suggests premium pricing strategy');
  } else if (inventory > 200) {
    reasons.push('High inventory levels support aggressive discounting');
  }
  
  // Dynamic alternative strategies
  const strategies = [];
  if (requestedDiscount > recommendedDiscount) {
    strategies.push(`Consider ${recommendedDiscount + 5}% discount with purchase of 2+ items`);
    strategies.push('Implement loyalty program benefits for frequent buyers');
  }
  
  if (seasonalTrends.includes('peak')) {
    strategies.push('Limited-time seasonal promotion with urgency messaging');
  } else {
    strategies.push('Create bundle offers with complementary products');
  }
  
  if (demandForecast === 'high') {
    strategies.push('Flash sale approach to capitalize on demand');
  } else {
    strategies.push('Extended promotion period to build momentum');
  }
  
  strategies.push('A/B test messaging: value vs. savings emphasis');
  
  // Generate dynamic market insights
  const marketInsights = [];
  const currentPrice = productData.price || 89.99;
  marketInsights.push(`${Math.round(profitMargin * 100)}% profit margin provides ${riskLevel} risk tolerance for discounting`);
  
  if (competitorPrices.length > 0) {
    const avgCompPrice = competitorPrices.reduce((a, b) => a + b, 0) / competitorPrices.length;
    const pricePosition = currentPrice > avgCompPrice ? 'premium' : currentPrice < avgCompPrice ? 'competitive' : 'market-aligned';
    marketInsights.push(`Current pricing is ${pricePosition} compared to competitors (avg: $${avgCompPrice.toFixed(2)})`);
  }
  
  if (demandForecast === 'high') {
    marketInsights.push('High demand forecast supports flexible pricing strategy');
  } else if (demandForecast === 'low') {
    marketInsights.push('Lower demand requires careful discount optimization');
  } else {
    marketInsights.push('Moderate demand allows for balanced pricing approach');
  }
  
  if (inventory < 50) {
    marketInsights.push(`Limited inventory (${inventory} units) suggests scarcity value`);
  } else if (inventory > 200) {
    marketInsights.push(`High inventory levels (${inventory} units) support promotional pricing`);
  } else {
    marketInsights.push(`Healthy inventory levels (${inventory} units) allow pricing flexibility`);
  }
  
  if (seasonalTrends.includes('peak')) {
    marketInsights.push('Peak season trends favor premium pricing with selective discounting');
  } else if (seasonalTrends.includes('off')) {
    marketInsights.push('Off-season conditions support aggressive promotional strategies');
  } else {
    marketInsights.push('Normal seasonal patterns allow standard discount approaches');
  }
  
  // Dynamic market impact assessment
  const avgCompetitorPrice = competitorPrices.reduce((a, b) => a + b, 0) / competitorPrices.length;
  const discountedPrice = currentPrice * (1 - recommendedDiscount / 100);
  
  let marketImpact = 'Neutral competitive positioning expected';
  if (discountedPrice < avgCompetitorPrice * 0.95) {
    marketImpact = 'Strong competitive advantage with aggressive pricing';
  } else if (discountedPrice > avgCompetitorPrice * 1.05) {
    marketImpact = 'Premium positioning maintained despite discount';
  }

  const analysis = {
    recommended_discount: recommendedDiscount,
    confidence_score: Math.round(confidenceScore * 100) / 100,
    risk_level: riskLevel,
    reasoning: reasons.join('. ') + '.',
    market_impact: marketImpact,
    market_insights: marketInsights,
    alternative_strategies: strategies
  };
  
  const response = {
    analysis: analysis,
    model: 'claude-3-sonnet',
    timestamp: new Date().toISOString()
  };
  
  console.log('🧠 AI Analysis Response:', JSON.stringify(response, null, 2));
  res.json(response);
});

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ 
    status: 'healthy', 
    service: 'algolia-mcp-server',
    timestamp: new Date().toISOString()
  });
});

// Start server
app.listen(PORT, () => {
  console.log(`🚀 Algolia MCP Server running on http://localhost:${PORT}`);
  console.log(`📋 Available endpoints:`);
  console.log(`   POST /mcp/tools/algolia_search - Product and market data search`);
  console.log(`   POST /mcp/ai/analyze - AI-powered discount analysis`);
  console.log(`   GET /health - Health check`);
});

// Graceful shutdown
process.on('SIGTERM', () => {
  console.log('🔄 Received SIGTERM, shutting down gracefully');
  process.exit(0);
});

process.on('SIGINT', () => {
  console.log('🔄 Received SIGINT, shutting down gracefully');
  process.exit(0);
});
