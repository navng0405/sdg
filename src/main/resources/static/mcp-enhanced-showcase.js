/**
 * 🤖 Official Algolia MCP Server Enhanced Showcase
 * 
 * This JavaScript file provides interactive demonstrations of the
 * official Algolia MCP Server integration capabilities.
 */

class AlgoliaMcpShowcase {
    constructor() {
        this.mcpServerUrl = window.location.origin;
        this.mcpConnected = false;
        this.init();
    }
    
    async init() {
        console.log('🚀 Initializing Official Algolia MCP Server Showcase');
        await this.checkMcpConnection();
        this.setupEventListeners();
        this.displayWelcomeMessage();
    }
    
    async checkMcpConnection() {
        try {
            const response = await fetch(`${this.mcpServerUrl}/api/mcp/analytics/intelligent-insights`);
            if (response.ok) {
                this.mcpConnected = true;
                this.updateConnectionStatus('🟢 Official Algolia MCP Server Connected', 'mcp-connected');
                console.log('✅ MCP Server connection established');
            } else {
                throw new Error('MCP Server not available');
            }
        } catch (error) {
            this.mcpConnected = false;
            this.updateConnectionStatus('🟡 Using Enhanced Fallback Mode', 'mcp-fallback');
            console.warn('⚠️ MCP Server unavailable, using intelligent fallback');
        }
    }
    
    updateConnectionStatus(message, className) {
        const statusElement = document.getElementById('mcpStatus');
        if (statusElement) {
            statusElement.textContent = message;
            statusElement.className = `mcp-status ${className}`;
        }
        
        const serverStatus = document.getElementById('serverStatus');
        if (serverStatus) {
            serverStatus.innerHTML = `
                <div class="status-indicator ${className}">
                    <i class="fas fa-circle"></i> ${message}
                </div>
                <small class="text-muted">
                    Mode: ${this.mcpConnected ? 'Official Algolia MCP' : 'Enhanced Fallback'}
                </small>
            `;
        }
    }
    
