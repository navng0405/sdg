// Smart Discount Generator - Frontend Application
class SmartDiscountGenerator {
    constructor() {
        this.apiBaseUrl = 'http://localhost:8080/api';
        this.currentUserId = 'user-001';
        this.currentProduct = null;
        this.allProducts = [];
        this.currentProductIndex = 0;
        this.countdownTimer = null;
        this.behaviorHistory = [];
        
        this.init();
    }
    
    async init() {
        this.setupEventListeners();
        await this.loadInitialData();
        this.updateSystemStatus();
        
        // Simulate some initial user behavior
        setTimeout(() => {
            this.addToBrowseHistory('search', `Searched for: "${this.currentProduct?.category || 'products'}"`);
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
    
    async loadInitialData() {
        try {
            this.showLoading('Loading product data...');
            
            // Load products from Algolia API
            await this.loadProductsFromAPI();
            
            // Set initial product (first product or random)
            if (this.allProducts.length > 0) {
                this.currentProductIndex = Math.floor(Math.random() * Math.min(this.allProducts.length, 5));
                this.currentProduct = this.allProducts[this.currentProductIndex];
                this.updateProductDisplay();
            } else {
                // Fallback to static product if API fails
                this.currentProduct = {
                    id: 'PROD001',
                    objectId: 'PROD001',
                    name: 'Wireless Bluetooth Headphones',
                    description: 'Premium noise-cancelling wireless headphones with 30-hour battery life',
                    price: 199.99,
                    category: 'Electronics',
                    imageUrl: 'https://images.unsplash.com/photo-1484704849700-f032a568e944?q=80&w=1470&auto=format&fit=crop&ixlib=rb-4.1.0',
                    rating: 4.5,
                    numberOfReviews: 1250
                };
                this.updateProductDisplay();
            }
            
            // Load user behavior history
            this.loadUserBehaviorHistory();
            
        } catch (error) {
            console.error('Error loading initial data:', error);
            this.showError('Failed to load product data');
        } finally {
            this.hideLoading();
        }
    }
    
    async loadProductsFromAPI() {
        try {
            const response = await fetch(`${this.apiBaseUrl}/products?limit=20`);
            if (response.ok) {
                const data = await response.json();
                console.log('Loaded products from API:', data);
                
                // Convert API products to internal format
                this.allProducts = data.products.map(product => ({
                    id: product.objectId,
                    objectId: product.objectId,
                    name: product.name,
                    description: product.description,
                    price: parseFloat(product.price),
                    category: product.category,
                    imageUrl: product.image_url || `https://via.placeholder.com/400x300/4A90E2/FFFFFF?text=${encodeURIComponent(product.name)}`,
                    rating: product.average_rating || 4.5,
                    numberOfReviews: product.number_of_reviews || 100,
                    profitMargin: product.profit_margin || 0.3
                }));
                
                console.log(`Loaded ${this.allProducts.length} products`);
            } else {
                throw new Error(`API request failed with status: ${response.status}`);
            }
        } catch (error) {
            console.error('Error loading products from API:', error);
            this.allProducts = []; // Will trigger fallback
        }
    }
    
    updateProductDisplay() {
        if (!this.currentProduct) return;
        
        // Update product name
        document.getElementById('product-name').textContent = this.currentProduct.name;
        
        // Update product description
        const descriptionElement = document.getElementById('product-description');
        if (descriptionElement) {
            descriptionElement.textContent = this.currentProduct.description || 'No description available';
        }
        
        // Update product image
        const imageElement = document.getElementById('product-image');
        if (imageElement) {
            imageElement.src = this.currentProduct.image_url;
            imageElement.alt = this.currentProduct.name;
        }
        
        // Update product price
        document.getElementById('product-price').textContent = `$${this.currentProduct.price.toFixed(2)}`;
        
        // Update product rating
        const ratingElement = document.getElementById('product-rating');
        if (ratingElement) {
            ratingElement.textContent = this.currentProduct.rating.toFixed(1);
        }
        
        // Update reviews count
        const reviewsElement = document.getElementById('product-reviews');
        if (reviewsElement) {
            reviewsElement.textContent = `(${this.currentProduct.numberOfReviews.toLocaleString()} reviews)`;
        }
        
        // Update stars display
        this.updateStarsDisplay(this.currentProduct.rating);
        
        // Add product switch functionality
        this.addProductSwitchControls();
        
        console.log('Updated product display for:', this.currentProduct.name);
    }
    
    updateStarsDisplay(rating) {
        const starsContainer = document.querySelector('.stars');
        if (!starsContainer) return;
        
        starsContainer.innerHTML = '';
        const fullStars = Math.floor(rating);
        const hasHalfStar = rating % 1 !== 0;
        
        // Add full stars
        for (let i = 0; i < fullStars; i++) {
            starsContainer.innerHTML += '<i class="fas fa-star"></i>';
        }
        
        // Add half star if needed
        if (hasHalfStar) {
            starsContainer.innerHTML += '<i class="fas fa-star-half-alt"></i>';
        }
        
        // Add empty stars
        const emptyStars = 5 - Math.ceil(rating);
        for (let i = 0; i < emptyStars; i++) {
            starsContainer.innerHTML += '<i class="far fa-star"></i>';
        }
    }
    
    addProductSwitchControls() {
        // Add next/previous product buttons if not already added
        if (this.allProducts.length > 1 && !document.getElementById('product-switch-controls')) {
            const productSection = document.querySelector('.product-section');
            const switchControls = document.createElement('div');
            switchControls.id = 'product-switch-controls';
            switchControls.className = 'product-switch-controls';
            switchControls.innerHTML = `
                <button id="prev-product-btn" class="switch-btn" title="Previous Product">
                    <i class="fas fa-chevron-left"></i>
                </button>
                <span class="product-counter">${this.currentProductIndex + 1} of ${this.allProducts.length}</span>
                <button id="next-product-btn" class="switch-btn" title="Next Product">
                    <i class="fas fa-chevron-right"></i>
                </button>
            `;
            
            productSection.appendChild(switchControls);
            
            // Add event listeners for product switching
            document.getElementById('prev-product-btn').addEventListener('click', () => {
                this.switchToPreviousProduct();
            });
            
            document.getElementById('next-product-btn').addEventListener('click', () => {
                this.switchToNextProduct();
            });
        } else if (document.getElementById('product-switch-controls')) {
            // Update counter if controls already exist
            const counter = document.querySelector('.product-counter');
            if (counter) {
                counter.textContent = `${this.currentProductIndex + 1} of ${this.allProducts.length}`;
            }
        }
    }
    
    switchToNextProduct() {
        if (this.allProducts.length > 1) {
            this.currentProductIndex = (this.currentProductIndex + 1) % this.allProducts.length;
            this.currentProduct = this.allProducts[this.currentProductIndex];
            this.updateProductDisplay();
            this.trackBehavior('product_switch', { 
                productId: this.currentProduct.id,
                direction: 'next'
            });
            this.addToBrowseHistory('view', `Viewed: ${this.currentProduct.name}`);
        }
    }
    
    switchToPreviousProduct() {
        if (this.allProducts.length > 1) {
            this.currentProductIndex = this.currentProductIndex === 0 ? 
                this.allProducts.length - 1 : this.currentProductIndex - 1;
            this.currentProduct = this.allProducts[this.currentProductIndex];
            this.updateProductDisplay();
            this.trackBehavior('product_switch', { 
                productId: this.currentProduct.id,
                direction: 'previous'
            });
            this.addToBrowseHistory('view', `Viewed: ${this.currentProduct.name}`);
        }
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
        if (!this.currentProduct) {
            this.showError('No product selected for discount generation');
            return;
        }
        
        this.showLoading(`Generating personalized discount for ${this.currentProduct.name}...`);
        
        try {
            // Include product-specific information in the discount request
            const requestUrl = `${this.apiBaseUrl}/get-discount?userId=${this.currentUserId}&productId=${this.currentProduct.objectId}`;
            const response = await fetch(requestUrl);
            
            if (response.ok) {
                const data = await response.json();
                if (data.discount) {
                    // Add product context to the discount data
                    data.discount.productId = this.currentProduct.objectId;
                    data.discount.productName = this.currentProduct.name;
                    data.discount.originalPrice = this.currentProduct.price;
                    
                    this.showDiscountBanner(data.discount);
                    this.addToBrowseHistory('discount', 
                        `AI generated ${data.discount.percentage}% discount for ${this.currentProduct.name}`);
                    
                    // Track the discount generation event
                    this.trackBehavior('discount_generated', {
                        productId: this.currentProduct.objectId,
                        productName: this.currentProduct.name,
                        discountPercentage: data.discount.percentage,
                        discountCode: data.discount.code
                    });
                } else {
                    this.showNoDiscountMessage(
                        data.message || `No discount available for ${this.currentProduct.name} at this time`
                    );
                }
            } else {
                this.showNoDiscountMessage('Unable to generate discount at this time');
            }
        } catch (error) {
            console.error('Error generating discount:', error);
            this.showError('Failed to generate discount. Please try again.');
        } finally {
            this.hideLoading();
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
