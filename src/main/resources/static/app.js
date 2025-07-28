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
        this.autoDiscountEnabled = false; // Set to true to enable auto-discounts on simulate buttons
        
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
        
        // Check for profit protection alerts periodically
        this.checkProfitProtectionAlerts();
        setInterval(() => {
            this.checkProfitProtectionAlerts();
        }, 60000); // Check every minute
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
                console.log('Initial product loaded from API:', this.currentProduct);
                this.updateProductDisplay();
            } else {
                // No products loaded - show error, do NOT use fallback
                this.currentProduct = null;
                this.showError('Failed to load products from API. Please check backend/Algolia connection.');
            }

            // Load user behavior history
            await this.loadUserBehaviorHistory();

        } catch (error) {
            console.error('Error loading initial data:', error);
            this.showError('Failed to load product data from API.');
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
                    id: product.objectID,
                    objectId: product.objectID,
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
                console.log('First product structure:', this.allProducts[0]);
            } else {
                throw new Error(`API request failed with status: ${response.status}`);
            }
        } catch (error) {
            console.error('Error loading products from API:', error);
            this.allProducts = []; // No fallback
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
            imageElement.src = this.currentProduct.imageUrl;
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
            console.log('Switched to next product:', this.currentProduct);
            this.updateProductDisplay();
            this.trackBehavior('product_switch', { 
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
            console.log('Switched to previous product:', this.currentProduct);
            this.updateProductDisplay();
            this.trackBehavior('product_switch', { 
                direction: 'previous'
            });
            this.addToBrowseHistory('view', `Viewed: ${this.currentProduct.name}`);
        }
    }
    
    async loadUserBehaviorHistory() {
        try {
            console.log('Loading user behavior history for:', this.currentUserId);
            const response = await fetch(`${this.apiBaseUrl}/user-behavior/${this.currentUserId}`);
            if (response.ok) {
                const data = await response.json();
                console.log('Loaded behavior history:', data.events?.length || 0, 'events');
                this.behaviorHistory = data.events || [];
                this.updateBrowseHistory();
            } else {
                console.error('Failed to load behavior history:', response.status);
            }
        } catch (error) {
            console.error('Error loading behavior history:', error);
        }
    }
    
    updateBrowseHistory() {
        const historyContainer = document.getElementById('browse-history-content');
        console.log('Updating browse history with', this.behaviorHistory?.length || 0, 'events');

        // If we have behavior history from API, use it
        if (this.behaviorHistory && this.behaviorHistory.length > 0) {
            historyContainer.innerHTML = '';

            // Show recent behavior events
            const recentEvents = this.behaviorHistory.slice(0, 10);
            console.log('Displaying', recentEvents.length, 'recent events');
            recentEvents.forEach((event, index) => {
                const historyItem = document.createElement('div');
                historyItem.className = 'history-item';

                const icon = this.getEventIcon(event.eventType);
                const description = this.getEventDescription(event);
                const timeAgo = this.getTimeAgo(event.timestamp);

                console.log(`Event ${index + 1}:`, event.eventType, '-', description);

                historyItem.innerHTML = `
                    <i class="${icon}"></i>
                    <div class="history-content">
                        <span>${description}</span>
                        <time>${timeAgo}</time>
                    </div>
                `;

                historyContainer.appendChild(historyItem);
            });
        } else {
            // No history - show error, do NOT use fallback
            historyContainer.innerHTML = '<div class="history-item"><i class="fas fa-exclamation-triangle"></i><div class="history-content"><span>No user behavior history found. Please check backend/API.</span><time>Just now</time></div></div>';
        }
    }
    
    getEventIcon(eventType) {
        const icons = {
            'cart_abandon': 'fas fa-shopping-cart',
            'product_view': 'fas fa-eye',
            'search_query': 'fas fa-search',
            'no_results_search': 'fas fa-search-minus',
            'price_hover': 'fas fa-mouse-pointer',
            'multiple_product_views': 'fas fa-eye',
            'discount_generated': 'fas fa-gift',
            'product_switch': 'fas fa-exchange-alt'
        };
        return icons[eventType] || 'fas fa-circle';
    }
    
    getEventDescription(event) {
        switch (event.eventType) {
            case 'cart_abandon':
                return `Abandoned cart (${event.details?.cart_value ? '$' + event.details.cart_value : 'unknown value'})`;
            case 'product_view':
                if (event.details?.product_name) {
                    return `Viewed: ${event.details.product_name}`;
                } else if (event.productId) {
                    return `Viewed product: ${event.productId}`;
                }
                return 'Viewed a product';
            case 'search_query':
                return `Searched for: "${event.query || 'items'}"`;
            case 'no_results_search':
                return `No results for: "${event.query || 'search'}"`;
            case 'price_hover':
                const hoverCount = event.details?.hover_count || 1;
                const price = event.details?.price ? '$' + event.details.price : '';
                return `Hesitated on price ${price} (${hoverCount}x)`;
            case 'multiple_product_views':
                const viewCount = event.details?.products_viewed?.length || event.details?.views || 'multiple';
                return `Browsed ${viewCount} products`;
            case 'discount_generated':
                const productName = event.details?.productName || event.productId || 'product';
                const discountCode = event.details?.discountCode || 'discount';
                return `Generated discount ${discountCode} for ${productName}`;
            case 'product_switch':
                return `Switched to next product`;
            default:
                return `${event.eventType.replace('_', ' ')} activity`;
        }
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
        
        // Add to browse history immediately for instant feedback
        const description = this.getSimulationDescription(eventType);
        this.addToBrowseHistory(eventType, description);
        
        const eventData = this.generateEventData(eventType);
        await this.trackBehavior(eventType, eventData);
        
        this.hideLoading();
        button.classList.remove('pulse');
        
        // Optional: Auto-generate discount for certain behaviors
        // Only if user hasn't disabled auto-discounts
        if (['cart_abandon', 'price_hover', 'multiple_product_views'].includes(eventType) && this.autoDiscountEnabled !== false) {
            console.log(`Auto-discount triggered for ${eventType} - disable by setting autoDiscountEnabled = false`);
            setTimeout(() => {
                this.generateDiscount();
            }, 2000);
        } else {
            console.log(`Simulated behavior: ${eventType} - no auto-discount (use Generate Smart Discount button)`);
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
            // Use the same fallback logic as in generateDiscount
            const productId = this.currentProduct ? (this.currentProduct.objectId || this.currentProduct.id) : null;
            console.log('Tracking behavior with productId:', productId, 'for product:', this.currentProduct);
            
            const response = await fetch(`${this.apiBaseUrl}/user-behavior`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    userId: this.currentUserId,
                    eventType: eventType,
                    productId: productId,
                    details: details
                })
            });
            
            if (response.ok) {
                console.log(`Tracked behavior: ${eventType}`);
                // Wait a moment for the backend to process before refreshing
                setTimeout(() => {
                    this.loadUserBehaviorHistory();
                }, 1000);
            } else {
                console.error('Failed to track behavior:', await response.text());
            }
        } catch (error) {
            console.error('Error tracking behavior:', error);
        }
    }
    
    async generateDiscount() {
        return this.generateDiscountEnhanced();
    }
    
    showDiscountBanner(discount) {
        const banner = document.getElementById('discount-banner');
        const headline = document.getElementById('discount-headline');
        const message = document.getElementById('discount-message');
        const reasoning = document.getElementById('discount-reasoning');
        const amount = document.getElementById('discount-amount');
        const code = document.getElementById('discount-code');
        
        headline.textContent = discount.headline || 'Special Offer!';
        message.textContent = discount.message || 'Limited time discount just for you!';
        reasoning.textContent = discount.reasoning || 'Personalized offer based on your activity';
        amount.textContent = discount.amount || '15% OFF';
        code.textContent = `Code: ${discount.code}`;
        
        // Add profit protection styling if applicable
        if (discount.profitProtected) {
            banner.classList.add('profit-protected');
            
            // Add profit protection badge
            const profitBadge = document.createElement('div');
            profitBadge.className = 'profit-badge';
            profitBadge.textContent = 'Sustainably Priced';
            reasoning.appendChild(profitBadge);
        } else {
            banner.classList.remove('profit-protected');
        }
        
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
                    type === 'cart_abandon' ? 'fas fa-shopping-cart' :
                    type === 'multiple_product_views' ? 'fas fa-eye' :
                    type === 'price_hover' ? 'fas fa-mouse-pointer' :
                    type === 'no_results_search' ? 'fas fa-search-minus' :
                    'fas fa-circle';
        
        historyItem.innerHTML = `
            <i class="${icon}"></i>
            <div class="history-content">
                <span>${description}</span>
                <time>Just now</time>
            </div>
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
    
    /**
     * Checks for recent profit protection veto decisions and shows alerts
     */
    async checkProfitProtectionAlerts() {
        try {
            const response = await fetch(`${this.apiBaseUrl}/profit-protection/veto-decisions?limit=5`);
            if (response.ok) {
                const data = await response.json();
                if (data.vetoDecisions && data.vetoDecisions.length > 0) {
                    this.showProfitProtectionAlerts(data.vetoDecisions);
                }
            }
        } catch (error) {
            console.error('Error checking profit protection alerts:', error);
        }
    }
    
    /**
     * Shows profit protection alerts in the UI
     */
    showProfitProtectionAlerts(vetoDecisions) {
        const recentVetos = vetoDecisions.filter(veto => {
            const vetoTime = new Date(veto.timestamp);
            const now = new Date();
            const diffMinutes = (now - vetoTime) / (1000 * 60);
            return diffMinutes <= 30; // Show alerts for vetos in last 30 minutes
        });
        
        if (recentVetos.length === 0) return;
        
        recentVetos.forEach((veto, index) => {
            setTimeout(() => {
                this.showProfitProtectionAlert(veto);
            }, index * 2000); // Stagger alerts by 2 seconds
        });
    }
    
    /**
     * Shows a single profit protection alert
     */
    showProfitProtectionAlert(vetoDecision) {
        const alertHtml = `
            <div class="profit-protection-alert alert-popup" id="profit-alert-${Date.now()}">
                <div class="alert-header">
                    <i class="fas fa-shield-alt"></i>
                    <span class="alert-title">Profit Protection Active</span>
                    <button class="alert-close" onclick="this.parentElement.parentElement.remove()">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <div class="alert-content">
                    <p><strong>Discount Blocked:</strong> ${vetoDecision.requestedDiscount?.toFixed(1) || 'N/A'}% discount for ${vetoDecision.productId}</p>
                    <p><strong>Max Allowed:</strong> ${vetoDecision.maxAllowedDiscount?.toFixed(1) || 'N/A'}% (80% of ${vetoDecision.profitMargin?.toFixed(1) || 'N/A'}% profit margin)</p>
                    <p><strong>Time:</strong> ${this.getTimeAgo(vetoDecision.timestamp)}</p>
                </div>
            </div>
        `;
        
        // Add to top of page
        const alertContainer = this.getOrCreateAlertContainer();
        alertContainer.insertAdjacentHTML('afterbegin', alertHtml);
        
        // Auto-remove after 10 seconds
        setTimeout(() => {
            const alertElement = document.getElementById(`profit-alert-${Date.now()}`);
            if (alertElement) {
                alertElement.style.animation = 'slideOutUp 0.3s ease-in-out';
                setTimeout(() => alertElement.remove(), 300);
            }
        }, 10000);
    }
    
    /**
     * Gets or creates the alert container
     */
    getOrCreateAlertContainer() {
        let alertContainer = document.getElementById('profit-protection-alerts');
        if (!alertContainer) {
            alertContainer = document.createElement('div');
            alertContainer.id = 'profit-protection-alerts';
            alertContainer.className = 'profit-protection-alerts-container';
            document.body.insertBefore(alertContainer, document.body.firstChild);
        }
        return alertContainer;
    }
    
    /**
     * 🚀 Enhanced generateDiscount with MCP-powered AI insights and profit protection
     */
    async generateDiscountEnhanced() {
        if (!this.currentProduct) {
            this.showError('No product selected for discount generation');
            return;
        }
        
        const productId = this.currentProduct.objectId || this.currentProduct.id;
        console.log('🎯 Using productId for AI-enhanced discount:', productId);
        
        this.showLoading(`🧠 Generating AI-powered personalized discount for ${this.currentProduct.name}...`);
        
        try {
            // First try MCP-enhanced endpoint
            const mcpRequestUrl = `${this.apiBaseUrl}/mcp/intelligent-discount?userId=${this.currentUserId}&productId=${productId}`;
            console.log('🚀 MCP-enhanced discount request URL:', mcpRequestUrl);
            
            const mcpResponse = await fetch(mcpRequestUrl);
            
            if (mcpResponse.ok) {
                const mcpData = await mcpResponse.json();
                if (mcpData.success && mcpData.discount) {
                    console.log('✅ MCP-enhanced discount generated successfully');
                    this.displayAiEnhancedDiscount(mcpData.discount, mcpData.aiInsights);
                    return;
                }
            }
            
            // Fallback to standard endpoint if MCP fails
            console.log('⚠️ MCP endpoint unavailable, falling back to standard discount generation');
            const fallbackUrl = `${this.apiBaseUrl}/get-discount?userId=${this.currentUserId}&productId=${productId}`;
            const response = await fetch(fallbackUrl);
            
            if (response.ok) {
                const data = await response.json();
                if (data.discount) {
                    this.displayStandardDiscount(data.discount);
                } else {
                    this.showNoDiscountMessage();
                }
            } else {
                throw new Error(`Failed to generate discount: ${response.status}`);
            }
            
        } catch (error) {
            console.error('❌ Error generating discount:', error);
            this.showError(`Failed to generate discount: ${error.message}`);
        } finally {
            this.hideLoading();
        }
    }
    
    /**
     * ✨ Display AI-enhanced discount with insights
     */
    displayAiEnhancedDiscount(discount, aiInsights) {
        const productId = this.currentProduct.objectId || this.currentProduct.id;
        
        // Check if discount was profit protected
        if (discount.profitProtected) {
            this.showProfitProtectionNotification(discount);
        }
        
        // Add product context to the discount data
        discount.productId = productId;
        discount.productName = this.currentProduct.name;
        discount.originalPrice = this.currentProduct.price;
        
        // Display the enhanced discount banner
        this.showAiEnhancedDiscountBanner(discount, aiInsights);
        
        // Add to browse history with AI context
        const aiContext = aiInsights ? ` (AI Confidence: ${(aiInsights.confidence_score * 100).toFixed(0)}%)` : '';
        this.addToBrowseHistory('ai_discount', 
            `🧠 AI generated ${discount.percentage || discount.amount} discount for ${this.currentProduct.name}${aiContext}`);
        
        // Track the AI-enhanced discount generation event
        this.trackBehavior('ai_discount_generated', {
            productId: productId,
            productName: this.currentProduct.name,
            discountPercentage: discount.percentage,
            aiConfidenceScore: discount.aiConfidenceScore,
            profitProtected: discount.profitProtected,
            urgent: discount.urgent
        });
    }
    
    /**
     * 📊 Display standard discount (fallback)
     */
    displayStandardDiscount(discount) {
        const productId = this.currentProduct.objectId || this.currentProduct.id;
        
        // Check if discount was profit protected
        if (discount.profitProtected) {
            this.showProfitProtectionNotification(discount);
        }
        
        // Add product context to the discount data
        discount.productId = productId;
        discount.productName = this.currentProduct.name;
        discount.originalPrice = this.currentProduct.price;
        
        this.showDiscountBanner(discount);
        this.addToBrowseHistory('discount', 
            `AI generated ${discount.percentage || discount.amount} discount for ${this.currentProduct.name}`);
        
        // Track the discount generation event
        this.trackBehavior('discount_generated', {
            productId: productId,
            productName: this.currentProduct.name,
            discountPercentage: discount.percentage,
            discountCode: discount.code,
            profitProtected: discount.profitProtected || false
        });
    }
    
    /**
     * 🎨 Show AI-enhanced discount banner with insights
     */
    showAiEnhancedDiscountBanner(discount, aiInsights) {
        const discountContainer = document.getElementById('discount-container');
        if (!discountContainer) return;
        
        // Calculate savings
        const originalPrice = this.currentProduct.price || 0;
        const discountValue = discount.percentage ? 
            (originalPrice * discount.percentage / 100) : 
            (discount.value || 0);
        const finalPrice = originalPrice - discountValue;
        
        // Build AI insights display
        let aiInsightsHtml = '';
        if (aiInsights) {
            aiInsightsHtml = `
                <div class="ai-insights">
                    <div class="ai-badge">🧠 AI-Powered Insights</div>
                    ${aiInsights.confidence_score ? `<div class="confidence">Confidence: ${(aiInsights.confidence_score * 100).toFixed(0)}%</div>` : ''}
                    ${aiInsights.market_position ? `<div class="insight">Market Position: ${aiInsights.market_position}</div>` : ''}
                    ${aiInsights.demand_prediction ? `<div class="insight">Demand: ${aiInsights.demand_prediction}</div>` : ''}
                </div>
            `;
        }
        
        // Build urgency indicator
        const urgencyHtml = discount.urgent ? 
            '<div class="urgency-indicator">⚡ Limited Time Offer!</div>' : '';
        
        // Build cross-sell suggestions
        let crossSellHtml = '';
        if (discount.crossSellSuggestions && discount.crossSellSuggestions.length > 0) {
            crossSellHtml = `
                <div class="cross-sell">
                    <strong>💡 Perfect Pairs:</strong> ${discount.crossSellSuggestions.join(', ')}
                </div>
            `;
        }
        
        discountContainer.innerHTML = `
            <div class="discount-banner ai-enhanced ${discount.urgent ? 'urgent' : ''}">
                ${urgencyHtml}
                <div class="discount-header">
                    <span class="discount-title">${discount.headline || 'Special Discount'}</span>
                    <span class="discount-amount">${discount.amount}</span>
                </div>
                <div class="discount-details">
                    <div class="price-breakdown">
                        <span class="original-price">Was: $${originalPrice.toFixed(2)}</span>
                        <span class="savings">Save: $${discountValue.toFixed(2)}</span>
                        <span class="final-price">Now: $${finalPrice.toFixed(2)}</span>
                    </div>
                    ${discount.description ? `<p class="discount-description">${discount.description}</p>` : ''}
                    ${discount.reasoning ? `<p class="discount-reasoning">${discount.reasoning}</p>` : ''}
                    ${crossSellHtml}
                </div>
                ${aiInsightsHtml}
                <div class="discount-actions">
                    <button class="apply-discount-btn" onclick="smartDiscountGenerator.applyDiscount('${discount.code}')">
                        Apply Discount
                    </button>
                    <div class="discount-code">Code: <strong>${discount.code}</strong></div>
                </div>
                ${discount.profitProtected ? '<div class="profit-protected-badge">🛡️ Profit Protected</div>' : ''}
            </div>
        `;
        
        discountContainer.style.display = 'block';
        
        // Start countdown if expires
        if (discount.expiresInSeconds) {
            this.startDiscountCountdown(discount.expiresInSeconds);
        }
    }
    
    /**
     * 🎯 Show no discount message with AI context
     */
    showNoDiscountMessage(message = null) {
        const discountContainer = document.getElementById('discount-container');
        if (!discountContainer) return;
        
        const defaultMessage = `No personalized discount available for ${this.currentProduct?.name || 'this product'} at this time`;
        
        discountContainer.innerHTML = `
            <div class="no-discount-message">
                <div class="no-discount-icon">🎯</div>
                <p>${message || defaultMessage}</p>
                <p class="suggestion">Try browsing similar products or check back later for new offers!</p>
            </div>
        `;
        
        discountContainer.style.display = 'block';
        
        // Auto-hide after a few seconds
        setTimeout(() => {
            if (discountContainer.style.display === 'block') {
                discountContainer.style.display = 'none';
            }
        }, 5000);
    }
    
    /**
     * Shows a notification when a discount has been adjusted by profit protection
     */
    showProfitProtectionNotification(discount) {
        const notification = `
            <div class="profit-protection-notification notification-popup" id="protection-notification-${Date.now()}">
                <div class="notification-header">
                    <i class="fas fa-shield-alt"></i>
                    <span>Discount Optimized</span>
                    <button class="notification-close" onclick="this.parentElement.parentElement.remove()">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <div class="notification-content">
                    <p>Your discount has been optimized for sustainable pricing!</p>
                    ${discount.originalRequestedDiscount ? 
                        `<p><small>Adjusted from ${discount.originalRequestedDiscount.toFixed(1)}% to ${discount.percentage?.toFixed(1) || 'optimized'}% to maintain product quality.</small></p>` : ''}
                </div>
            </div>
        `;
        
        const alertContainer = this.getOrCreateAlertContainer();
        alertContainer.insertAdjacentHTML('afterbegin', notification);
        
        // Auto-remove after 8 seconds
        setTimeout(() => {
            const notificationElement = document.getElementById(`protection-notification-${Date.now()}`);
            if (notificationElement) {
                notificationElement.style.animation = 'slideOutUp 0.3s ease-in-out';
                setTimeout(() => notificationElement.remove(), 300);
            }
        }, 8000);
    }
}

// Initialize the application when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    new SmartDiscountGenerator();
});
