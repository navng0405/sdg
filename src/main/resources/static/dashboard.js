// Dashboard Page JavaScript - Smart Discount Generator
class DashboardManager {
    constructor() {
        this.userId = 'user-001';
        this.userName = 'John Doe';
        this.cartItems = JSON.parse(localStorage.getItem('cartItems') || '[]');
        this.currentChart = 'behavior';
        this.charts = {};
        this.preferences = {
            emailNotifications: true,
            pushNotifications: true,
            smsNotifications: false,
            minDiscount: 10,
            notificationFrequency: 2,
            interests: ['electronics', 'clothing', 'sports']
        };
        
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.loadUserData();
        this.loadOffers();
        this.loadTimeline();
        this.loadDiscountHistory();
        this.initializeCharts();
        this.setupPreferences();
        this.updateCartCount();
        this.startRealTimeUpdates();
    }

    setupEventListeners() {
        // Refresh offers
        document.getElementById('refresh-offers-btn')?.addEventListener('click', () => {
            this.loadOffers();
        });

        // Timeline filter
        document.getElementById('timeline-filter')?.addEventListener('change', (e) => {
            this.loadTimeline(e.target.value);
        });

        // Chart controls
        document.querySelectorAll('.chart-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const chartType = e.target.dataset.chart;
                this.switchChart(chartType);
            });
        });

        // History filters
        document.getElementById('filter-history-btn')?.addEventListener('click', () => {
            this.filterHistory();
        });

        document.getElementById('export-history-btn')?.addEventListener('click', () => {
            this.exportHistory();
        });

        // Preferences
        document.getElementById('save-preferences-btn')?.addEventListener('click', () => {
            this.savePreferences();
        });

        // Range sliders
        document.getElementById('min-discount')?.addEventListener('input', (e) => {
            document.getElementById('min-discount-value').textContent = `${e.target.value}%`;
        });

        document.getElementById('notification-frequency')?.addEventListener('input', (e) => {
            const values = ['Low', 'Medium', 'High'];
            document.getElementById('frequency-value').textContent = values[e.target.value - 1];
        });

        // Interest tags
        document.querySelectorAll('.interest-tag').forEach(tag => {
            tag.addEventListener('click', (e) => {
                e.target.classList.toggle('active');
            });
        });

        // AI Assistant
        document.getElementById('toggle-assistant')?.addEventListener('click', () => {
            const panel = document.getElementById('assistant-panel');
            panel?.classList.toggle('hidden');
        });

        document.getElementById('close-assistant')?.addEventListener('click', () => {
            document.getElementById('assistant-panel')?.classList.add('hidden');
        });

        document.getElementById('send-message')?.addEventListener('click', () => {
            this.sendChatMessage();
        });

        document.getElementById('chat-input')?.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.sendChatMessage();
            }
        });
    }

    async loadUserData() {
        try {
            // Simulate API call to get user statistics
            const userData = await this.fetchUserData();
            
            document.getElementById('user-name').textContent = userData.name;
            document.getElementById('total-orders').textContent = userData.totalOrders;
            document.getElementById('total-saved').textContent = `$${userData.totalSaved}`;
            document.getElementById('avg-discount').textContent = `${userData.avgDiscount}%`;
            document.getElementById('ai-confidence').textContent = `${userData.aiConfidence}%`;
        } catch (error) {
            console.error('Error loading user data:', error);
        }
    }

    async fetchUserData() {
        // Simulate API delay
        await new Promise(resolve => setTimeout(resolve, 500));
        
        return {
            name: this.userName,
            totalOrders: 12,
            totalSaved: 248,
            avgDiscount: 18,
            aiConfidence: 92
        };
    }

    async loadOffers() {
        try {
            const offers = await this.fetchPersonalizedOffers();
            this.renderOffers(offers);
        } catch (error) {
            console.error('Error loading offers:', error);
        }
    }

    async fetchPersonalizedOffers() {
        // Simulate AI-powered offer generation
        await new Promise(resolve => setTimeout(resolve, 300));
        
        const offers = [
            {
                id: 1,
                discount: '20%',
                description: 'Perfect for your electronics shopping pattern',
                code: 'TECH20-AI',
                expiry: '2 hours',
                category: 'Electronics',
                confidence: 94
            },
            {
                id: 2,
                discount: '15%',
                description: 'Based on your recent cart abandonment',
                code: 'COMEBACK15',
                expiry: '24 hours',
                category: 'General',
                confidence: 87
            },
            {
                id: 3,
                discount: '25%',
                description: 'Weekend special for loyal customers',
                code: 'WEEKEND25',
                expiry: '3 days',
                category: 'Clothing',
                confidence: 91
            }
        ];

        return offers;
    }

    renderOffers(offers) {
        const container = document.getElementById('offers-container');
        if (!container) return;

        container.innerHTML = offers.map(offer => `
            <div class="offer-card" data-offer-id="${offer.id}">
                <div class="offer-header">
                    <div class="offer-discount">${offer.discount}</div>
                    <div class="offer-expiry">Expires in ${offer.expiry}</div>
                </div>
                <div class="offer-description">${offer.description}</div>
                <div class="offer-code" onclick="dashboardManager.copyOfferCode('${offer.code}')">
                    ${offer.code}
                    <i class="fas fa-copy" style="margin-left: 0.5rem; opacity: 0.7;"></i>
                </div>
            </div>
        `).join('');
    }

    copyOfferCode(code) {
        navigator.clipboard.writeText(code).then(() => {
            this.showNotification(`Code "${code}" copied to clipboard!`, 'success');
        }).catch(() => {
            this.showNotification('Failed to copy code', 'error');
        });
    }

    async loadTimeline(period = 'today') {
        try {
            const timeline = await this.fetchTimeline(period);
            this.renderTimeline(timeline);
        } catch (error) {
            console.error('Error loading timeline:', error);
        }
    }

    async fetchTimeline(period) {
        await new Promise(resolve => setTimeout(resolve, 200));
        
        const timelineData = {
            today: [
                {
                    icon: 'fas fa-eye',
                    title: 'Viewed Wireless Headphones',
                    description: 'Spent 2 minutes analyzing product details',
                    time: '2 hours ago'
                },
                {
                    icon: 'fas fa-shopping-cart',
                    title: 'Added Smart Watch to Cart',
                    description: 'AI detected high purchase intent',
                    time: '4 hours ago'
                },
                {
                    icon: 'fas fa-search',
                    title: 'Searched for "bluetooth speakers"',
                    description: 'Browsed 5 results, no clicks',
                    time: '6 hours ago'
                }
            ],
            week: [
                {
                    icon: 'fas fa-gift',
                    title: 'Applied 15% Discount',
                    description: 'TECH15 code used on electronics purchase',
                    time: '2 days ago'
                },
                {
                    icon: 'fas fa-star',
                    title: 'Left Product Review',
                    description: '5-star review for Yoga Mat Premium',
                    time: '3 days ago'
                },
                {
                    icon: 'fas fa-truck',
                    title: 'Order Delivered',
                    description: 'Coffee Maker Deluxe delivered successfully',
                    time: '5 days ago'
                }
            ],
            month: [
                {
                    icon: 'fas fa-trophy',
                    title: 'Achieved VIP Status',
                    description: 'Unlocked exclusive discounts and early access',
                    time: '1 week ago'
                },
                {
                    icon: 'fas fa-chart-line',
                    title: 'Savings Milestone',
                    description: 'Reached $200 in total savings',
                    time: '2 weeks ago'
                },
                {
                    icon: 'fas fa-heart',
                    title: 'Added to Wishlist',
                    description: '3 items added to wishlist',
                    time: '3 weeks ago'
                }
            ]
        };

        return timelineData[period] || timelineData.today;
    }

    renderTimeline(timeline) {
        const container = document.getElementById('timeline-container');
        if (!container) return;

        container.innerHTML = timeline.map(item => `
            <div class="timeline-item">
                <div class="timeline-icon">
                    <i class="${item.icon}"></i>
                </div>
                <div class="timeline-content">
                    <div class="timeline-title">${item.title}</div>
                    <div class="timeline-description">${item.description}</div>
                    <div class="timeline-time">${item.time}</div>
                </div>
            </div>
        `).join('');
    }

    initializeCharts() {
        const ctx = document.getElementById('behaviorChart');
        if (!ctx) return;

        // Initialize with behavior chart
        this.createBehaviorChart(ctx);
    }

    createBehaviorChart(ctx) {
        if (this.charts.behavior) {
            this.charts.behavior.destroy();
        }

        this.charts.behavior = new Chart(ctx, {
            type: 'line',
            data: {
                labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
                datasets: [{
                    label: 'Product Views',
                    data: [12, 19, 8, 15, 22, 18, 25],
                    borderColor: '#667eea',
                    backgroundColor: 'rgba(102, 126, 234, 0.1)',
                    tension: 0.4,
                    fill: true
                }, {
                    label: 'Cart Additions',
                    data: [3, 5, 2, 4, 7, 6, 8],
                    borderColor: '#764ba2',
                    backgroundColor: 'rgba(118, 75, 162, 0.1)',
                    tension: 0.4,
                    fill: true
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'top',
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });
    }

    createSpendingChart(ctx) {
        if (this.charts.spending) {
            this.charts.spending.destroy();
        }

        this.charts.spending = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
                datasets: [{
                    label: 'Spending ($)',
                    data: [320, 450, 280, 380, 520, 410],
                    backgroundColor: 'rgba(102, 126, 234, 0.8)',
                    borderColor: '#667eea',
                    borderWidth: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'top',
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });
    }

    createDiscountsChart(ctx) {
        if (this.charts.discounts) {
            this.charts.discounts.destroy();
        }

        this.charts.discounts = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: ['Electronics', 'Clothing', 'Home', 'Sports'],
                datasets: [{
                    data: [35, 25, 20, 20],
                    backgroundColor: [
                        '#667eea',
                        '#764ba2',
                        '#5cb85c',
                        '#f0ad4e'
                    ],
                    borderWidth: 2,
                    borderColor: '#fff'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                    }
                }
            }
        });
    }

    switchChart(chartType) {
        // Update button states
        document.querySelectorAll('.chart-btn').forEach(btn => {
            btn.classList.remove('active');
        });
        document.querySelector(`[data-chart="${chartType}"]`)?.classList.add('active');

        const ctx = document.getElementById('behaviorChart');
        if (!ctx) return;

        // Create appropriate chart
        switch (chartType) {
            case 'behavior':
                this.createBehaviorChart(ctx);
                break;
            case 'spending':
                this.createSpendingChart(ctx);
                break;
            case 'discounts':
                this.createDiscountsChart(ctx);
                break;
        }

        this.currentChart = chartType;
    }

    async loadDiscountHistory() {
        try {
            const history = await this.fetchDiscountHistory();
            this.renderDiscountHistory(history);
        } catch (error) {
            console.error('Error loading discount history:', error);
        }
    }

    async fetchDiscountHistory() {
        await new Promise(resolve => setTimeout(resolve, 300));
        
        return [
            {
                code: 'TECH20-AI',
                description: 'AI-generated electronics discount',
                amount: '$45.99',
                date: '2024-01-20',
                status: 'used'
            },
            {
                code: 'COMEBACK15',
                description: 'Cart abandonment recovery',
                amount: '$23.50',
                date: '2024-01-18',
                status: 'used'
            },
            {
                code: 'WEEKEND25',
                description: 'Weekend special offer',
                amount: '$67.25',
                date: '2024-01-15',
                status: 'used'
            },
            {
                code: 'NEWUSER10',
                description: 'New user welcome discount',
                amount: '$15.99',
                date: '2024-01-10',
                status: 'expired'
            }
        ];
    }

    renderDiscountHistory(history) {
        const container = document.getElementById('history-list');
        if (!container) return;

        container.innerHTML = history.map(item => `
            <div class="history-item">
                <div class="history-details">
                    <div class="history-code">${item.code}</div>
                    <div class="history-description">${item.description}</div>
                </div>
                <div class="history-amount">${item.amount}</div>
                <div class="history-date">${new Date(item.date).toLocaleDateString()}</div>
            </div>
        `).join('');
    }

    filterHistory() {
        const startDate = document.getElementById('start-date')?.value;
        const endDate = document.getElementById('end-date')?.value;
        
        if (startDate && endDate) {
            this.showNotification(`Filtering history from ${startDate} to ${endDate}`, 'info');
            // In a real implementation, this would filter the actual data
            this.loadDiscountHistory();
        } else {
            this.showNotification('Please select both start and end dates', 'error');
        }
    }

    exportHistory() {
        // Simulate export functionality
        this.showNotification('Exporting discount history...', 'info');
        
        setTimeout(() => {
            this.showNotification('History exported successfully!', 'success');
        }, 1500);
    }

    setupPreferences() {
        // Load saved preferences
        const savedPrefs = JSON.parse(localStorage.getItem('userPreferences') || '{}');
        this.preferences = { ...this.preferences, ...savedPrefs };

        // Set checkbox states
        document.getElementById('email-notifications').checked = this.preferences.emailNotifications;
        document.getElementById('push-notifications').checked = this.preferences.pushNotifications;
        document.getElementById('sms-notifications').checked = this.preferences.smsNotifications;

        // Set range values
        document.getElementById('min-discount').value = this.preferences.minDiscount;
        document.getElementById('min-discount-value').textContent = `${this.preferences.minDiscount}%`;
        
        document.getElementById('notification-frequency').value = this.preferences.notificationFrequency;
        const frequencies = ['Low', 'Medium', 'High'];
        document.getElementById('frequency-value').textContent = frequencies[this.preferences.notificationFrequency - 1];

        // Set interest tags
        document.querySelectorAll('.interest-tag').forEach(tag => {
            const category = tag.dataset.category;
            if (this.preferences.interests.includes(category)) {
                tag.classList.add('active');
            }
        });
    }

    savePreferences() {
        // Collect current preferences
        this.preferences.emailNotifications = document.getElementById('email-notifications').checked;
        this.preferences.pushNotifications = document.getElementById('push-notifications').checked;
        this.preferences.smsNotifications = document.getElementById('sms-notifications').checked;
        this.preferences.minDiscount = parseInt(document.getElementById('min-discount').value);
        this.preferences.notificationFrequency = parseInt(document.getElementById('notification-frequency').value);

        // Collect interests
        this.preferences.interests = Array.from(document.querySelectorAll('.interest-tag.active'))
            .map(tag => tag.dataset.category);

        // Save to localStorage
        localStorage.setItem('userPreferences', JSON.stringify(this.preferences));

        // Send to server (simulate)
        this.trackUserBehavior('preferences_updated', this.preferences);

        this.showNotification('Preferences saved successfully!', 'success');
    }

    sendChatMessage() {
        const input = document.getElementById('chat-input');
        const message = input?.value.trim();
        
        if (!message) return;

        // Add user message to chat
        this.addChatMessage(message, 'user');
        input.value = '';

        // Simulate AI response
        setTimeout(() => {
            const response = this.generateAIResponse(message);
            this.addChatMessage(response, 'ai');
        }, 1000);
    }

    addChatMessage(message, sender) {
        const messagesContainer = document.getElementById('chat-messages');
        if (!messagesContainer) return;

        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${sender}-message`;
        
        const avatar = sender === 'ai' ? '🤖' : '👤';
        
        messageDiv.innerHTML = `
            <div class="message-avatar">${avatar}</div>
            <div class="message-content">
                <p>${message}</p>
            </div>
        `;

        messagesContainer.appendChild(messageDiv);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    generateAIResponse(userMessage) {
        const responses = {
            'shopping': 'Based on your shopping patterns, you tend to purchase electronics on weekends and respond well to 15-20% discounts. Your conversion rate is highest when discounts are offered after viewing 3+ products.',
            'discount': 'Your average discount rate is 18%, which is 23% higher than typical users. The AI has identified that you respond best to time-limited offers and personalized recommendations.',
            'behavior': 'Your shopping behavior shows strong intent signals - you spend an average of 2.5 minutes per product page and have a 67% cart completion rate when discounts are applied.',
            'recommendations': 'Based on your purchase history, I recommend focusing on electronics and sports categories. You have a 78% likelihood of purchasing complementary items when viewing tech products.',
            'default': 'I can help you understand your shopping patterns, optimize your discount strategy, and provide personalized recommendations. Try asking about your shopping behavior, discount history, or product recommendations!'
        };

        const lowerMessage = userMessage.toLowerCase();
        
        for (const [key, response] of Object.entries(responses)) {
            if (lowerMessage.includes(key)) {
                return response;
            }
        }
        
        return responses.default;
    }

    updateCartCount() {
        const cartCount = document.getElementById('cart-count');
        if (cartCount) {
            const totalItems = this.cartItems.reduce((sum, item) => sum + item.quantity, 0);
            cartCount.textContent = totalItems;
        }
    }

    startRealTimeUpdates() {
        // Simulate real-time updates every 30 seconds
        setInterval(() => {
            this.updateAIConfidence();
            this.checkForNewOffers();
        }, 30000);
    }

    updateAIConfidence() {
        const confidence = document.getElementById('ai-confidence');
        if (confidence) {
            const currentValue = parseInt(confidence.textContent);
            const newValue = Math.min(95, currentValue + Math.floor(Math.random() * 3));
            confidence.textContent = `${newValue}%`;
        }
    }

    async checkForNewOffers() {
        // Simulate checking for new personalized offers
        const hasNewOffer = Math.random() > 0.7; // 30% chance
        
        if (hasNewOffer) {
            this.showNotification('New personalized offer available!', 'success');
            await this.loadOffers();
        }
    }

    async trackUserBehavior(eventType, data = {}) {
        try {
            const behaviorData = {
                userId: this.userId,
                eventType,
                timestamp: new Date().toISOString(),
                metadata: {
                    page: 'dashboard',
                    ...data
                }
            };

            const response = await fetch('/api/user-behavior', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(behaviorData)
            });

            if (!response.ok) {
                throw new Error('Failed to track behavior');
            }
        } catch (error) {
            console.error('Error tracking behavior:', error);
        }
    }

    showNotification(message, type = 'info') {
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.textContent = message;
        
        document.body.appendChild(notification);
        
        setTimeout(() => {
            notification.remove();
        }, 3000);
    }
}

// Initialize dashboard manager when page loads
let dashboardManager;
document.addEventListener('DOMContentLoaded', () => {
    dashboardManager = new DashboardManager();
});

// Add notification styles
const notificationStyles = `
<style>
.notification {
    position: fixed;
    top: 20px;
    right: 20px;
    padding: 1rem 1.5rem;
    border-radius: 10px;
    color: white;
    font-weight: 600;
    z-index: 10000;
    animation: slideIn 0.3s ease;
}

.notification-success {
    background: linear-gradient(135deg, #5cb85c 0%, #4cae4c 100%);
}

.notification-error {
    background: linear-gradient(135deg, #d9534f 0%, #c9302c 100%);
}

.notification-info {
    background: linear-gradient(135deg, #5bc0de 0%, #46b8da 100%);
}

@keyframes slideIn {
    from {
        transform: translateX(100%);
        opacity: 0;
    }
    to {
        transform: translateX(0);
        opacity: 1;
    }
}
</style>
`;

document.head.insertAdjacentHTML('beforeend', notificationStyles);
