/**
 * MCP-Enhanced Application JavaScript
 * Provides intelligent UI interactions powered by Algolia MCP Server + Claude AI
 */

// Application State
let mcpChatVisible = false;
let countdownTimer = null;
let currentAnalysis = null;

// DOM Elements
const elements = {
    generateButton: document.getElementById('generate-intelligent-discount'),
    userId: document.getElementById('user-id'),
    requestedDiscount: document.getElementById('requested-discount'),
    userIntent: document.getElementById('user-intent'),
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
    chatInput: document.getElementById('chat-input')
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
    
    // Product rotation (demo feature)
    setInterval(rotateProduct, 60000); // Rotate product every minute
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
        
        // Get form data
        const userId = elements.userId.value || 'user-001';
        const requestedDiscount = parseFloat(elements.requestedDiscount.value) || 15.0;
        const userIntent = elements.userIntent.value || 'purchase_consideration';
        const productId = 'PROD018'; // Current product ID
        
        // Call MCP API
        const response = await fetch(`/api/mcp/intelligent-discount?userId=${encodeURIComponent(userId)}&productId=${encodeURIComponent(productId)}&requestedDiscount=${requestedDiscount}&userIntent=${encodeURIComponent(userIntent)}`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
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
        showErrorState('Failed to connect to MCP AI service. Please try again.');
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
    if (Array.isArray(insights)) {
        elements.marketInsights.innerHTML = insights.map(insight => 
            `<div class="insight-item"><i class="fas fa-chart-line"></i> ${insight}</div>`
        ).join('');
    } else {
        elements.marketInsights.innerHTML = `<div class="insight-item"><i class="fas fa-chart-line"></i> ${insights}</div>`;
    }
}

/**
 * Display AI Recommendations
 */
function displayAiRecommendations(recommendations) {
    if (Array.isArray(recommendations)) {
        elements.aiRecommendations.innerHTML = recommendations.map(rec => 
            `<div class="recommendation-item"><i class="fas fa-lightbulb"></i> ${rec}</div>`
        ).join('');
    } else {
        elements.aiRecommendations.innerHTML = `<div class="recommendation-item"><i class="fas fa-lightbulb"></i> ${recommendations}</div>`;
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
        // Simulate AI response (in real implementation, this would call Claude via MCP)
        setTimeout(() => {
            // Remove typing indicator
            const typingMessage = elements.mcpChatMessages.lastElementChild;
            if (typingMessage && typingMessage.classList.contains('typing')) {
                typingMessage.remove();
            }
            
            // Add AI response
            const aiResponse = generateAiResponse(message);
            addChatMessage(aiResponse, 'assistant');
        }, 1500);
        
    } catch (error) {
        console.error('Chat error:', error);
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
 * Update Product Display (demo rotation)
 */
function updateProductDisplay() {
    // This could be connected to real product data
    const products = [
        {
            name: 'Waterproof Hiking Backpack',
            price: '$89.99',
            description: 'Durable and waterproof hiking backpack with multiple compartments',
            category: 'Sports',
            margin: '35%',
            image: 'https://images.unsplash.com/photo-1620953749696-38989c40eadb?w=300'
        },
        {
            name: 'Wireless Bluetooth Headphones',
            price: '$129.99',
            description: 'Premium noise-cancelling wireless headphones with 30-hour battery',
            category: 'Electronics',
            margin: '42%',
            image: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=300'
        },
        {
            name: 'Smart Fitness Watch',
            price: '$199.99',
            description: 'Advanced fitness tracking with heart rate monitor and GPS',
            category: 'Wearables',
            margin: '38%',
            image: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=300'
        }
    ];
    
    // Start with first product
    displayProduct(products[0]);
}

/**
 * Rotate Product Display (demo feature)
 */
function rotateProduct() {
    const products = [
        {
            name: 'Waterproof Hiking Backpack',
            price: '$89.99',
            description: 'Durable and waterproof hiking backpack with multiple compartments',
            category: 'Sports',
            margin: '35%',
            image: 'https://images.unsplash.com/photo-1620953749696-38989c40eadb?w=300'
        },
        {
            name: 'Wireless Bluetooth Headphones',
            price: '$129.99',
            description: 'Premium noise-cancelling wireless headphones with 30-hour battery',
            category: 'Electronics',
            margin: '42%',
            image: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=300'
        },
        {
            name: 'Smart Fitness Watch',
            price: '$199.99',
            description: 'Advanced fitness tracking with heart rate monitor and GPS',
            category: 'Wearables',
            margin: '38%',
            image: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=300'
        }
    ];
    
    const currentTime = Date.now();
    const productIndex = Math.floor(currentTime / 60000) % products.length;
    displayProduct(products[productIndex]);
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
        image: document.getElementById('product-img')
    };
    
    if (elements.name) elements.name.textContent = product.name;
    if (elements.price) elements.price.textContent = product.price;
    if (elements.description) elements.description.textContent = product.description;
    if (elements.category) elements.category.textContent = product.category;
    if (elements.margin) elements.margin.textContent = `Margin: ${product.margin}`;
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
 * Global function for HTML onclick handlers
 */
window.toggleMcpChat = toggleMcpChat;
window.sendChatMessage = sendChatMessage;

// Export for potential module usage
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        generateIntelligentDiscount,
        toggleMcpChat,
        sendChatMessage,
        updateAnalytics
    };
}
