// Cart Page JavaScript - Smart Discount Generator
class CartManager {
    constructor() {
        this.cartItems = JSON.parse(localStorage.getItem('cartItems') || '[]');
        this.appliedDiscount = JSON.parse(localStorage.getItem('appliedDiscount') || 'null');
        this.userId = 'user-001';
        this.startTime = Date.now();
        this.itemsAdded = 0;
        this.itemsRemoved = 0;
        this.abandonmentRisk = 'Low';
        this.abandonmentTimer = null;
        this.modalCountdown = null;
        
        // Real product data cache - will be populated from Algolia
        this.products = {};
        this.productsLoaded = false;
        
        this.init();
    }

    async init() {
        this.setupEventListeners();
        
        // Load real product data first
        await this.loadProductData();
        
        this.renderCartItems();
        this.updateCartSummary();
        this.loadRecommendations();
        this.startBehaviorTracking();
        this.setupAbandonmentDetection();
        
        // Add some demo items if cart is empty (using real product IDs)
        if (this.cartItems.length === 0) {
            this.addDemoItems();
        }
    }

    async addDemoItems() {
        // Add demo items for better UX using real product IDs from Algolia
        const productIds = Object.keys(this.products).slice(0, 2);
        if (productIds.length >= 2) {
            this.cartItems = [
                { id: productIds[0], quantity: 1 },
                { id: productIds[1], quantity: 1 }
            ];
            localStorage.setItem('cartItems', JSON.stringify(this.cartItems));
            this.renderCartItems();
            this.updateCartSummary();
        }
    }