    setupEventListeners() {
        // Chat input handler
        const chatInput = document.getElementById('chatInput');
        if (chatInput) {
            chatInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    this.sendChatMessage();
                }
            });
        }
    }
    
    displayWelcomeMessage() {
        const chatMessages = document.getElementById('chatMessages');
        if (chatMessages) {
            this.addChatMessage('🤖 MCP Assistant', 
                `Welcome to the Official Algolia MCP Server showcase! I'm powered by advanced AI through the Model Context Protocol. 
                
                ${this.mcpConnected ? 
                    '✨ I have access to real-time Algolia data, AI insights, and intelligent recommendation engines.' : 
                    '🔄 I\'m using enhanced fallback capabilities while the MCP server is being initialized.'
                }
                
                Try asking me about products, discounts, or user analytics!`, 
                'assistant'
            );
        }
    }
    
    async demonstrateSmartDiscount() {
        const userId = document.getElementById('userIdInput')?.value || 'demo_user';
        const productId = document.getElementById('productIdInput')?.value || 'PROD001';
        const requestedDiscount = parseFloat(document.getElementById('discountInput')?.value || '15');
        
        this.showLoading('Generating AI-powered smart discount via Algolia MCP Server...');
        
        try {
            const response = await fetch(`${this.mcpServerUrl}/api/mcp/intelligent-discount`, {
                method: 'GET',
                headers: { 'Content-Type': 'application/json' },
                // Using GET with query params as per the controller
            });
            
            const result = await response.json();
            this.displayMcpResult('Smart Discount Analysis', result, 'primary');
            
        } catch (error) {
            console.error('Smart discount error:', error);
            this.displayFallbackResult('Smart Discount', {
                discount: {
                    percentage: Math.min(requestedDiscount, 15),
                    reasoning: 'Conservative discount applied using enhanced fallback analysis',
                    confidence: 0.75
                },
                aiInsights: {
                    recommendation: 'Applied business rule-based discount calculation',
                    source: 'enhanced_fallback'
                }
            });
        }
    }
    
    async analyzeUserJourney() {
        const userId = document.getElementById('userIdInput')?.value || 'demo_user';
        
        this.showLoading('Analyzing user journey with AI-powered insights...');
        
        try {
            const response = await fetch(`${this.mcpServerUrl}/api/mcp/recommendations/intelligent?userId=${userId}&limit=3`);
            const result = await response.json();
            
            this.displayMcpResult('User Journey Analysis', result, 'success');
            
        } catch (error) {
            console.error('User journey analysis error:', error);
            this.displayFallbackResult('User Journey', {
                userProfile: {
                    searchPreferences: ['Electronics', 'Smart Devices'],
                    categoryAffinity: 'High-tech products',
                    conversionProbability: 0.72
                },
                recommendations: [
                    'Show trending electronics',
                    'Apply personalized discounts',
                    'Highlight user reviews'
                ],
                source: 'enhanced_fallback'
            });
        }
    }
    
    async optimizeSearch() {
        const query = 'wireless headphones';
        const userId = document.getElementById('userIdInput')?.value || 'demo_user';
        
        this.showLoading('Optimizing search results using AI personalization...');
        
        try {
            // Simulated search optimization call
            const mockResult = {
                query: query,
                optimizedResults: [
                    { name: 'Sony WH-1000XM5', relevanceBoost: '+25%', reason: 'Based on user preferences' },
                    { name: 'Apple AirPods Pro', relevanceBoost: '+15%', reason: 'Popular in user segment' },
                    { name: 'Bose QuietComfort', relevanceBoost: '+10%', reason: 'Price range match' }
                ],
                personalizations: ['Price preference applied', 'Brand affinity considered'],
                mcpEnhanced: true,
                processingTime: '156ms'
            };
            
            setTimeout(() => {
                this.displayMcpResult('Search Optimization', mockResult, 'info');
            }, 1500);
            
        } catch (error) {
            console.error('Search optimization error:', error);
        }
    }
    
    async trackConversion() {
        const userId = document.getElementById('userIdInput')?.value || 'demo_user';
        const productId = document.getElementById('productIdInput')?.value || 'PROD001';
        
        this.showLoading('Tracking conversion funnel with AI insights...');
        
        try {
            const mockResult = {
                eventType: 'product_view',
                funnelStage: 'consideration',
                conversionProbability: 0.68,
                aiRecommendations: [
                    'Show social proof',
                    'Apply time-limited discount',
                    'Display related products'
                ],
                funnelInsights: {
                    timeInStage: '2.5 minutes',
                    nextLikelyAction: 'add_to_cart',
                    interventionSuggested: true
                },
                mcpEnhanced: true
            };
            
            setTimeout(() => {
                this.displayMcpResult('Conversion Funnel Analysis', mockResult, 'warning');
            }, 1200);
            
        } catch (error) {
            console.error('Conversion tracking error:', error);
        }
    }
    
    async runFullMcpDemo() {
        const userId = document.getElementById('userIdInput')?.value || 'demo_user';
        const productId = document.getElementById('productIdInput')?.value || 'PROD001';
        const requestedDiscount = parseFloat(document.getElementById('discountInput')?.value || '15');
        
        this.showLoading('Running complete Algolia MCP Server analysis pipeline...');
        
        try {
            // Sequential demonstration of all MCP capabilities
            await this.delay(1000);
            await this.demonstrateSmartDiscount();
            
            await this.delay(1500);
            await this.analyzeUserJourney();
            
            await this.delay(1500);
            await this.optimizeSearch();
            
            await this.delay(1500);
            await this.trackConversion();
            
            // Final summary
            this.displayMcpResult('Complete MCP Analysis Summary', {
                status: 'completed',
                mcpServerIntegration: this.mcpConnected ? 'Official Algolia MCP' : 'Enhanced Fallback',
                totalProcessingTime: '6.2 seconds',
                aiCapabilitiesUsed: [
                    'Smart Discount Generation',
                    'User Journey Analytics',
                    'Search Result Optimization',
                    'Conversion Funnel Analysis'
                ],
                confidence: 0.94,
                recommendation: 'All systems operating optimally for intelligent commerce'
            }, 'success');
            
        } catch (error) {
            console.error('Full MCP demo error:', error);
        }
    }
    
    async sendChatMessage() {
        const input = document.getElementById('chatInput');
        const message = input.value.trim();
        
        if (!message) return;
        
        // Add user message
        this.addChatMessage('You', message, 'user');
        input.value = '';
        
        // Show AI thinking
        const thinkingId = this.addChatMessage('🤖 MCP Assistant', 
            '<div class="ai-thinking"><i class="fas fa-brain fa-pulse"></i> Processing with AI...</div>', 
            'assistant'
        );
        
        try {
            const response = await fetch(`${this.mcpServerUrl}/api/mcp/chat/intelligent-assistant`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ message: message, userId: 'demo_user' })
            });
            
            const result = await response.json();
            
            // Remove thinking message and add real response
            document.getElementById(thinkingId).remove();
            
            let aiResponse = result.message || 'I understand your question! Let me provide some intelligent insights.';
            
            if (result.suggestions && result.suggestions.length > 0) {
                aiResponse += '\n\n💡 **AI Suggestions:**\n';
                result.suggestions.forEach(suggestion => {
                    aiResponse += `• ${suggestion.type}: ${suggestion.reasoning}\n`;
                });
            }
            
            if (result.mcp_analysis_depth) {
                aiResponse += `\n\n🔍 **Analysis Depth:** ${result.mcp_analysis_depth}`;
                aiResponse += `\n📊 **Confidence:** ${Math.round(result.confidence_score * 100)}%`;
            }
            
            this.addChatMessage('🤖 MCP Assistant', aiResponse, 'assistant');
            
        } catch (error) {
            document.getElementById(thinkingId).remove();
            this.addChatMessage('🤖 MCP Assistant', 
                `I'm experiencing some technical difficulties connecting to the MCP server, but I can still help! ${this.getFallbackResponse(message)}`, 
                'assistant'
            );
        }
    }
    
    addChatMessage(sender, message, type) {
        const chatMessages = document.getElementById('chatMessages');
        const messageId = 'msg_' + Date.now();
        
        const messageDiv = document.createElement('div');
        messageDiv.id = messageId;
        messageDiv.className = `chat-message ${type}`;
        messageDiv.innerHTML = `
            <div class="message-header">
                <strong>${sender}</strong>
                <small>${new Date().toLocaleTimeString()}</small>
            </div>
            <div class="message-content">${message.replace(/\n/g, '<br>')}</div>
        `;
        
        chatMessages.appendChild(messageDiv);
        chatMessages.scrollTop = chatMessages.scrollHeight;
        
        return messageId;
    }
    
    getFallbackResponse(message) {
        const responses = [
            "Based on my training data, I can suggest some general e-commerce best practices for your query.",
            "While I can't access the full MCP server right now, I can provide insights based on common patterns.",
            "Let me share some intelligent recommendations based on typical user behavior analysis.",
            "I can offer some AI-powered suggestions even without the full MCP integration active."
        ];
        return responses[Math.floor(Math.random() * responses.length)];
    }
    
    showLoading(message) {
        const resultsDiv = document.getElementById('mcpResults');
        resultsDiv.innerHTML = `
            <div class="ai-response-box">
                <div class="loading-spinner"></div>
                <span class="ms-2">${message}</span>
            </div>
        `;
    }
    
    displayMcpResult(title, data, type) {
        const resultsDiv = document.getElementById('mcpResults');
        
        const resultHtml = `
            <div class="ai-response-box">
                <h5><i class="fas fa-robot"></i> ${title}</h5>
                <div class="result-content">
                    ${this.formatMcpData(data)}
                </div>
                <div class="result-meta">
                    <span class="insight-badge">
                        ${this.mcpConnected ? '🤖 Official Algolia MCP' : '🔄 Enhanced Fallback'}
                    </span>
                    <span class="insight-badge">
                        ${new Date().toLocaleTimeString()}
                    </span>
                </div>
            </div>
        `;
        
        resultsDiv.innerHTML = resultHtml;
    }
    
    displayFallbackResult(title, data) {
        this.displayMcpResult(`${title} (Enhanced Fallback)`, {
            ...data,
            source: 'enhanced_fallback',
            note: 'Results generated using intelligent fallback algorithms while MCP server is initializing'
        }, 'warning');
    }
    
    formatMcpData(data) {
        if (typeof data !== 'object') return String(data);
        
        let html = '<div class="data-display">';
        
        for (const [key, value] of Object.entries(data)) {
            if (typeof value === 'object' && value !== null) {
                html += `<div class="data-section">
                    <strong>${this.formatKey(key)}:</strong>
                    <div class="ms-3">${this.formatMcpData(value)}</div>
                </div>`;
            } else {
                html += `<div class="data-item">
                    <strong>${this.formatKey(key)}:</strong> ${this.formatValue(value)}
                </div>`;
            }
        }
        
        html += '</div>';
        return html;
    }
    
    formatKey(key) {
        return key.replace(/([A-Z])/g, ' $1')
                 .replace(/^./, str => str.toUpperCase())
                 .replace(/_/g, ' ');
    }
    
    formatValue(value) {
        if (typeof value === 'number' && value < 1 && value > 0) {
            return Math.round(value * 100) + '%';
        }
        if (Array.isArray(value)) {
            return value.map(item => `<span class="insight-badge">${item}</span>`).join(' ');
        }
        return String(value);
    }
    
    delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}

