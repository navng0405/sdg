/**
 * MCP-Enhanced Application JavaScript
 * Provides intelligent UI interactions powered by Algolia MCP Server + Claude AI
 */

// Application State
let mcpChatVisible = false;
let countdownTimer = null;
let currentAnalysis = null;
let selectedProduct = null;
let availableProducts = [];

// DOM Elements
const elements = {
    generateButton: document.getElementById('generate-intelligent-discount'),
    userId: document.getElementById('user-id'),
    requestedDiscount: document.getElementById('requested-discount'),
    userIntent: document.getElementById('user-intent'),
    productSelector: document.getElementById('product-selector'),
    analysisContainer: document.getElementById('ai-analysis-container'),
    discountBanner: document.getElementById('discount-banner'),
    aiReasoning: document.getElementById('ai-reasoning'),
    confidenceFill: document.getElementById('confidence-fill'),
    confidencePercentage: document.getElementById('confidence-percentage'),
    marketInsights: document.getElementById('market-insights'),
    aiRecommendations: document.getElementById('ai-recommendations'),
    discountAmount: document.getElementById('discount-amount'),
    discountCode: document.getElementById('discount-code'),
    discountHeadline: document.getElementById('discount-headline'),
    discountMessage: document.getElementById('discount-message'),
    countdown: document.getElementById('countdown'),
    mcpChatFab: document.getElementById('mcp-chat-fab'),
    mcpChatContainer: document.getElementById('mcp-chat-container'),
    mcpChatMessages: document.getElementById('mcp-chat-messages'),
    chatInput: document.getElementById('chat-input'),
    productSearch: document.getElementById('product-search'),
    searchButton: document.getElementById('search-products'),
    productGrid: document.getElementById('product-grid'),
    selectedProductDiv: document.getElementById('selected-product'),
    productImage: document.getElementById('product-image'),
    productName: document.getElementById('product-name'),
    productPrice: document.getElementById('product-price'),
    productCategory: document.getElementById('product-category'),
    stockStatus: document.getElementById('stock-status'),
    profitMargin: document.getElementById('profit-margin')
};

/**
 * Initialize the MCP-Enhanced Application
 */
document.addEventListener('DOMContentLoaded', function() {
    initializeApp();
    setupEventListeners();
    updateAnalytics();
});

/**
 * Initialize application
 */
function initializeApp() {
    console.log('🤖 Initializing MCP-Enhanced Smart Discount Generator...');
    
    // Set initial product data
    updateProductDisplay();
    
    // Start analytics updates
    setInterval(updateAnalytics, 30000); // Update every 30 seconds
    
    console.log('✅ MCP Application initialized successfully');
}

/**
 * Setup event listeners
 */
function setupEventListeners() {
    // Main discount generation button
    elements.generateButton?.addEventListener('click', generateIntelligentDiscount);
    
    // MCP Chat functionality
    elements.mcpChatFab?.addEventListener('click', toggleMcpChat);
    
    // Chat input handling
    elements.chatInput?.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            sendChatMessage();
        }
    });
    
    // Product selection change
    elements.productSelector?.addEventListener('change', updateSelectedProduct);
    
    // Product search functionality
    elements.searchButton?.addEventListener('click', searchProducts);
    elements.productSearch?.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            searchProducts();
        }
    });
    
    // Load initial products
    loadInitialProducts();
}

/**
 * Load initial products from Algolia
 */
async function loadInitialProducts() {
    try {
        console.log('🔍 Loading initial products...');
        elements.productGrid.innerHTML = '<div class="loading-spinner"></div> Loading products...';
        
        const response = await fetch('/api/algolia/products?limit=12');
        if (response.ok) {
            const products = await response.json();
            availableProducts = products;
            displayProducts(products);
        } else {
            // Fallback to demo products
            availableProducts = getDemoProducts();
            displayProducts(availableProducts);
        }
    } catch (error) {
        console.error('Error loading products:', error);
        // Fallback to demo products
        availableProducts = getDemoProducts();
        displayProducts(availableProducts);
    }
}

/**
 * Search products based on user input
 */
