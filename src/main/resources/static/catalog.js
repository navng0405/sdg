// Catalog Page JavaScript - Smart Discount Generator
class CatalogManager {
    constructor() {
        this.currentPage = 1;
        this.itemsPerPage = 12;
        this.currentFilters = {
            search: '',
            category: '',
            priceRange: '',
            sortBy: 'relevance'
        };
        this.behaviorStats = {
            productsViewed: 0,
            searchQueries: 0,
            hesitationScore: 0,
            aiConfidence: 85
        };
        this.cartItems = JSON.parse(localStorage.getItem('cartItems') || '[]');
        this.userId = 'user-001';
        
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.loadProducts();
        this.loadRecommendations();
        this.updateCartCount();
        this.startBehaviorTracking();
    }

    setupEventListeners() {
        // Search functionality
        const searchInput = document.getElementById('search-input');
        const searchBtn = document.getElementById('search-btn');
        
        searchBtn?.addEventListener('click', () => this.handleSearch());
        searchInput?.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.handleSearch();
        });

        // Filter functionality
        document.getElementById('category-filter')?.addEventListener('change', (e) => {
            this.currentFilters.category = e.target.value;
            this.loadProducts();
        });

        document.getElementById('price-filter')?.addEventListener('change', (e) => {
            this.currentFilters.priceRange = e.target.value;
            this.loadProducts();
        });

        document.getElementById('sort-filter')?.addEventListener('change', (e) => {
            this.currentFilters.sortBy = e.target.value;
            this.loadProducts();
        });

        // View toggle
        document.getElementById('grid-view')?.addEventListener('click', () => this.setView('grid'));
        document.getElementById('list-view')?.addEventListener('click', () => this.setView('list'));

        // Pagination
        document.getElementById('prev-page')?.addEventListener('click', () => this.changePage(-1));
        document.getElementById('next-page')?.addEventListener('click', () => this.changePage(1));

        // Behavior panel toggle
        document.getElementById('toggle-behavior-panel')?.addEventListener('click', () => {
            const content = document.getElementById('behavior-content');
            content?.classList.toggle('hidden');
        });

        // User ID input
        document.getElementById('user-id')?.addEventListener('change', (e) => {
            this.userId = e.target.value;
        });

        // Discount banner close
        document.getElementById('close-discount-btn')?.addEventListener('click', () => {
            document.getElementById('discount-banner')?.classList.add('hidden');
        });

        // Claim discount
        document.getElementById('claim-discount-btn')?.addEventListener('click', () => {
            this.claimDiscount();
        });
    }

    async handleSearch() {
        const searchInput = document.getElementById('search-input');
        const query = searchInput?.value.trim();
        
        if (query) {
            this.currentFilters.search = query;
            this.behaviorStats.searchQueries++;
            this.updateBehaviorStats();
            
            // Track search behavior
            await this.trackUserBehavior('search', { query });
            
            this.loadProducts();
        }
    }

    async loadProducts() {
        this.showLoading(true);
        
        try {
            // Simulate API call - in real implementation, this would call your Algolia service
            const products = await this.fetchProducts();
            this.renderProducts(products);
            this.updatePagination();
        } catch (error) {
            console.error('Error loading products:', error);
            this.showError('Failed to load products');
        } finally {
            this.showLoading(false);
        }
    }

    async fetchProducts() {
        try {
            console.log('Fetching products from Algolia API...');
            
            // Build query parameters
            const params = new URLSearchParams();
            
            if (this.currentFilters.search) {
                params.append('query', this.currentFilters.search);
            }
            
            params.append('limit', '50');
            
            // Fetch products from API
            const response = await fetch(`/api/products?${params.toString()}`);
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            const data = await response.json();
            console.log('Loaded products from API:', data);
            
            // Convert Algolia products to frontend format
            let products = data.products.map(product => ({
                id: product.objectID,
                name: product.name,
                description: product.description,
                price: parseFloat(product.price),
                originalPrice: product.price * 1.25, // Simulate original price
                image: product.image_url || `https://via.placeholder.com/300x200/4A90E2/FFFFFF?text=${encodeURIComponent(product.name)}`,
                rating: product.average_rating || 4.5,
                reviews: product.number_of_reviews || 100,
                category: product.category?.toLowerCase() || 'general',
                discount: Math.floor((1 - (product.price / (product.price * 1.25))) * 100),
                profitMargin: product.profit_margin || 0.3
            }));
            
            // Apply client-side filters
            let filteredProducts = products;
            
            // Category filter
            if (this.currentFilters.category) {
                filteredProducts = filteredProducts.filter(product =>
                    product.category === this.currentFilters.category
                );
            }
            
            // Price range filter
            if (this.currentFilters.priceRange) {
                const [min, max] = this.currentFilters.priceRange.split('-').map(p => 
                    p === '+' ? Infinity : parseInt(p)
                );
                filteredProducts = filteredProducts.filter(product =>
                    product.price >= min && (max === undefined || product.price <= max)
                );
            }
            
            // Apply sorting
            switch (this.currentFilters.sortBy) {
                case 'price-low':
                    filteredProducts.sort((a, b) => a.price - b.price);
                    break;
                case 'price-high':
                    filteredProducts.sort((a, b) => b.price - a.price);
                    break;
                case 'rating':
                    filteredProducts.sort((a, b) => b.rating - a.rating);
                    break;
                case 'newest':
                    // Simulate newest first (reverse order)
                    filteredProducts.reverse();
                    break;
                case 'relevance':
                default:
                    // Keep Algolia's relevance order
                    break;
            }
            
            console.log('Filtered products:', filteredProducts.length);
            return filteredProducts;
            
        } catch (error) {
            console.error('Error fetching products:', error);
            // Return fallback mock data
            return this.getFallbackProducts();
        }
    }
    
    getFallbackProducts() {
        console.log('Using fallback product data...');
        return [
            {
                id: 'PROD001',
                name: 'Wireless Bluetooth Headphones',
                description: 'Premium noise-cancelling wireless headphones with 30-hour battery life',
                price: 199.99,
                originalPrice: 249.99,
                image: 'https://via.placeholder.com/300x200/4A90E2/FFFFFF?text=Headphones',
                rating: 4.5,
                reviews: 1250,
                category: 'electronics',
                discount: 20
            },
            {
                id: 'PROD002',
                name: 'Smart Fitness Watch',
                description: 'Advanced fitness tracking with heart rate monitor and GPS',
                price: 299.99,
                originalPrice: 399.99,
                image: 'https://via.placeholder.com/300x200/5CB85C/FFFFFF?text=Smart+Watch',
                rating: 4.7,
                reviews: 890,
                category: 'electronics',
                discount: 25
            },
            {
                id: 'PROD003',
                name: 'Organic Cotton T-Shirt',
                description: 'Comfortable organic cotton t-shirt in various colors',
                price: 29.99,
                originalPrice: 39.99,
                image: 'https://via.placeholder.com/300x200/F0AD4E/FFFFFF?text=T-Shirt',
                rating: 4.3,
                reviews: 456,
                category: 'clothing',
                discount: 25
            }
        ];
    }

    renderProducts(products) {
        const productsGrid = document.getElementById('products-grid');
        if (!productsGrid) return;

        productsGrid.innerHTML = products.map(product => `
            <div class="product-card" data-product-id="${product.id}" onclick="catalogManager.viewProduct(${product.id})">
                <div class="product-image">
                    <img src="${product.image}" alt="${product.name}" loading="lazy">
                    ${product.discount ? `<div class="discount-badge">${product.discount}% OFF</div>` : ''}
                </div>
                <div class="product-info">
                    <h3 class="product-name">${product.name}</h3>
                    <p class="product-description">${product.description}</p>
                    <div class="product-rating">
                        <div class="stars">
                            ${this.generateStars(product.rating)}
                        </div>
                        <span class="rating-text">${product.rating} (${product.reviews} reviews)</span>
                    </div>
                    <div class="product-price">
                        <span class="current-price">$${product.price}</span>
                        ${product.originalPrice && product.originalPrice > product.price ? 
                            `<span class="original-price">$${product.originalPrice}</span>` : ''}
                    </div>
                    <div class="product-actions">
                        <button class="add-to-cart-btn" onclick="event.stopPropagation(); catalogManager.addToCart(${product.id})">
                            <i class="fas fa-shopping-cart"></i> Add to Cart
                        </button>
                        <button class="quick-view-btn" onclick="event.stopPropagation(); catalogManager.quickView(${product.id})">
                            <i class="fas fa-eye"></i>
                        </button>
                    </div>
                </div>
            </div>
        `).join('');
    }

    async loadRecommendations() {
        try {
            // Simulate AI-powered recommendations
            const recommendations = await this.fetchRecommendations();
            this.renderRecommendations(recommendations);
        } catch (error) {
            console.error('Error loading recommendations:', error);
        }
    }

    async fetchRecommendations() {
        // Simulate API call to get AI recommendations
        await new Promise(resolve => setTimeout(resolve, 300));
        
        return [
            {
                id: 7,
                name: 'AI Recommended: Smart Speaker',
                description: 'Voice-controlled smart speaker with AI assistant',
                price: 79.99,
                originalPrice: 99.99,
                image: 'https://via.placeholder.com/250x150/6C5CE7/FFFFFF?text=Smart+Speaker',
                rating: 4.5,
                aiConfidence: 92
            },
            {
                id: 8,
                name: 'Trending: Wireless Charger',
                description: 'Fast wireless charging pad for all devices',
                price: 39.99,
                originalPrice: 59.99,
                image: 'https://via.placeholder.com/250x150/A29BFE/FFFFFF?text=Wireless+Charger',
                rating: 4.3,
                aiConfidence: 88
            }
        ];
    }

    renderRecommendations(recommendations) {
        const recommendationsGrid = document.getElementById('recommendations-grid');
        if (!recommendationsGrid) return;

        recommendationsGrid.innerHTML = recommendations.map(product => `
            <div class="product-card recommendation-card" data-product-id="${product.id}" onclick="catalogManager.viewProduct(${product.id})">
                <div class="product-image">
                    <img src="${product.image}" alt="${product.name}" loading="lazy">
                    <div class="ai-badge">AI ${product.aiConfidence}%</div>
                </div>
                <div class="product-info">
                    <h3 class="product-name">${product.name}</h3>
                    <p class="product-description">${product.description}</p>
                    <div class="product-price">
                        <span class="current-price">$${product.price}</span>
                        ${product.originalPrice ? `<span class="original-price">$${product.originalPrice}</span>` : ''}
                    </div>
                    <button class="add-to-cart-btn" onclick="event.stopPropagation(); catalogManager.addToCart(${product.id})">
                        <i class="fas fa-shopping-cart"></i> Add to Cart
                    </button>
                </div>
            </div>
        `).join('');
    }

    generateStars(rating) {
        const fullStars = Math.floor(rating);
        const hasHalfStar = rating % 1 !== 0;
        const emptyStars = 5 - fullStars - (hasHalfStar ? 1 : 0);

        return '★'.repeat(fullStars) + 
               (hasHalfStar ? '☆' : '') + 
               '☆'.repeat(emptyStars);
    }

    async viewProduct(productId) {
        this.behaviorStats.productsViewed++;
        this.updateBehaviorStats();
        
        // Track product view behavior
        await this.trackUserBehavior('product_view', { productId });
        
        // Check for discount eligibility
        this.checkDiscountEligibility();
        
        // Navigate to product detail page
        window.location.href = `index.html?productId=${productId}`;
    }

    async addToCart(productId) {
        // Add to cart logic
        const existingItem = this.cartItems.find(item => item.id === productId);
        
        if (existingItem) {
            existingItem.quantity += 1;
        } else {
            this.cartItems.push({ id: productId, quantity: 1 });
        }
        
        localStorage.setItem('cartItems', JSON.stringify(this.cartItems));
        this.updateCartCount();
        
        // Track add to cart behavior
        await this.trackUserBehavior('add_to_cart', { productId });
        
        // Show success message
        this.showNotification('Product added to cart!', 'success');
    }

    quickView(productId) {
        // Quick view modal logic would go here
        console.log('Quick view for product:', productId);
    }

    updateCartCount() {
        const cartCount = document.getElementById('cart-count');
        if (cartCount) {
            const totalItems = this.cartItems.reduce((sum, item) => sum + item.quantity, 0);
            cartCount.textContent = totalItems;
        }
    }

    setView(viewType) {
        const productsGrid = document.getElementById('products-grid');
        const gridBtn = document.getElementById('grid-view');
        const listBtn = document.getElementById('list-view');
        
        if (viewType === 'list') {
            productsGrid?.classList.add('list-view');
            gridBtn?.classList.remove('active');
            listBtn?.classList.add('active');
        } else {
            productsGrid?.classList.remove('list-view');
            gridBtn?.classList.add('active');
            listBtn?.classList.remove('active');
        }
    }

    changePage(direction) {
        this.currentPage += direction;
        this.loadProducts();
    }

    updatePagination() {
        const prevBtn = document.getElementById('prev-page');
        const nextBtn = document.getElementById('next-page');
        const pageInfo = document.getElementById('page-info');
        
        if (prevBtn) prevBtn.disabled = this.currentPage <= 1;
        if (nextBtn) nextBtn.disabled = this.currentPage >= 5; // Mock total pages
        if (pageInfo) pageInfo.textContent = `Page ${this.currentPage} of 5`;
    }

    showLoading(show) {
        const spinner = document.getElementById('loading-spinner');
        const productsGrid = document.getElementById('products-grid');
        
        if (show) {
            spinner?.classList.remove('hidden');
            productsGrid?.classList.add('hidden');
        } else {
            spinner?.classList.add('hidden');
            productsGrid?.classList.remove('hidden');
        }
    }

    showError(message) {
        this.showNotification(message, 'error');
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

    async trackUserBehavior(eventType, data = {}) {
        try {
            const behaviorData = {
                userId: this.userId,
                eventType,
                timestamp: new Date().toISOString(),
                productId: data.productId || null,
                query: data.query || null,
                metadata: {
                    page: 'catalog',
                    userAgent: navigator.userAgent,
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

    async checkDiscountEligibility() {
        // Increase hesitation score based on behavior
        this.behaviorStats.hesitationScore = Math.min(100, this.behaviorStats.hesitationScore + 10);
        this.updateBehaviorStats();
        
        // Check if user is eligible for discount
        if (this.behaviorStats.hesitationScore > 50) {
            try {
                const response = await fetch(`/api/get-discount?userId=${this.userId}`);
                if (response.ok) {
                    const discountData = await response.json();
                    this.showDiscountBanner(discountData);
                }
            } catch (error) {
                console.error('Error checking discount eligibility:', error);
            }
        }
    }

    showDiscountBanner(discountData) {
        const banner = document.getElementById('discount-banner');
        const headline = document.getElementById('discount-headline');
        const message = document.getElementById('discount-message');
        const amount = document.getElementById('discount-amount');
        const code = document.getElementById('discount-code');
        
        if (banner && discountData) {
            headline.textContent = discountData.headline || 'Special Offer!';
            message.textContent = discountData.message || 'AI detected your interest - here\'s a personalized discount!';
            amount.textContent = `${discountData.percentage}% OFF`;
            code.textContent = `Code: ${discountData.code}`;
            
            banner.classList.remove('hidden');
            this.startCountdown(discountData.expiryMinutes || 30);
        }
    }

    startCountdown(minutes) {
        const timer = document.getElementById('countdown-timer');
        let totalSeconds = minutes * 60;
        
        const interval = setInterval(() => {
            const mins = Math.floor(totalSeconds / 60);
            const secs = totalSeconds % 60;
            
            if (timer) {
                timer.textContent = `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
            }
            
            totalSeconds--;
            
            if (totalSeconds < 0) {
                clearInterval(interval);
                document.getElementById('discount-banner')?.classList.add('hidden');
            }
        }, 1000);
    }

    async claimDiscount() {
        const code = document.getElementById('discount-code')?.textContent.replace('Code: ', '');
        
        try {
            const response = await fetch('/api/apply-discount', {
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
                this.showNotification('Discount applied successfully!', 'success');
                document.getElementById('discount-banner')?.classList.add('hidden');
            }
        } catch (error) {
            console.error('Error applying discount:', error);
            this.showNotification('Failed to apply discount', 'error');
        }
    }

    updateBehaviorStats() {
        document.getElementById('products-viewed').textContent = this.behaviorStats.productsViewed;
        document.getElementById('search-queries').textContent = this.behaviorStats.searchQueries;
        document.getElementById('hesitation-score').textContent = this.behaviorStats.hesitationScore;
        document.getElementById('ai-confidence').textContent = `${this.behaviorStats.aiConfidence}%`;
    }

    startBehaviorTracking() {
        // Track page load
        this.trackUserBehavior('page_load', { page: 'catalog' });
        
        // Track scroll behavior
        let scrollTimeout;
        window.addEventListener('scroll', () => {
            clearTimeout(scrollTimeout);
            scrollTimeout = setTimeout(() => {
                this.behaviorStats.hesitationScore = Math.min(100, this.behaviorStats.hesitationScore + 1);
                this.updateBehaviorStats();
            }, 1000);
        });
        
        // Track mouse movement for hesitation detection
        let mouseTimeout;
        document.addEventListener('mousemove', () => {
            clearTimeout(mouseTimeout);
            mouseTimeout = setTimeout(() => {
                this.behaviorStats.hesitationScore = Math.min(100, this.behaviorStats.hesitationScore + 0.5);
                this.updateBehaviorStats();
            }, 2000);
        });
    }
}

// Initialize catalog manager when page loads
let catalogManager;
document.addEventListener('DOMContentLoaded', () => {
    catalogManager = new CatalogManager();
});

// Add some additional CSS for notifications
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

.ai-badge {
    position: absolute;
    top: 10px;
    left: 10px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    padding: 0.3rem 0.6rem;
    border-radius: 15px;
    font-size: 0.8rem;
    font-weight: bold;
}

.recommendation-card {
    border: 2px solid #667eea;
}
</style>
`;

document.head.insertAdjacentHTML('beforeend', notificationStyles);
