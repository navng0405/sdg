// Smart Discount Generator - Frontend Application
class SmartDiscountGenerator {
    constructor() {
        this.apiBaseUrl = 'http://localhost:8080/api';
        this.currentUserId = 'demo-user-123';
        this.currentProduct = {
            id: 'PROD001',
            name: 'Wireless Bluetooth Headphones',
            price: 199.99
        };
        this.countdownTimer = null;
        this.behaviorHistory = [];
        
        this.init();
    }
    
    init() {
        this.setupEventListeners();
        this.loadInitialData();
        this.updateSystemStatus();
        
        // Simulate some initial user behavior
        setTimeout(() => {
            this.addToBrowseHistory('search', 'Searched for: "wireless headphones"');
        }, 1000);
    }
    
    setupEventListeners() {
        // User ID input
        document.getElementById('user-id').addEventListener('change', (e) => {
            this.currentUserId = e.target.value;
        });
        
        // Product actions
        document.getElementById('add-to-cart-btn').addEventListener('click', () => {
            this.handleAddToCart();
        });
        
        document.getElementById('buy-now-btn').addEventListener('click', () => {
            this.handleBuyNow();
        });
        
        // Behavior simulation buttons
        document.querySelectorAll('.sim-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const eventType = e.currentTarget.dataset.event;
                this.simulateUserBehavior(eventType);
            });
        });
        
        // Get discount button
        document.getElementById('get-discount-btn').addEventListener('click', () => {
            this.generateDiscount();
        });
        
        // Discount banner actions
        document.getElementById('claim-discount-btn').addEventListener('click', () => {
            this.claimDiscount();
        });
        
        document.getElementById('close-discount-btn').addEventListener('click', () => {
            this.hideDiscountBanner();
        });
        
        // Modal actions
        document.getElementById('close-modal-btn').addEventListener('click', () => {
            this.hideModal();
        });
        
        document.getElementById('continue-shopping-btn').addEventListener('click', () => {
            this.hideModal();
        });
        
        // Price hover simulation
        document.getElementById('product-price').addEventListener('mouseenter', () => {
            this.trackBehavior('price_hover', {
                hover_count: 1,
                price: this.currentProduct.price
            });
        });
    }
    
    loadInitialData() {
        // Load product data
        this.updateProductDisplay();
        
        // Load user behavior history
        this.loadUserBehaviorHistory();
    }
    
    updateProductDisplay() {
        document.getElementById('product-name').textContent = this.currentProduct.name;
        document.getElementById('product-price').textContent = `$${this.currentProduct.price}`;
    }
    
    async loadUserBehaviorHistory() {
        try {
            const response = await fetch(`${this.apiBaseUrl}/user-behavior/${this.currentUserId}`);
            if (response.ok) {
                const data = await response.json();
                this.behaviorHistory = data.events || [];
                this.updateBrowseHistory();
            }
        } catch (error) {
            console.error('Error loading behavior history:', error);
        }
    }
    
    updateBrowseHistory() {
        const historyContainer = document.getElementById('browse-history-content');
        historyContainer.innerHTML = '';
        
        // Show recent behavior events
        const recentEvents = this.behaviorHistory.slice(0, 5);
        recentEvents.forEach(event => {
            const historyItem = document.createElement('div');
            historyItem.className = 'history-item';
            
            const icon = this.getEventIcon(event.eventType);
            const description = this.getEventDescription(event);
            const timeAgo = this.getTimeAgo(event.timestamp);
            
            historyItem.innerHTML = `
                <i class="${icon}"></i>
                <span>${description}</span>
                <time>${timeAgo}</time>
            `;
            
            historyContainer.appendChild(historyItem);
        });
        
        // Add default items if no history
        if (recentEvents.length === 0) {
            historyContainer.innerHTML = `
                <div class="history-item">
                    <i class="fas fa-search"></i>
                    <span>Searched for: "wireless headphones"</span>
                    <time>2 minutes ago</time>
                </div>
                <div class="history-item">
                    <i class="fas fa-eye"></i>
                    <span>Viewed: Smart Watch Series X</span>
                    <time>5 minutes ago</time>
                </div>
            `;
        }
    }
    
    getEventIcon(eventType) {
        const icons = {
            'cart_abandon': 'fas fa-shopping-cart',
            'product_view': 'fas fa-eye',
            'search_query': 'fas fa-search',
            'no_results_search': 'fas fa-search-minus',
            'price_hover': 'fas fa-mouse-pointer',
            'multiple_product_views': 'fas fa-eye'
        };
        return icons[eventType] || 'fas fa-circle';
    }
    
    getEventDescription(event) {
        const descriptions = {
            'cart_abandon': 'Abandoned cart',
            'product_view': `Viewed: ${event.productId || 'Product'}`,
            'search_query': `Searched for: "${event.query || 'items'}"`,
            'no_results_search': `No results for: "${event.query || 'search'}"`,
            'price_hover': 'Hesitated on price',
            'multiple_product_views': 'Browsed multiple products'
        };
        return descriptions[event.eventType] || 'User activity';
    }
    
    getTimeAgo(timestamp) {
        const now = new Date();
        const eventTime = new Date(timestamp);
        const diffMinutes = Math.floor((now - eventTime) / (1000 * 60));
        
        if (diffMinutes < 1) return 'Just now';
        if (diffMinutes < 60) return `${diffMinutes} minutes ago`;
        if (diffMinutes < 1440) return `${Math.floor(diffMinutes / 60)} hours ago`;
        return `${Math.floor(diffMinutes / 1440)} days ago`;
    }
    
    async handleAddToCart() {
        this.showLoading('Adding to cart...');
        
        // Simulate add to cart behavior
        await this.trackBehavior('product_view', {
            action: 'add_to_cart',
            product_name: this.currentProduct.name
        });
        
        this.hideLoading();
        this.addToBrowseHistory('cart', `Added ${this.currentProduct.name} to cart`);
        
        // Show success animation
        const button = document.getElementById('add-to-cart-btn');
        button.classList.add('pulse');
        setTimeout(() => button.classList.remove('pulse'), 2000);
        
        // Simulate cart abandonment after 3 seconds
        setTimeout(() => {
            this.simulateUserBehavior('cart_abandon');
        }, 3000);
    }
    
    async handleBuyNow() {
        this.showLoading('Processing purchase...');
        
        await this.trackBehavior('product_view', {
            action: 'buy_now',
            product_name: this.currentProduct.name
        });
        
        this.hideLoading();
        this.addToBrowseHistory('purchase', `Purchased ${this.currentProduct.name}`);
        
        // Show success modal
        this.showSuccessModal();
    }
    
    async simulateUserBehavior(eventType) {
        const button = document.querySelector(`[data-event="${eventType}"]`);
        button.classList.add('pulse');
        
        this.showLoading(`Simulating ${eventType.replace('_', ' ')}...`);
        
        const eventData = this.generateEventData(eventType);
        await this.trackBehavior(eventType, eventData);
        
        this.hideLoading();
        button.classList.remove('pulse');
        
        // Add to browse history
        const description = this.getSimulationDescription(eventType);
        this.addToBrowseHistory(eventType, description);
        
        // Auto-generate discount for certain behaviors
        if (['cart_abandon', 'price_hover', 'multiple_product_views'].includes(eventType)) {
            setTimeout(() => {
                this.generateDiscount();
            }, 2000);
        }
    }
    
    generateEventData(eventType) {
        const eventDataMap = {
            'cart_abandon': {
                abandonment_reason: 'price_concern',
                cart_value: this.currentProduct.price,
                time_in_cart: 45
            },
            'multiple_product_views': {
                products_viewed: ['PROD001', 'PROD002', 'PROD003'],
                view_duration: 30,
                comparison_behavior: true
            },
            'price_hover': {
                hover_count: 3,
                hover_duration: 5,
                price: this.currentProduct.price
            },
            'no_results_search': {
                search_query: 'premium wireless earbuds gold',
                suggested_alternatives: 0
            }
        };
        
        return eventDataMap[eventType] || {};
    }
    
    getSimulationDescription(eventType) {
        const descriptions = {
            'cart_abandon': 'Simulated cart abandonment',
            'multiple_product_views': 'Simulated browsing multiple products',
            'price_hover': 'Simulated price hesitation',
            'no_results_search': 'Simulated search with no results'
        };
        return descriptions[eventType] || 'Simulated user behavior';
    }
    
    async trackBehavior(eventType, details = {}) {
        try {
            const response = await fetch(`${this.apiBaseUrl}/user-behavior`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    userId: this.currentUserId,
                    eventType: eventType,
                    productId: this.currentProduct.id,
                    details: details
                })
            });
            
            if (response.ok) {
                console.log(`Tracked behavior: ${eventType}`);
                this.loadUserBehaviorHistory(); // Refresh history
            } else {
                console.error('Failed to track behavior:', await response.text());
            }
        } catch (error) {
            console.error('Error tracking behavior:', error);
        }
    }
    
    async generateDiscount() {
        this.showLoading('Generating AI-powered discount...');
        
        try {
            const response = await fetch(`${this.apiBaseUrl}/get-discount?userId=${this.currentUserId}`);
            const data = await response.json();
            
            this.hideLoading();
            
            if (data.status === 'offer_generated' && data.discount) {
                this.showDiscountBanner(data.discount);
                this.updateSystemStatus();
            } else {
                this.showNoDiscountMessage(data.message);
            }
        } catch (error) {
            this.hideLoading();
            console.error('Error generating discount:', error);
            this.showError('Failed to generate discount. Please try again.');
        }
    }
    
    showDiscountBanner(discount) {
        const banner = document.getElementById('discount-banner');
        const headline = document.getElementById('discount-headline');
        const message = document.getElementById('discount-message');
        const amount = document.getElementById('discount-amount');
        const code = document.getElementById('discount-code');
        
        headline.textContent = discount.headline || 'Special Offer!';
        message.textContent = discount.message || 'Limited time discount just for you!';
        amount.textContent = discount.amount || '15% OFF';
        code.textContent = `Code: ${discount.code}`;
        
        banner.classList.remove('hidden');
        
        // Start countdown timer
        this.startCountdownTimer(discount.expiresInSeconds || 1800);
        
        // Add to browse history
        this.addToBrowseHistory('discount', `Received ${discount.amount} discount offer`);
    }
    
    startCountdownTimer(seconds) {
        if (this.countdownTimer) {
            clearInterval(this.countdownTimer);
        }
        
        const timerElement = document.getElementById('countdown-timer');
        let remainingSeconds = seconds;
        
        this.countdownTimer = setInterval(() => {
            const minutes = Math.floor(remainingSeconds / 60);
            const secs = remainingSeconds % 60;
            timerElement.textContent = `${minutes}:${secs.toString().padStart(2, '0')}`;
            
            remainingSeconds--;
            
            if (remainingSeconds < 0) {
                clearInterval(this.countdownTimer);
                this.hideDiscountBanner();
                this.showError('Discount offer has expired');
            }
        }, 1000);
    }
    
    hideDiscountBanner() {
        document.getElementById('discount-banner').classList.add('hidden');
        if (this.countdownTimer) {
            clearInterval(this.countdownTimer);
        }
    }
    
    async claimDiscount() {
        const discountCode = document.getElementById('discount-code').textContent.replace('Code: ', '');
        
        this.showLoading('Applying discount...');
        
        try {
            const response = await fetch(`${this.apiBaseUrl}/apply-discount?discountCode=${discountCode}&userId=${this.currentUserId}`, {
                method: 'POST'
            });
            
            const data = await response.json();
            
            this.hideLoading();
            
            if (data.status === 'success') {
                this.hideDiscountBanner();
                this.applyDiscountToPrice();
                this.showSuccessModal();
                this.addToBrowseHistory('discount', 'Applied discount successfully');
            } else {
                this.showError(data.message || 'Failed to apply discount');
            }
        } catch (error) {
            this.hideLoading();
            console.error('Error applying discount:', error);
            this.showError('Failed to apply discount. Please try again.');
        }
    }
    
    applyDiscountToPrice() {
        const priceElement = document.getElementById('product-price');
        const originalPriceElement = document.getElementById('original-price');
        
        const discountAmount = document.getElementById('discount-amount').textContent;
        let discountedPrice = this.currentProduct.price;
        
        if (discountAmount.includes('%')) {
            const percentage = parseInt(discountAmount);
            discountedPrice = this.currentProduct.price * (1 - percentage / 100);
        } else if (discountAmount.includes('$')) {
            const amount = parseFloat(discountAmount.replace('$', '').replace(' off', ''));
            discountedPrice = this.currentProduct.price - amount;
        }
        
        originalPriceElement.textContent = `$${this.currentProduct.price}`;
        originalPriceElement.style.display = 'inline';
        priceElement.textContent = `$${discountedPrice.toFixed(2)}`;
        priceElement.style.color = '#e74c3c';
        
        // Calculate savings
        const savings = this.currentProduct.price - discountedPrice;
        document.getElementById('savings-amount').textContent = `$${savings.toFixed(2)}`;
    }
    
    showNoDiscountMessage(message) {
        const button = document.getElementById('get-discount-btn');
        const originalText = button.innerHTML;
        
        button.innerHTML = '<i class="fas fa-info-circle"></i> No Discount Available';
        button.classList.add('shake');
        
        setTimeout(() => {
            button.innerHTML = originalText;
            button.classList.remove('shake');
        }, 3000);
        
        this.addToBrowseHistory('info', message || 'No discount available at this time');
    }
    
    showSuccessModal() {
        document.getElementById('success-modal').classList.remove('hidden');
    }
    
    hideModal() {
        document.getElementById('success-modal').classList.add('hidden');
    }
    
    showLoading(message = 'Loading...') {
        const overlay = document.getElementById('loading-overlay');
        const text = overlay.querySelector('p');
        text.textContent = message;
        overlay.classList.remove('hidden');
    }
    
    hideLoading() {
        document.getElementById('loading-overlay').classList.add('hidden');
    }
    
    showError(message) {
        // Simple error display - in production, use a proper notification system
        alert(message);
    }
    
    addToBrowseHistory(type, description) {
        const historyContainer = document.getElementById('browse-history-content');
        const historyItem = document.createElement('div');
        historyItem.className = 'history-item';
        
        const icon = type === 'search' ? 'fas fa-search' :
                    type === 'cart' ? 'fas fa-shopping-cart' :
                    type === 'purchase' ? 'fas fa-credit-card' :
                    type === 'discount' ? 'fas fa-gift' :
                    type === 'info' ? 'fas fa-info-circle' :
                    'fas fa-circle';
        
        historyItem.innerHTML = `
            <i class="${icon}"></i>
            <span>${description}</span>
            <time>Just now</time>
        `;
        
        historyContainer.insertBefore(historyItem, historyContainer.firstChild);
        
        // Keep only the latest 10 items
        while (historyContainer.children.length > 10) {
            historyContainer.removeChild(historyContainer.lastChild);
        }
    }
    
    async updateSystemStatus() {
        try {
            // Check active discounts
            const response = await fetch(`${this.apiBaseUrl}/active-discounts`);
            if (response.ok) {
                const data = await response.json();
                document.getElementById('active-discounts-count').textContent = data.count || 0;
            }
            
            // Update status indicators
            document.getElementById('mcp-status').textContent = 'Ready';
            document.getElementById('algolia-status').textContent = 'Connected';
            document.getElementById('ai-status').textContent = 'Active';
        } catch (error) {
            console.error('Error updating system status:', error);
            document.getElementById('mcp-status').textContent = 'Error';
            document.getElementById('algolia-status').textContent = 'Error';
            document.getElementById('ai-status').textContent = 'Error';
        }
    }
}

// Initialize the application when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    new SmartDiscountGenerator();
});