async function searchProducts() {
    const query = elements.productSearch.value.trim();
    
    if (!query) {
        displayProducts(availableProducts);
        return;
    }
    
    try {
        console.log('🔍 Searching products for:', query);
        elements.productGrid.innerHTML = '<div class="loading-spinner"></div> Searching...';
        
        const response = await fetch(`/api/algolia/search?query=${encodeURIComponent(query)}&limit=12`);
        if (response.ok) {
            const results = await response.json();
            displayProducts(results.hits || results);
        } else {
            // Fallback to local search
            const filtered = availableProducts.filter(product => 
                product.name.toLowerCase().includes(query.toLowerCase()) ||
                product.category.toLowerCase().includes(query.toLowerCase())
            );
            displayProducts(filtered);
        }
    } catch (error) {
        console.error('Error searching products:', error);
        // Fallback to local search
        const filtered = availableProducts.filter(product => 
            product.name.toLowerCase().includes(query.toLowerCase()) ||
            product.category.toLowerCase().includes(query.toLowerCase())
        );
        displayProducts(filtered);
    }
}

/**
 * Display products in the grid
 */
function displayProducts(products) {
    if (!products || products.length === 0) {
        elements.productGrid.innerHTML = '<p style="text-align: center; color: #666; padding: 40px;">No products found. Try a different search term.</p>';
        return;
    }
    
    elements.productGrid.innerHTML = products.map(product => `
        <div class="product-grid-item" onclick="selectProduct('${product.objectID || product.id}')">
            <img src="${product.image_url || product.image || 'https://images.unsplash.com/photo-1560472354-b33ff0c44a43?w=200&h=200&fit=crop'}" 
                 alt="${product.name}" 
                 onerror="this.src='https://images.unsplash.com/photo-1560472354-b33ff0c44a43?w=200&h=200&fit=crop'">
            <h5>${product.name}</h5>
            <div class="price">$${product.price}</div>
            <div class="category">${product.category}</div>
        </div>
    `).join('');
    
    console.log(`📦 Displayed ${products.length} products`);
}

/**
 * Select a product for discount generation
 */
function selectProduct(productId) {
    const product = availableProducts.find(p => (p.objectID || p.id) === productId);
    if (!product) {
        console.error('Product not found:', productId);
        return;
    }
    
    selectedProduct = product;
    
    // Update selected product display
    elements.productImage.src = product.image_url || product.image || 'https://images.unsplash.com/photo-1560472354-b33ff0c44a43?w=200&h=200&fit=crop';
    elements.productName.textContent = product.name;
    elements.productPrice.textContent = `$${product.price}`;
    elements.productCategory.textContent = product.category;
    elements.stockStatus.textContent = product.stock > 0 ? 'In Stock' : 'Out of Stock';
    elements.profitMargin.textContent = `Margin: ${product.profit_margin || '35'}%`;
    
    // Show selected product section
    elements.selectedProductDiv.style.display = 'block';
    
    // Update grid selection
    document.querySelectorAll('.product-grid-item').forEach(item => {
        item.classList.remove('selected');
    });
    event.target.closest('.product-grid-item').classList.add('selected');
    
    console.log('✅ Selected product:', product.name);
}

/**
 * Get demo products as fallback
 */
function getDemoProducts() {
    return [
        {
            id: 'PROD018',
            objectID: 'PROD018',
            name: 'Waterproof Hiking Backpack',
            price: 89.99,
            category: 'Sports',
            image_url: 'https://images.unsplash.com/photo-1620953749696-38989c40eadb?w=300',
            stock: 15,
            profit_margin: 35
        },
        {
            id: 'PROD017',
            objectID: 'PROD017',
            name: 'High-Performance Running Shoes',
            price: 129.99,
            category: 'Sports',
            image_url: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=300',
            stock: 8,
            profit_margin: 42
        },
        {
            id: 'PROD016',
            objectID: 'PROD016',
            name: 'Professional DSLR Camera Kit',
            price: 899.99,
            category: 'Electronics',
            image_url: 'https://images.unsplash.com/photo-1606983340126-99ab4feaa64a?w=300',
            stock: 3,
            profit_margin: 28
        },
        {
            id: 'PROD015',
            objectID: 'PROD015',
            name: 'Wireless Bluetooth Headphones',
            price: 199.99,
            category: 'Electronics',
            image_url: 'https://images.unsplash.com/photo-1572635196237-14b3f281503f?w=300',
            stock: 12,
            profit_margin: 45
        },
        {
            id: 'PROD014',
            objectID: 'PROD014',
            name: 'Smart Fitness Watch',
            price: 249.99,
            category: 'Electronics',
            image_url: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=300',
            stock: 6,
            profit_margin: 38
        },
        {
            id: 'PROD013',
            objectID: 'PROD013',
            name: 'Premium Coffee Maker',
            price: 159.99,
            category: 'Home',
            image_url: 'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=300',
            stock: 9,
            profit_margin: 32
        }
    ];
}

