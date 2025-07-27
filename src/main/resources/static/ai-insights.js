// AI Insights Dashboard Manager
class AIInsightsDashboard {
    constructor() {
        this.apiBaseUrl = '/api';
        this.chatHistory = [];
        this.userId = 'demo-user-' + Math.random().toString(36).substr(2, 9);
        this.init();
    }

    async init() {
        this.setupEventListeners();
        await this.loadInitialInsights();
        this.startRealTimeUpdates();
    }

    setupEventListeners() {
        // Chat functionality
        document.getElementById('chat-send').addEventListener('click', () => this.sendChatMessage());
        document.getElementById('chat-input').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.sendChatMessage();
        });
    }

    async loadInitialInsights() {
        try {
            // Load all AI insights
            await Promise.all([
                this.loadSearchAnalytics(),
                this.loadBehaviorInsights(),
                this.loadProductPerformance(),
                this.loadAIRecommendations()
            ]);
        } catch (error) {
            console.error('Error loading initial insights:', error);
        }
    }

    async loadSearchAnalytics() {
        try {
            const response = await fetch(`${this.apiBaseUrl}/ai-insights?days=7`);
            const data = await response.json();
            
            const container = document.getElementById('search-analytics');
            const analytics = data.searchAnalytics || this.getMockSearchAnalytics();
            
            container.innerHTML = `
                <div class="metric">
                    <span>Total Searches</span>
                    <span class="metric-value">${analytics.totalSearches?.toLocaleString() || '1,247'}</span>
                </div>
                <div class="metric">
                    <span>Unique Queries</span>
                    <span class="metric-value">${analytics.uniqueQueries || '342'}</span>
                </div>
                <div class="metric">
                    <span>Avg Results Clicked</span>
                    <span class="metric-value">${analytics.avgResultsClicked || '2.3'}</span>
                </div>
                <div class="metric">
                    <span>Zero Results Rate</span>
                    <span class="metric-value" style="color: ${(analytics.zeroResultsRate || 0.12) > 0.15 ? 'red' : 'green'}">${((analytics.zeroResultsRate || 0.12) * 100).toFixed(1)}%</span>
                </div>
                <div class="performance-chart">
                    <strong>🔥 Top Search Terms</strong><br>
                    ${(analytics.topQueries || ['wireless headphones', 'running shoes', 'smart watch']).map(query => 
                        `<span style="background: #667eea; color: white; padding: 0.25rem 0.5rem; margin: 0.25rem; border-radius: 4px; display: inline-block;">${query}</span>`
                    ).join('')}
                </div>
            `;
        } catch (error) {
            console.error('Error loading search analytics:', error);
            document.getElementById('search-analytics').innerHTML = '<p style="color: red;">Error loading analytics</p>';
        }
    }

    async loadBehaviorInsights() {
        try {
            const response = await fetch(`${this.apiBaseUrl}/ai-insights?userId=${this.userId}&days=7`);
            const data = await response.json();
            
            const container = document.getElementById('behavior-insights');
            const insights = data.behaviorInsights || this.getMockBehaviorInsights();
            
            container.innerHTML = `
                <div class="metric">
                    <span>Session Duration</span>
                    <span class="metric-value">${insights.avgSessionDuration || '5m 32s'}</span>
                </div>
                <div class="metric">
                    <span>Pages per Session</span>
                    <span class="metric-value">${insights.pagesPerSession || '4.2'}</span>
                </div>
                <div class="metric">
                    <span>Conversion Rate</span>
                    <span class="metric-value" style="color: green">${((insights.conversionRate || 0.078) * 100).toFixed(1)}%</span>
                </div>
                <div class="metric">
                    <span>Cart Abandonment</span>
                    <span class="metric-value" style="color: orange">${((insights.cartAbandonmentRate || 0.23) * 100).toFixed(1)}%</span>
                </div>
                <div style="margin-top: 1rem; padding: 1rem; background: #e3f2fd; border-radius: 8px;">
                    <strong>🎯 AI Insight:</strong><br>
                    ${insights.aiInsight || 'Users spend most time on Electronics category. Consider featuring trending tech products.'}
                </div>
            `;
        } catch (error) {
            console.error('Error loading behavior insights:', error);
            document.getElementById('behavior-insights').innerHTML = '<p style="color: red;">Error loading insights</p>';
        }
    }

    async loadProductPerformance() {
        try {
            const response = await fetch(`${this.apiBaseUrl}/ai-insights?days=7`);
            const data = await response.json();
            
            const container = document.getElementById('product-performance');
            const performance = data.productPerformance || this.getMockProductPerformance();
            
            container.innerHTML = `
                <div class="metric">
                    <span>Total Products Viewed</span>
                    <span class="metric-value">${performance.totalViews?.toLocaleString() || '3,456'}</span>
                </div>
                <div class="metric">
                    <span>Products Added to Cart</span>
                    <span class="metric-value">${performance.addedToCart?.toLocaleString() || '287'}</span>
                </div>
                <div class="metric">
                    <span>Best Performing Category</span>
                    <span class="metric-value">${performance.topCategory || 'Electronics'}</span>
                </div>
                <div class="performance-chart">
                    <strong>⭐ Top Performing Products</strong><br>
                    ${(performance.topProducts || [
                        { name: 'Wireless Headphones', views: 156 },
                        { name: 'Smart Watch', views: 134 },
                        { name: 'Running Shoes', views: 98 }
                    ]).map(product => 
                        `<div style="display: flex; justify-content: space-between; margin: 0.5rem 0;">
                            <span>${product.name}</span>
                            <span style="font-weight: bold; color: #667eea;">${product.views} views</span>
                        </div>`
                    ).join('')}
                </div>
            `;
        } catch (error) {
            console.error('Error loading product performance:', error);
            document.getElementById('product-performance').innerHTML = '<p style="color: red;">Error loading performance data</p>';
        }
    }

    async loadAIRecommendations() {
        try {
            const response = await fetch(`${this.apiBaseUrl}/ai-insights?days=7`);
            const data = await response.json();
            
            const container = document.getElementById('ai-recommendations');
            // Fix: Use data.aiRecommendations.insights since that's the actual array
            const recommendations = data.aiRecommendations?.insights || this.getMockAIRecommendations();
            
            container.innerHTML = recommendations.map(rec => `
                <div class="recommendation-item">
                    <strong>${rec.title}</strong><br>
                    <small>${rec.description}</small>
                    <div style="margin-top: 0.5rem;">
                        <span style="background: rgba(255,255,255,0.2); padding: 0.25rem 0.5rem; border-radius: 4px; font-size: 0.8rem;">
                            Impact: ${rec.impact || rec.priority}
                        </span>
                    </div>
                </div>
            `).join('');
        } catch (error) {
            console.error('Error loading AI recommendations:', error);
            document.getElementById('ai-recommendations').innerHTML = '<p style="color: red;">Error loading recommendations</p>';
        }
    }

    async sendChatMessage() {
        const input = document.getElementById('chat-input');
        const message = input.value.trim();
        
        if (!message) return;
        
        // Add user message to chat
        this.addChatMessage(message, 'user');
        input.value = '';
        
        // Add loading indicator
        const loadingId = this.addChatMessage('<div class="loading"></div> Thinking...', 'ai');
        
        try {
            // Fix: Use the correct MCP chat endpoint
            const response = await fetch(`${this.apiBaseUrl}/mcp/chat/intelligent-assistant`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    message: message,
                    user_id: this.userId // Fix: use user_id not userId
                })
            });
            
            const data = await response.json();
            
            // Remove loading message
            this.removeChatMessage(loadingId);
            
            // Add AI response
            this.addChatMessage(data.message || this.generateMockAIResponse(message), 'ai');
            
            // Show suggested products if any (from MCP suggestions)
            if (data.suggestions && data.suggestions.length > 0) {
                const suggestionsHtml = data.suggestions.map(suggestion => `
                    <div style="border: 1px solid #ddd; border-radius: 8px; padding: 0.5rem; margin: 0.25rem 0; background: white;">
                        <strong>Product ID: ${suggestion.product_id}</strong><br>
                        <small>Discount: ${suggestion.discount} • Confidence: ${suggestion.confidence}</small><br>
                        <small>${suggestion.reasoning}</small>
                    </div>
                `).join('');
                
                this.addChatMessage(`Here are some recommendations:<br>${suggestionsHtml}`, 'ai');
            }
            
        } catch (error) {
            console.error('Error sending chat message:', error);
            this.removeChatMessage(loadingId);
            this.addChatMessage('Sorry, I encountered an error. Please try again.', 'ai');
        }
    }

    addChatMessage(message, sender) {
        const messagesContainer = document.getElementById('chat-messages');
        const messageId = 'msg-' + Date.now();
        
        const messageDiv = document.createElement('div');
        messageDiv.className = `chat-message ${sender}`;
        messageDiv.id = messageId;
        messageDiv.innerHTML = message;
        
        messagesContainer.appendChild(messageDiv);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
        
        // Store in chat history
        this.chatHistory.push({ message, sender, timestamp: Date.now() });
        
        return messageId;
    }

    removeChatMessage(messageId) {
        const messageElement = document.getElementById(messageId);
        if (messageElement) {
            messageElement.remove();
        }
    }

    generateMockAIResponse(message) {
        const responses = [
            `Based on your search for "${message}", I found several relevant products in our Algolia-powered catalog. Our AI analysis shows high engagement with similar queries.`,
            `Great question! Using our advanced search analytics, I can see that products related to "${message}" have been trending recently.`,
            `I've analyzed thousands of customer interactions for queries like "${message}". Here's what our AI recommendations engine suggests:`,
            `Our machine learning models, powered by Algolia's search data, indicate strong user interest in products matching "${message}".`
        ];
        
        return responses[Math.floor(Math.random() * responses.length)];
    }

    startRealTimeUpdates() {
        // Simulate real-time updates every 30 seconds
        setInterval(() => {
            if (Math.random() > 0.7) { // 30% chance to update
                this.loadSearchAnalytics();
            }
        }, 30000);
    }

    // Mock data generators for demo purposes
    getMockSearchAnalytics() {
        return {
            totalSearches: 1247 + Math.floor(Math.random() * 100),
            uniqueQueries: 342 + Math.floor(Math.random() * 20),
            avgResultsClicked: (2.3 + Math.random() * 0.5).toFixed(1),
            zeroResultsRate: 0.12 + Math.random() * 0.05,
            topQueries: ['wireless headphones', 'running shoes', 'smart watch', 'laptop', 'phone case']
        };
    }

    getMockBehaviorInsights() {
        return {
            avgSessionDuration: '5m 32s',
            pagesPerSession: (4.2 + Math.random() * 1).toFixed(1),
            conversionRate: 0.078 + Math.random() * 0.02,
            cartAbandonmentRate: 0.23 + Math.random() * 0.05,
            aiInsight: 'Users spend most time on Electronics category. AI suggests featuring trending tech products during peak hours.'
        };
    }

    getMockProductPerformance() {
        return {
            totalViews: 3456 + Math.floor(Math.random() * 500),
            addedToCart: 287 + Math.floor(Math.random() * 50),
            topCategory: 'Electronics',
            topProducts: [
                { name: 'Wireless Bluetooth Headphones', views: 156 + Math.floor(Math.random() * 20) },
                { name: 'Smart Watch Series X', views: 134 + Math.floor(Math.random() * 15) },
                { name: 'Running Shoes - Men\'s', views: 98 + Math.floor(Math.random() * 25) }
            ]
        };
    }

    getMockAIRecommendations() {
        return [
            {
                title: '🚀 Boost Electronics Sales',
                description: 'AI detected 34% increase in electronics searches. Recommend featuring wireless headphones prominently.',
                impact: 'High'
            },
            {
                title: '🎯 Personalization Opportunity',
                description: 'Users searching for "running" show high interest in fitness. Cross-sell fitness accessories.',
                impact: 'Medium'
            },
            {
                title: '⚡ Real-time Discount Trigger',
                description: 'Cart abandonment rate spike detected. Deploy smart discount campaign for hesitant users.',
                impact: 'High'
            },
            {
                title: '📈 Search Optimization',
                description: 'Queries for "budget phone" returning zero results. Consider adding affordable phone category.',
                impact: 'Medium'
            }
        ];
    }
}

// Initialize the dashboard when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    new AIInsightsDashboard();
});