    setupEventListeners() {
        // Clear cart
        document.getElementById('clear-cart-btn')?.addEventListener('click', () => {
            this.clearCart();
        });

        // Apply discount
        document.getElementById('apply-discount-btn')?.addEventListener('click', () => {
            this.applyDiscount();
        });

        // Checkout
        document.getElementById('checkout-btn')?.addEventListener('click', () => {
            this.proceedToCheckout();
        });

        // Behavior panel toggle
        document.getElementById('toggle-behavior-panel')?.addEventListener('click', () => {
            const content = document.getElementById('behavior-content');
            content?.classList.toggle('hidden');
        });

        // User ID input
        document.getElementById('user-id')?.addEventListener('change', (e) => {
            this.userId = e.target.value;
        });

        // Abandonment banner close
        document.getElementById('close-abandonment-btn')?.addEventListener('click', () => {
            document.getElementById('abandonment-banner')?.classList.add('hidden');
        });

        // Recovery button
        document.getElementById('apply-recovery-btn')?.addEventListener('click', () => {
            this.applyRecoveryDiscount();
        });

        // Modal controls
        document.getElementById('close-modal-btn')?.addEventListener('click', () => {
            this.closeAbandonmentModal();
        });

        document.getElementById('accept-offer-btn')?.addEventListener('click', () => {
            this.acceptAbandonmentOffer();
        });

        document.getElementById('decline-offer-btn')?.addEventListener('click', () => {
            this.declineAbandonmentOffer();
        });

        // Discount code input enter key
        document.getElementById('discount-code-input')?.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.applyDiscount();
            }
        });

        document.getElementById('chat-input')?.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.sendChatMessage();
            }
        });
    }

    async loadProductData() {
        if (this.productsLoaded) return;
        
        try {
            console.log('Loading product data from Algolia...');
            
            // First, try to get products that are currently in the cart
            const cartProductIds = this.cartItems.map(item => item.id);
            
            if (cartProductIds.length > 0) {
                // Load cart products first using batch endpoint
                await this.loadProductsBatch(cartProductIds);
            }
            
            // Then load all available products for recommendations
            const response = await fetch('/api/products?limit=50');
            if (response.ok) {
                const data = await response.json();
                console.log('Loaded products from API:', data);
                
                // Convert products array to object with objectID as key
                data.products.forEach(product => {
                    this.products[product.objectID] = {
                        id: product.objectID,
                        name: product.name,
                        description: product.description,
                        price: parseFloat(product.price),
                        image: product.image_url || `https://via.placeholder.com/300x200/4A90E2/FFFFFF?text=${encodeURIComponent(product.name)}`,
                        category: product.category,
                        rating: product.average_rating || 4.5,
                        reviews: product.number_of_reviews || 100,
                        profitMargin: product.profit_margin || 0.3
                    };
                });
                
                this.productsLoaded = true;
                console.log('Products loaded successfully:', Object.keys(this.products).length, 'products');
            } else {
                console.error('Failed to load products:', response.status);
                this.loadFallbackProducts();
            }
        } catch (error) {
            console.error('Error loading product data:', error);
            this.loadFallbackProducts();
        }
    }

    async loadProductsBatch(productIds) {
        try {
            const response = await fetch('/api/products/batch', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ productIds })
            });
            
            if (response.ok) {
                const data = await response.json();
                console.log('Loaded batch products:', data);
                
                // Add batch products to cache
                Object.entries(data.products).forEach(([productId, product]) => {
                    this.products[productId] = {
                        id: product.objectID,
                        name: product.name,
                        description: product.description,
                        price: parseFloat(product.price),
                        image: product.image_url || `https://via.placeholder.com/300x200/4A90E2/FFFFFF?text=${encodeURIComponent(product.name)}`,
                        category: product.category,
                        rating: product.average_rating || 4.5,
                        reviews: product.number_of_reviews || 100,
                        profitMargin: product.profit_margin || 0.3
                    };
                });
            }
        } catch (error) {
            console.error('Error loading batch products:', error);
        }
    }

    loadFallbackProducts() {
        console.log('Loading fallback mock products...');
        // Fallback to mock data if API fails
        this.products = {
            'PROD001': {
                id: 'PROD001',
                name: 'Wireless Bluetooth Headphones',
                description: 'Premium noise-cancelling wireless headphones',
                price: 199.99,
                image: 'https://via.placeholder.com/300x200/4A90E2/FFFFFF?text=Headphones',
                category: 'Electronics',
                rating: 4.5,
                reviews: 1250
            },
            'PROD002': {
                id: 'PROD002',
                name: 'Smart Fitness Watch',
                description: 'Advanced fitness tracking with heart rate monitor',
                price: 299.99,
                image: 'https://via.placeholder.com/300x200/5CB85C/FFFFFF?text=Smart+Watch',
                category: 'Electronics',
                rating: 4.7,
                reviews: 890
            },
            'PROD003': {
                id: 'PROD003',
                name: 'Organic Cotton T-Shirt',
                description: 'Comfortable organic cotton t-shirt',
                price: 29.99,
                image: 'https://via.placeholder.com/300x200/F0AD4E/FFFFFF?text=T-Shirt',
                category: 'Clothing',
                rating: 4.3,
                reviews: 456
            }
        };
        this.productsLoaded = true;
    }

    renderCartItems() {
        const container = document.getElementById('cart-items-container');
        const emptyCart = document.getElementById('empty-cart');
        const checkoutBtn = document.getElementById('checkout-btn');

        if (!container) return;

        if (this.cartItems.length === 0) {
            container.classList.add('hidden');
            emptyCart?.classList.remove('hidden');
            checkoutBtn.disabled = true;
            return;
        }

        container.classList.remove('hidden');
        emptyCart?.classList.add('hidden');
        checkoutBtn.disabled = false;

        container.innerHTML = this.cartItems.map(item => {
            const product = this.products[item.id];
            if (!product) return '';

            return `
                <div class="cart-item" data-item-id="${item.id}">
                    <div class="item-image">
                        <img src="${product.image}" alt="${product.name}" loading="lazy">
                    </div>
                    <div class="item-details">
                        <h3 class="item-name">${product.name}</h3>
                        <p class="item-description">${product.description}</p>
                    </div>
                    <div class="item-price">$${product.price.toFixed(2)}</div>
                    <div class="quantity-controls">
                        <button class="quantity-btn" onclick="cartManager.updateQuantity(${item.id}, ${item.quantity - 1})">
                            <i class="fas fa-minus"></i>
                        </button>
                        <input type="number" class="quantity-input" value="${item.quantity}" 
                               onchange="cartManager.updateQuantity(${item.id}, parseInt(this.value))" min="1">
                        <button class="quantity-btn" onclick="cartManager.updateQuantity(${item.id}, ${item.quantity + 1})">
                            <i class="fas fa-plus"></i>
                        </button>
                    </div>
                    <button class="remove-item-btn" onclick="cartManager.removeItem(${item.id})">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            `;
        }).join('');

        this.updateCartCount();
    }

    updateQuantity(productId, newQuantity) {
        if (newQuantity <= 0) {
            this.removeItem(productId);
            return;
        }

        const item = this.cartItems.find(item => item.id === productId);
        if (item) {
            const oldQuantity = item.quantity;
            item.quantity = newQuantity;
            
            if (newQuantity > oldQuantity) {
                this.itemsAdded++;
            } else {
                this.itemsRemoved++;
            }
            
            this.saveCart();
            this.renderCartItems();
            this.updateCartSummary();
            this.updateBehaviorStats();
            this.trackUserBehavior('quantity_change', { productId, oldQuantity, newQuantity });
        }
    }

    removeItem(productId) {
        this.cartItems = this.cartItems.filter(item => item.id !== productId);
        this.itemsRemoved++;
        this.saveCart();
        this.renderCartItems();
        this.updateCartSummary();
        this.updateBehaviorStats();
        this.trackUserBehavior('remove_from_cart', { productId });
        this.showNotification('Item removed from cart', 'info');
    }

    clearCart() {
        if (confirm('Are you sure you want to clear your cart?')) {
            this.cartItems = [];
            this.appliedDiscount = null;
            this.saveCart();
            this.renderCartItems();
            this.updateCartSummary();
            this.trackUserBehavior('clear_cart');
            this.showNotification('Cart cleared', 'info');
        }
    }

    updateCartSummary() {
        const subtotal = this.calculateSubtotal();
        const shipping = subtotal > 0 ? 9.99 : 0;
        const tax = subtotal * 0.08; // 8% tax
        let discount = 0;
        
        if (this.appliedDiscount) {
            discount = subtotal * (this.appliedDiscount.percentage / 100);
        }
        
        const total = subtotal + shipping + tax - discount;

        // Update display
        document.getElementById('subtotal').textContent = `$${subtotal.toFixed(2)}`;
        document.getElementById('shipping').textContent = subtotal > 0 ? `$${shipping.toFixed(2)}` : 'FREE';
        document.getElementById('tax').textContent = `$${tax.toFixed(2)}`;
        document.getElementById('total').textContent = `$${total.toFixed(2)}`;

        // Show/hide discount row
        const discountRow = document.getElementById('discount-row');
        if (this.appliedDiscount && discount > 0) {
            discountRow?.classList.remove('hidden');
            document.getElementById('discount-code-display').textContent = this.appliedDiscount.code;
            document.getElementById('discount-amount').textContent = `-$${discount.toFixed(2)}`;
        } else {
            discountRow?.classList.add('hidden');
        }

        this.updateCartCount();
    }

    calculateSubtotal() {
        return this.cartItems.reduce((total, item) => {
            const product = this.products[item.id];
            return total + (product ? product.price * item.quantity : 0);
        }, 0);
    }

    updateCartCount() {
        const cartCount = document.getElementById('cart-count');
        if (cartCount) {
            const totalItems = this.cartItems.reduce((sum, item) => sum + item.quantity, 0);
            cartCount.textContent = totalItems;
        }
    }

    async applyDiscount() {
        const codeInput = document.getElementById('discount-code-input');
        const messageDiv = document.getElementById('discount-message');
        const code = codeInput?.value.trim();

        if (!code) {
            this.showDiscountMessage('Please enter a discount code', 'error');
            return;
        }

        try {
            const response = await fetch('/api/validate-discount', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    userId: this.userId,
                    discountCode: code
                })
            });

            if (response.ok) {
                const discountData = await response.json();
                this.appliedDiscount = {
                    code: code,
                    percentage: discountData.percentage || 15,
                    description: discountData.description || 'Discount applied'
                };
                
                localStorage.setItem('appliedDiscount', JSON.stringify(this.appliedDiscount));
                this.updateCartSummary();
                this.showDiscountMessage(`${this.appliedDiscount.description} - ${this.appliedDiscount.percentage}% off applied!`, 'success');
                codeInput.value = '';
                
                this.trackUserBehavior('discount_applied', { code, percentage: this.appliedDiscount.percentage });
            } else {
                this.showDiscountMessage('Invalid or expired discount code', 'error');
            }
        } catch (error) {
            console.error('Error applying discount:', error);
            // Fallback for demo - accept common codes
            if (['SAVE15', 'WELCOME10', 'COMEBACK10', 'STAYWITHUS15'].includes(code.toUpperCase())) {
                const percentage = code.includes('15') ? 15 : 10;
                this.appliedDiscount = {
                    code: code.toUpperCase(),
                    percentage: percentage,
                    description: 'Demo discount'
                };
                
                localStorage.setItem('appliedDiscount', JSON.stringify(this.appliedDiscount));
                this.updateCartSummary();
                this.showDiscountMessage(`Demo discount applied - ${percentage}% off!`, 'success');
                codeInput.value = '';
            } else {
                this.showDiscountMessage('Invalid discount code', 'error');
            }
        }
    }

    showDiscountMessage(message, type) {
        const messageDiv = document.getElementById('discount-message');
        if (messageDiv) {
            messageDiv.textContent = message;
            messageDiv.className = `discount-message ${type}`;
            
            // Clear message after 5 seconds
            setTimeout(() => {
                messageDiv.textContent = '';
                messageDiv.className = 'discount-message';
            }, 5000);
        }
    }

    async proceedToCheckout() {
        if (this.cartItems.length === 0) return;

        // Track checkout attempt
        await this.trackUserBehavior('checkout_initiated', {
            cartValue: this.calculateSubtotal(),
            itemCount: this.cartItems.length
        });

        // For demo purposes, show success message
        this.showNotification('Redirecting to checkout...', 'success');
        
        // Simulate redirect delay
        setTimeout(() => {
            window.location.href = 'success.html';
        }, 1500);
    }

    async loadRecommendations() {
        try {
            const recommendations = await this.fetchRecommendations();
            this.renderRecommendations(recommendations);
        } catch (error) {
            console.error('Error loading recommendations:', error);
        }
    }

    async fetchRecommendations() {
        try {
            // Ensure products are loaded
            if (!this.productsLoaded) {
                await this.loadProductData();
            }
            
            // Return products not in cart
            const cartProductIds = this.cartItems.map(item => item.id.toString());
            const availableProducts = Object.values(this.products).filter(
                product => !cartProductIds.includes(product.id.toString())
            );
            
            // Simulate AI-powered recommendations by selecting products from different categories
            const recommendations = [];
            const categories = [...new Set(availableProducts.map(p => p.category))];
            
            for (const category of categories) {
                const categoryProducts = availableProducts.filter(p => p.category === category);
                if (categoryProducts.length > 0 && recommendations.length < 3) {
                    recommendations.push(categoryProducts[0]);
                }
            }
            
            // Fill remaining slots with any available products
            while (recommendations.length < 3 && recommendations.length < availableProducts.length) {
                const remaining = availableProducts.filter(p => !recommendations.includes(p));
                if (remaining.length > 0) {
                    recommendations.push(remaining[0]);
                } else {
                    break;
                }
            }
            
            console.log('Generated recommendations:', recommendations);
            return recommendations;
        } catch (error) {
            console.error('Error fetching recommendations:', error);
            return [];
        }
    }

    renderRecommendations(recommendations) {
        const container = document.getElementById('recommendations-container');
        if (!container) return;

        container.innerHTML = recommendations.map(product => `
            <div class="recommendation-item" onclick="cartManager.viewProduct(${product.id})">
                <div class="recommendation-image">
                    <img src="${product.image}" alt="${product.name}" loading="lazy">
                </div>
                <div class="recommendation-details">
                    <div class="recommendation-name">${product.name}</div>
                    <div class="recommendation-price">$${product.price.toFixed(2)}</div>
                </div>
                <button class="add-recommendation-btn" onclick="event.stopPropagation(); cartManager.addRecommendation(${product.id})">
                    <i class="fas fa-plus"></i>
                </button>
            </div>
        `).join('');
    }

    addRecommendation(productId) {
        const existingItem = this.cartItems.find(item => item.id === productId);
        
        if (existingItem) {
            existingItem.quantity += 1;
        } else {
            this.cartItems.push({ id: productId, quantity: 1 });
        }
        
        this.itemsAdded++;
        this.saveCart();
        this.renderCartItems();
        this.updateCartSummary();
        this.loadRecommendations(); // Refresh recommendations
        this.updateBehaviorStats();
        this.trackUserBehavior('add_recommendation', { productId });
        this.showNotification('Recommendation added to cart!', 'success');
    }

    viewProduct(productId) {
        window.location.href = `index.html?productId=${productId}`;
    }

    saveCart() {
        localStorage.setItem('cartItems', JSON.stringify(this.cartItems));
    }

    setupAbandonmentDetection() {
        let inactivityTimer;
        let mouseLeaveTimer;
        
        // Reset inactivity timer on user interaction
        const resetInactivityTimer = () => {
            clearTimeout(inactivityTimer);
            inactivityTimer = setTimeout(() => {
                this.triggerAbandonmentWarning();
            }, 30000); // 30 seconds of inactivity
        };

        // Track mouse movements and clicks
        document.addEventListener('mousemove', resetInactivityTimer);
        document.addEventListener('click', resetInactivityTimer);
        document.addEventListener('scroll', resetInactivityTimer);
        document.addEventListener('keypress', resetInactivityTimer);

        // Detect when user tries to leave the page
        document.addEventListener('mouseleave', () => {
            if (this.cartItems.length > 0) {
                mouseLeaveTimer = setTimeout(() => {
                    this.showAbandonmentModal();
                }, 1000); // Show modal after 1 second of mouse leave
            }
        });

        document.addEventListener('mouseenter', () => {
            clearTimeout(mouseLeaveTimer);
        });

        // Detect page unload
        window.addEventListener('beforeunload', (e) => {
            if (this.cartItems.length > 0) {
                this.trackUserBehavior('cart_abandonment_attempt');
                // Note: Modern browsers ignore custom messages
                e.preventDefault();
                e.returnValue = '';
            }
        });

        // Start initial timer
        resetInactivityTimer();
    }

    triggerAbandonmentWarning() {
        if (this.cartItems.length > 0) {
            this.abandonmentRisk = 'High';
            this.updateBehaviorStats();
            this.showAbandonmentBanner();
            this.trackUserBehavior('abandonment_warning_triggered');
        }
    }

    showAbandonmentBanner() {
        const banner = document.getElementById('abandonment-banner');
        banner?.classList.remove('hidden');
    }

    showAbandonmentModal() {
        const modal = document.getElementById('abandonment-modal');
        modal?.classList.remove('hidden');
        this.startModalCountdown();
        this.trackUserBehavior('abandonment_modal_shown');
    }

    closeAbandonmentModal() {
        const modal = document.getElementById('abandonment-modal');
        modal?.classList.add('hidden');
        clearInterval(this.modalCountdown);
    }

    startModalCountdown() {
        let timeLeft = 300; // 5 minutes
        const countdownElement = document.getElementById('modal-countdown');
        
        this.modalCountdown = setInterval(() => {
            const minutes = Math.floor(timeLeft / 60);
            const seconds = timeLeft % 60;
            
            if (countdownElement) {
                countdownElement.textContent = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
            }
            
            timeLeft--;
            
            if (timeLeft < 0) {
                this.closeAbandonmentModal();
            }
        }, 1000);
    }

    async acceptAbandonmentOffer() {
        // Apply the abandonment recovery discount
        this.appliedDiscount = {
            code: 'STAYWITHUS15',
            percentage: 15,
            description: 'Abandonment recovery discount'
        };
        
        localStorage.setItem('appliedDiscount', JSON.stringify(this.appliedDiscount));
        this.updateCartSummary();
        this.closeAbandonmentModal();
        this.showNotification('15% discount applied! Thanks for staying!', 'success');
        
        await this.trackUserBehavior('abandonment_offer_accepted', {
            discountCode: 'STAYWITHUS15',
            percentage: 15
        });
    }

    declineAbandonmentOffer() {
        this.closeAbandonmentModal();
        this.trackUserBehavior('abandonment_offer_declined');
        
        // Show final attempt banner
        setTimeout(() => {
            this.showAbandonmentBanner();
        }, 2000);
    }

    async applyRecoveryDiscount() {
        this.appliedDiscount = {
            code: 'COMEBACK10',
            percentage: 10,
            description: 'Recovery discount'
        };
        
        localStorage.setItem('appliedDiscount', JSON.stringify(this.appliedDiscount));
        this.updateCartSummary();
        document.getElementById('abandonment-banner')?.classList.add('hidden');
        this.showNotification('Recovery discount applied!', 'success');
        
        await this.trackUserBehavior('recovery_discount_applied', {
            discountCode: 'COMEBACK10',
            percentage: 10
        });
    }

    async trackUserBehavior(eventType, data = {}) {
        try {
            const behaviorData = {
                userId: this.userId,
                eventType,
                timestamp: new Date().toISOString(),
                metadata: {
                    page: 'cart',
                    cartValue: this.calculateSubtotal(),
                    itemCount: this.cartItems.length,
                    timeInCart: Math.floor((Date.now() - this.startTime) / 1000),
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

    updateBehaviorStats() {
        const timeInCart = Math.floor((Date.now() - this.startTime) / 1000);
        const minutes = Math.floor(timeInCart / 60);
        const seconds = timeInCart % 60;
        
        document.getElementById('time-in-cart').textContent = `${minutes}:${seconds.toString().padStart(2, '0')}`;
        document.getElementById('items-added').textContent = this.itemsAdded;
        document.getElementById('items-removed').textContent = this.itemsRemoved;
        
        const riskElement = document.getElementById('abandonment-risk');
        if (riskElement) {
            riskElement.textContent = this.abandonmentRisk;
            riskElement.className = `stat-value risk-${this.abandonmentRisk.toLowerCase()}`;
        }
    }

    startBehaviorTracking() {
        // Track page load
        this.trackUserBehavior('cart_page_load');
        
        // Update behavior stats every second
        setInterval(() => {
            this.updateBehaviorStats();
        }, 1000);
        
        // Track scroll behavior
        let scrollTimeout;
        window.addEventListener('scroll', () => {
            clearTimeout(scrollTimeout);
            scrollTimeout = setTimeout(() => {
                this.trackUserBehavior('cart_scroll');
            }, 2000);
        });
    }

    showNotification(message, type = 'info') {
        // Create notification element
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.textContent = message;
        
        document.body.appendChild(notification);
        
        // Remove after 3 seconds
        setTimeout(() => {
            notification.remove();
        }, 3000);
    }
}

// Initialize cart manager when page loads
let cartManager;
document.addEventListener('DOMContentLoaded', () => {
    cartManager = new CartManager();
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