/**
 * Generate AI-Optimized Discount - Main MCP functionality
 */
async function generateIntelligentDiscount() {
    try {
        const button = elements.generateButton;
        const originalText = button.innerHTML;
        
        // Show loading state
        button.innerHTML = '<i class="fas fa-spinner fa-spin"></i> AI Analyzing...';
        button.disabled = true;
        
        // Show analysis container
        elements.analysisContainer.style.display = 'block';
        elements.aiReasoning.innerHTML = 'Connecting to Algolia MCP Server... <span class="processing-indicator pulse">🧠 AI analyzing market conditions</span>';
        
        // Animate confidence score to show processing
        animateConfidenceScore(0, 25, 1000);
        
        // Check if product is selected
        if (!selectedProduct) {
            alert('Please select a product first!');
            button.innerHTML = originalText;
            button.disabled = false;
            return;
        }
        
        // Get form data
        const userId = elements.userId.value || 'user-001';
        const requestedDiscount = parseFloat(elements.requestedDiscount.value) || 15;
        const userIntent = elements.userIntent.value || 'price_sensitive';
        const productId = selectedProduct.objectID || selectedProduct.id;
        
        console.log('🎯 Generating discount for:', { userId, productId, requestedDiscount, userIntent, productName: selectedProduct.name });
        
        // Call MCP API
        const response = await fetch(`/api/mcp/intelligent-discount?userId=${encodeURIComponent(userId)}&productId=${encodeURIComponent(productId)}&requestedDiscount=${requestedDiscount}&userIntent=${encodeURIComponent(userIntent)}`);
        
        if (!response.ok) {
            if (response.status === 503) {
                throw new Error('MCP service temporarily unavailable - using fallback analysis');
            } else if (response.status === 500) {
                throw new Error('AI analysis service error - please try again');
            } else {
                throw new Error(`Service error (${response.status}) - please try again`);
            }
        }
        
        const data = await response.json();
        console.log('🤖 MCP Response:', data);
        
        // Store analysis for later reference
        currentAnalysis = data;
        
        // Update UI with AI analysis
        await displayAiAnalysis(data);
        
        // Show discount banner if discount was generated
        if (data.discount) {
            displayDiscountBanner(data.discount);
        }
        
        // Update analytics
        updateAnalytics();
        
    } catch (error) {
        console.error('❌ Error generating intelligent discount:', error);
        
        // Show appropriate error message based on error type
        let errorMessage = 'Failed to connect to AI service. ';
        if (error.message.includes('MCP service temporarily unavailable')) {
            errorMessage += 'Using fallback analysis instead.';
            showWarningState(errorMessage);
        } else if (error.message.includes('AI analysis service error')) {
            errorMessage += 'Please try again in a moment.';
            showErrorState(errorMessage);
        } else {
            errorMessage += 'Please check your connection and try again.';
            showErrorState(errorMessage);
        }
    } finally {
        // Restore button
        elements.generateButton.innerHTML = '<i class="fas fa-brain"></i> Generate AI-Optimized Discount';
        elements.generateButton.disabled = false;
    }
}

/**
 * Display AI Analysis Results
 */