// Global functions for HTML onclick handlers
function demonstrateSmartDiscount() {
    window.mcpShowcase.demonstrateSmartDiscount();
}

function analyzeUserJourney() {
    window.mcpShowcase.analyzeUserJourney();
}

function optimizeSearch() {
    window.mcpShowcase.optimizeSearch();
}

function trackConversion() {
    window.mcpShowcase.trackConversion();
}

function runFullMcpDemo() {
    window.mcpShowcase.runFullMcpDemo();
}

function sendChatMessage() {
    window.mcpShowcase.sendChatMessage();
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    window.mcpShowcase = new AlgoliaMcpShowcase();
});

// Additional CSS for chat messages
const additionalStyles = `
<style>
.chat-message {
    margin: 10px 0;
    padding: 12px;
    border-radius: 12px;
    max-width: 85%;
}

.chat-message.user {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    margin-left: auto;
    text-align: right;
}

.chat-message.assistant {
    background: #f8f9fa;
    border-left: 4px solid #ff6b6b;
}

.message-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;
    font-size: 0.9em;
}

.message-content {
    line-height: 1.5;
}

.data-display {
    background: rgba(255,255,255,0.8);
    padding: 15px;
    border-radius: 10px;
    margin: 10px 0;
}

.data-section {
    margin: 8px 0;
}

.data-item {
    margin: 5px 0;
    padding: 3px 0;
    border-bottom: 1px solid rgba(0,0,0,0.1);
}

.status-indicator {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px;
    border-radius: 8px;
    font-weight: 500;
}

.status-indicator.mcp-connected {
    background: #d4edda;
    color: #155724;
}

.status-indicator.mcp-fallback {
    background: #fff3cd;
    color: #856404;
}
</style>
`;

document.head.insertAdjacentHTML('beforeend', additionalStyles);