async function displayAiAnalysis(data) {
    const discount = data.discount;
    
    if (!discount) {
        elements.aiReasoning.innerHTML = '⚠️ AI analysis completed but no discount approved at this time.';
        return;
    }
    
    // Update reasoning
    elements.aiReasoning.innerHTML = `
        <strong>AI Analysis Complete:</strong> ${discount.reasoning || discount.ai_reasoning || 'AI has analyzed market conditions and user behavior patterns.'}
    `;
    
    // Animate confidence score
    const confidenceScore = Math.round((discount.confidence_score || 0.84) * 100);
    await animateConfidenceScore(25, confidenceScore, 1500);
    
    // Display market insights
    if (discount.market_insights) {
        displayMarketInsights(discount.market_insights);
    } else {
        displayMarketInsights([
            "Current market demand for outdoor equipment is high",
            "Competitor pricing analysis shows 10-15% discount range",
            "User shows strong purchase intent based on behavior patterns"
        ]);
    }
    
    // Display AI recommendations
    if (discount.ai_recommendations || discount.alternative_strategies) {
        displayAiRecommendations(discount.ai_recommendations || discount.alternative_strategies);
    } else {
        displayAiRecommendations([
            "Recommended discount range: 10-15%",
            "Add urgency timer for conversion boost",
            "Follow up with email campaign if not converted"
        ]);
    }
}

/**
 * Display Market Insights
 */
function displayMarketInsights(insights) {
    console.log('📊 Displaying market insights:', insights);
    
    if (!insights) {
        // Fallback insights
        elements.marketInsights.innerHTML = `
            <div class="insight-item"><i class="fas fa-chart-line"></i> Market analysis in progress</div>
            <div class="insight-item"><i class="fas fa-trending-up"></i> Competitive pricing evaluated</div>
            <div class="insight-item"><i class="fas fa-users"></i> Customer behavior patterns analyzed</div>
        `;
        return;
    }
    
    let insightItems = [];
    
    // Handle insights from backend: {analysis_type: "...", insights: [...]}
    if (insights.insights && Array.isArray(insights.insights)) {
        insightItems = insights.insights;
    }
    // Handle direct array format
    else if (Array.isArray(insights)) {
        insightItems = insights;
    } 
    // Handle object with various properties
    else if (typeof insights === 'object') {
        if (insights.market_impact) {
            insightItems.push(`Market Impact: ${insights.market_impact}`);
        }
        if (insights.demand_forecast) {
            insightItems.push(`Demand Forecast: ${insights.demand_forecast}`);
        }
        if (insights.competitive_position) {
            insightItems.push(`Competitive Position: ${insights.competitive_position}`);
        }
        if (insights.price_elasticity) {
            insightItems.push(`Price Elasticity: ${insights.price_elasticity}`);
        }
        if (insights.seasonal_trends) {
            insightItems.push(`Seasonal Trends: ${insights.seasonal_trends}`);
        }
        
        // If no specific properties found, try to extract values
        if (insightItems.length === 0) {
            Object.keys(insights).forEach(key => {
                if (typeof insights[key] === 'string' && key !== 'analysis_type') {
                    insightItems.push(`${key.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())}: ${insights[key]}`);
                }
            });
        }
    } 
    // Handle string format
    else if (typeof insights === 'string') {
        insightItems = [insights];
    }
    
    // Fallback if still no insights
    if (insightItems.length === 0) {
        insightItems = [
            'Advanced market analysis completed',
            'Competitive intelligence gathered',
            'Customer segmentation analyzed'
        ];
    }
    
    elements.marketInsights.innerHTML = insightItems.map(insight => 
        `<div class="insight-item"><i class="fas fa-chart-line"></i> ${insight}</div>`
    ).join('');
}

/**
 * Display AI Recommendations
 */
function displayAiRecommendations(recommendations) {
    console.log('💡 Displaying AI recommendations:', recommendations);
    
    if (!recommendations) {
        // Fallback recommendations
        elements.aiRecommendations.innerHTML = `
            <div class="recommendation-item"><i class="fas fa-lightbulb"></i> Recommended discount range: 10-15%</div>
            <div class="recommendation-item"><i class="fas fa-clock"></i> Add urgency timer for conversion boost</div>
            <div class="recommendation-item"><i class="fas fa-envelope"></i> Follow up with email campaign if not converted</div>
        `;
        return;
    }
    
    if (Array.isArray(recommendations)) {
        elements.aiRecommendations.innerHTML = recommendations.map(rec => 
            `<div class="recommendation-item"><i class="fas fa-lightbulb"></i> ${rec}</div>`
        ).join('');
    } else if (typeof recommendations === 'object') {
        // Handle object recommendations
        const recItems = [];
        
        if (recommendations.alternative_strategies && Array.isArray(recommendations.alternative_strategies)) {
            recItems.push(...recommendations.alternative_strategies);
        }
        if (recommendations.strategies && Array.isArray(recommendations.strategies)) {
            recItems.push(...recommendations.strategies);
        }
        if (recommendations.recommendations && Array.isArray(recommendations.recommendations)) {
            recItems.push(...recommendations.recommendations);
        }
        
        // If object has direct string properties, extract them
        if (recItems.length === 0) {
            Object.keys(recommendations).forEach(key => {
                if (typeof recommendations[key] === 'string') {
                    recItems.push(`${key.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())}: ${recommendations[key]}`);
                } else if (Array.isArray(recommendations[key])) {
                    recItems.push(...recommendations[key]);
                }
            });
        }
        
        // Fallback if no recommendations found
        if (recItems.length === 0) {
            recItems.push('Optimize discount timing for peak conversion');
            recItems.push('Consider bundle offers for increased value');
            recItems.push('Implement loyalty program benefits');
        }
        
        elements.aiRecommendations.innerHTML = recItems.map(rec => 
            `<div class="recommendation-item"><i class="fas fa-lightbulb"></i> ${rec}</div>`
        ).join('');
        
    } else if (typeof recommendations === 'string') {
        elements.aiRecommendations.innerHTML = `<div class="recommendation-item"><i class="fas fa-lightbulb"></i> ${recommendations}</div>`;
    } else {
        // Unknown format, show fallback
        elements.aiRecommendations.innerHTML = `
            <div class="recommendation-item"><i class="fas fa-lightbulb"></i> AI strategy optimization complete</div>
            <div class="recommendation-item"><i class="fas fa-target"></i> Personalized approach recommended</div>
            <div class="recommendation-item"><i class="fas fa-chart-bar"></i> Performance metrics optimized</div>
        `;
    }
}

/**
 * Display Discount Banner
 */
function displayDiscountBanner(discount) {
    // Update discount details
    elements.discountAmount.textContent = discount.amount || `${discount.value}% OFF`;
    elements.discountCode.textContent = discount.code || 'SMART15';
    elements.discountHeadline.textContent = discount.headline || 'Special AI-Optimized Offer!';
    elements.discountMessage.textContent = discount.message || 'Your personalized discount is ready!';
    
    // Show banner
    elements.discountBanner.style.display = 'block';
    
    // Start countdown timer
    startCountdown(discount.expires_in_seconds || 1800);
    
    // Scroll to banner
    elements.discountBanner.scrollIntoView({ behavior: 'smooth' });
}

/**
 * Animate Confidence Score
 */
function animateConfidenceScore(fromPercent, toPercent, duration) {
    return new Promise(resolve => {
        const startTime = Date.now();
        const startPercent = fromPercent;
        const endPercent = toPercent;
        
        function animate() {
            const now = Date.now();
            const elapsed = now - startTime;
            const progress = Math.min(elapsed / duration, 1);
            
            const currentPercent = startPercent + (endPercent - startPercent) * progress;
            
            elements.confidenceFill.style.width = `${currentPercent}%`;
            elements.confidencePercentage.textContent = `${Math.round(currentPercent)}%`;
            
            if (progress < 1) {
                requestAnimationFrame(animate);
            } else {
                resolve();
            }
        }
        
        animate();
    });
}

/**
 * Start Countdown Timer
 */
function startCountdown(seconds) {
    let timeLeft = seconds;
    
    if (countdownTimer) {
        clearInterval(countdownTimer);
    }
    
    countdownTimer = setInterval(() => {
        const minutes = Math.floor(timeLeft / 60);
        const secs = timeLeft % 60;
        elements.countdown.textContent = `${minutes}:${secs.toString().padStart(2, '0')}`;
        
        timeLeft--;
        
        if (timeLeft < 0) {
            clearInterval(countdownTimer);
            elements.countdown.textContent = 'Expired';
            elements.discountBanner.style.opacity = '0.6';
        }
    }, 1000);
}

/**
 * Toggle MCP Chat
 */
function toggleMcpChat() {
    mcpChatVisible = !mcpChatVisible;
    
    if (mcpChatVisible) {
        elements.mcpChatContainer.style.display = 'flex';
        elements.mcpChatFab.style.display = 'none';
        elements.chatInput.focus();
    } else {
        elements.mcpChatContainer.style.display = 'none';
        elements.mcpChatFab.style.display = 'block';
    }
}

/**
 * Send Chat Message
 */
async function sendChatMessage() {
    const input = elements.chatInput;
    const message = input.value.trim();
    
    if (!message) return;
    
    // Add user message
    addChatMessage(message, 'user');
    input.value = '';
    
    // Show typing indicator
    addChatMessage('AI is thinking...', 'assistant', true);
    
    try {
        // Call the actual MCP chat endpoint
        const response = await fetch('/api/mcp/chat/intelligent-assistant', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                message: message,
                user_id: elements.userId?.value || 'demo-user'
            })
        });
        
        // Remove typing indicator
        const typingMessage = elements.mcpChatMessages.lastElementChild;
        if (typingMessage && typingMessage.classList.contains('typing')) {
            typingMessage.remove();
        }
        
        if (response.ok) {
            const data = await response.json();
            
            // Add AI response
            addChatMessage(data.message, 'assistant');
            
            // If there are suggestions (like discounts or products), show them
            if (data.suggestions && data.suggestions.length > 0) {
                const suggestionsHtml = data.suggestions.map(suggestion => {
                    if (suggestion.type === 'product_recommendation') {
                        return `<div class="chat-suggestion product-suggestion">
                            🎧 <strong>${suggestion.product_name}</strong>
                            <br>� <span style="color: #5468ff; font-weight: bold;">${suggestion.price}</span>
                            <br>⭐ ${suggestion.rating} (${suggestion.reviews})
                            <br>🔧 ${suggestion.key_features}
                            <br><small style="color: #666;">✨ ${suggestion.reasoning}</small>
                        </div>`;
                    } else if (suggestion.type === 'discount_opportunity') {
                        return `<div class="chat-suggestion discount-suggestion">
                            💰 <strong>${suggestion.product_name || 'Special Offer'}</strong>
                            <br>🏷️ <span style="color: #ff6b6b; font-weight: bold;">${suggestion.discount || suggestion.discounted_price}</span>
                            ${suggestion.original_price ? `<span style="text-decoration: line-through; color: #999;"> ${suggestion.original_price}</span>` : ''}
                            <br><small style="color: #666;">✨ ${suggestion.reasoning}</small>
                            ${suggestion.confidence ? `<br><small>Confidence: ${(suggestion.confidence * 100).toFixed(1)}%</small>` : ''}
                        </div>`;
                    } else if (suggestion.type === 'eco_product') {
                        return `<div class="chat-suggestion eco-suggestion">
                            🌱 <strong>${suggestion.product_name}</strong>
                            <br>💰 ${suggestion.price}
                            <br>⭐ ${suggestion.rating} | 🌍 Eco Score: ${suggestion.eco_score}
                            <br><small style="color: #666;">✨ ${suggestion.reasoning}</small>
                        </div>`;
                    } else {
                        return `<div class="chat-suggestion">
                            💡 <strong>${suggestion.discount || suggestion.product_name || 'Recommendation'}</strong>
                            <br><small>${suggestion.reasoning}</small>
                            ${suggestion.confidence ? `<br><small>Confidence: ${(suggestion.confidence * 100).toFixed(1)}%</small>` : ''}
                        </div>`;
                    }
                }).join('');
                addChatMessage(suggestionsHtml, 'assistant');
            }
        } else {
            addChatMessage('Sorry, I\'m having trouble processing your request right now. Please try again.', 'assistant');
        }
        
    } catch (error) {
        console.error('Chat error:', error);
        // Remove typing indicator if there's an error
        const typingMessage = elements.mcpChatMessages.lastElementChild;
        if (typingMessage && typingMessage.classList.contains('typing')) {
            typingMessage.remove();
        }
        addChatMessage('Sorry, I\'m having trouble connecting right now. Please try again.', 'assistant');
    }
}

/**
 * Add Chat Message
 */
function addChatMessage(message, sender, isTyping = false) {
    const messageDiv = document.createElement('div');
    messageDiv.className = `chat-message ${sender}`;
    
    if (isTyping) {
        messageDiv.classList.add('typing');
    }
    
    if (sender === 'assistant') {
        messageDiv.innerHTML = `
            <div class="mcp-badge">AI</div>
            <p>${message}</p>
        `;
    } else {
        messageDiv.innerHTML = `
            <div class="user-badge">You</div>
            <p>${message}</p>
        `;
    }
    
    elements.mcpChatMessages.appendChild(messageDiv);
    elements.mcpChatMessages.scrollTop = elements.mcpChatMessages.scrollHeight;
}

/**
 * Generate AI Response (simplified for demo)
 */
function generateAiResponse(userMessage) {
    const message = userMessage.toLowerCase();
    
    if (message.includes('discount') || message.includes('price')) {
        return 'I can help you find the best AI-optimized discounts! Our MCP-powered system analyzes market conditions, user behavior, and profit margins to generate personalized offers. Would you like me to analyze a specific product?';
    } else if (message.includes('product') || message.includes('recommend')) {
        return 'Based on our Algolia-powered analysis, I can recommend products that match your preferences and current market trends. What type of product are you looking for?';
    } else if (message.includes('how') || message.includes('work')) {
        return 'Our system uses Algolia\'s MCP (Model Context Protocol) server combined with Claude AI to analyze real-time market data, user behavior patterns, and competitive pricing to generate intelligent discount recommendations that protect profit margins while maximizing conversion rates.';
    } else {
        return 'I\'m your AI shopping assistant powered by Algolia MCP and Claude AI. I can help you with product recommendations, intelligent discount analysis, and personalized shopping insights. What would you like to know?';
    }
}

/**
 * Update Analytics Dashboard
 */
async function updateAnalytics() {
    try {
        const response = await fetch('/api/mcp/analytics/intelligent-insights?days=7');
        
        if (response.ok) {
            const data = await response.json();
            
            // Update analytics cards with real data
            if (data.profit_protection) {
                document.getElementById('total-analyses').textContent = data.profit_protection.total_mcp_analyses || '156';
                document.getElementById('optimizations').textContent = data.profit_protection.ai_optimizations || '43';
                document.getElementById('protected-revenue').textContent = `$${(data.profit_protection.profit_protected_revenue / 1000).toFixed(1)}K` || '$12.8K';
                document.getElementById('avg-confidence').textContent = `${(data.profit_protection.average_confidence_score * 100).toFixed(1)}%` || '84.7%';
            }
        }
    } catch (error) {
        console.log('Analytics update failed, using demo data');
        // Keep existing demo values
    }
}

/**
 * Product data for dynamic selection
 */
const productCatalog = {
    'PROD018': {
        name: 'Waterproof Hiking Backpack',
        price: '$89.99',
        description: 'Durable and waterproof hiking backpack with multiple compartments',
        category: 'Sports',
        margin: '35%',
        rating: '4.6★',
        image: 'https://images.unsplash.com/photo-1620953749696-38989c40eadb?w=300'
    },
    'PROD017': {
        name: 'High-Performance Running Shoes',
        price: '$129.99',
        description: 'Professional running shoes with advanced cushioning technology',
        category: 'Sports',
        margin: '42%',
        rating: '4.8★',
        image: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=300'
    },
    'PROD016': {
        name: 'Professional DSLR Camera Kit',
        price: '$899.99',
        description: 'Complete camera kit with lens, tripod, and professional accessories',
        category: 'Electronics',
        margin: '28%',
        rating: '4.4★',
        image: 'https://images.unsplash.com/photo-1502920917128-1aa500764cbd?w=300'
    },
    'PROD015': {
        name: 'Wireless Bluetooth Headphones',
        price: '$199.99',
        description: 'Premium noise-cancelling wireless headphones with 30-hour battery',
        category: 'Electronics',
        margin: '45%',
        rating: '4.7★',
        image: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=300'
    },
    'PROD014': {
        name: 'Smart Fitness Watch',
        price: '$249.99',
        description: 'Advanced fitness tracking with heart rate monitor and GPS',
        category: 'Wearables',
        margin: '38%',
        rating: '4.5★',
        image: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=300'
    },
    'PROD013': {
        name: 'Premium Coffee Maker',
        price: '$159.99',
        description: 'Professional-grade coffee maker with built-in grinder',
        category: 'Home & Kitchen',
        margin: '40%',
        rating: '4.3★',
        image: 'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=300'
    }
};

/**
 * Update product display when selection changes
 */
function updateSelectedProduct() {
    const selectedProductId = elements.productSelector?.value || 'PROD018';
    const product = productCatalog[selectedProductId];
    
    if (product) {
        displayProduct(product);
    }
}

/**
 * Update Product Display (now uses dynamic catalog)
 */
function updateProductDisplay() {
    // Start with the selected product or default to PROD018
    const selectedProductId = elements.productSelector?.value || 'PROD018';
    const product = productCatalog[selectedProductId];
    displayProduct(product);
}

/**
 * Display Product Information
 */
function displayProduct(product) {
    const elements = {
        name: document.getElementById('product-name'),
        price: document.getElementById('product-price'),
        description: document.getElementById('product-description'),
        category: document.getElementById('product-category'),
        margin: document.getElementById('profit-margin'),
        rating: document.getElementById('product-rating'),
        image: document.getElementById('product-img')
    };
    
    if (elements.name) elements.name.textContent = product.name;
    if (elements.price) elements.price.textContent = product.price;
    if (elements.description) elements.description.textContent = product.description;
    if (elements.category) elements.category.textContent = product.category;
    if (elements.margin) elements.margin.textContent = `Margin: ${product.margin}`;
    if (elements.rating) elements.rating.textContent = product.rating;
    if (elements.image) elements.image.src = product.image;
}

/**
 * Show Error State
 */
function showErrorState(message) {
    elements.aiReasoning.innerHTML = `<span style="color: #dc3545;"><i class="fas fa-exclamation-triangle"></i> ${message}</span>`;
    elements.confidenceFill.style.width = '0%';
    elements.confidencePercentage.textContent = '0%';
    elements.marketInsights.innerHTML = '<div class="insight-item"><i class="fas fa-info-circle"></i> Please check your connection and try again</div>';
    elements.aiRecommendations.innerHTML = '<div class="recommendation-item"><i class="fas fa-redo"></i> Click the button above to retry</div>';
}

/**
 * Show Warning State (for fallback scenarios)
 */
function showWarningState(message) {
    elements.aiReasoning.innerHTML = `<span style="color: #ffc107;"><i class="fas fa-exclamation-circle"></i> ${message}</span>`;
    elements.confidenceFill.style.width = '60%';
    elements.confidencePercentage.textContent = '60%';
    elements.marketInsights.innerHTML = '<div class="insight-item"><i class="fas fa-info-circle"></i> Using standard business rules for analysis</div>';
    elements.aiRecommendations.innerHTML = '<div class="recommendation-item"><i class="fas fa-cog"></i> Fallback analysis mode active</div>';
}

/**
 * Global function for HTML onclick handlers
 */
window.toggleMcpChat = toggleMcpChat;
window.sendChatMessage = sendChatMessage;
window.updateSelectedProduct = updateSelectedProduct;

// Export for potential module usage
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        generateIntelligentDiscount,
        toggleMcpChat,
        sendChatMessage,
        updateAnalytics
    };
}
