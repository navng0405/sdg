// InstantSearch Catalog Manager
class InstantSearchCatalog {
    constructor() {
        this.searchClient = null;
        this.search = null;
        this.behaviorStats = {
            productsViewed: 0,
            searchQueries: 0,
            hesitationScore: 0
        };
        this.viewedProducts = new Set();
        this.searchStartTime = Date.now();
        this.lastSearchTime = Date.now();
        this.init();
    }

    async init() {
        try {
            // Initialize Algolia search client with hardcoded configuration
            this.searchClient = algoliasearch('D5NLZGUAN7', 'f563ed99dff0f7ccb8c2b78e681376be');
            
            // Initialize InstantSearch
            this.search = instantsearch({
                indexName: 'sdg_products',
                searchClient: this.searchClient,
                routing: true
            });

            this.setupWidgets();
            this.setupEventListeners();
            this.startBehaviorTracking();
            
            // Start the search
            this.search.start();
            
            console.log('InstantSearch initialized successfully');
        } catch (error) {
            console.error('Error initializing InstantSearch:', error);
            this.fallbackToCustomSearch();
        }
    }

    setupWidgets() {
        // Search Box
        this.search.addWidgets([
            instantsearch.widgets.searchBox({
                container: '#searchbox',
                placeholder: 'Search for products...',
                showReset: true,
                showSubmit: true,
                cssClasses: {
                    form: 'search-form',
                    input: 'search-input',
                    submit: 'search-submit',
                    reset: 'search-reset'
                }
            })
        ]);

        // Hits (Products)
        this.search.addWidgets([
            instantsearch.widgets.hits({
                container: '#hits',
                templates: {
                    item: (hit, { html, components }) => html`
                        <div class="product-card" data-product-id="${hit.objectID}">
                            <img 
                                src="${hit.image_url || `https://via.placeholder.com/300x200/4A90E2/FFFFFF?text=${encodeURIComponent(hit.name)}`}"
                                alt="${hit.name}" 
                                class="product-image"
                                loading="lazy"
                            />
                            <div class="product-info">
                                <h3 class="product-name">${components.Highlight({ hit, attribute: 'name' })}</h3>
                                <p class="product-description">${hit.description || 'No description available'}</p>
                                
                                <div class="product-price">
                                    <span class="current-price">$${parseFloat(hit.price).toFixed(2)}</span>
                                    ${hit.originalPrice ? html`
                                        <span class="original-price">$${parseFloat(hit.originalPrice).toFixed(2)}</span>
                                        <span class="discount-badge">${Math.round((1 - hit.price/hit.originalPrice) * 100)}% OFF</span>
                                    ` : ''}
                                </div>
                                
                                <div class="product-rating">
                                    <div class="stars">
                                        ${this.renderStars(hit.average_rating || 4.5)}
                                    </div>
                                    <span class="rating-text">(${hit.number_of_reviews || 100} reviews)</span>
                                </div>
                                
                                <div class="product-actions">
                                    <button class="btn btn-primary add-to-cart-btn" data-product-id="${hit.objectID}">
                                        <i class="fas fa-shopping-cart"></i> Add to Cart
                                    </button>
                                    <button class="btn btn-secondary view-details-btn" data-product-id="${hit.objectID}">
                                        <i class="fas fa-eye"></i> View Details
                                    </button>
                                </div>
                            </div>
                        </div>
                    `,
                    empty: `
                        <div class="empty-results">
                            <i class="fas fa-search" style="font-size: 3rem; color: #ccc; margin-bottom: 1rem;"></i>
                            <h3>No products found</h3>
                            <p>Try adjusting your search or filters</p>
                        </div>
                    `
                }
            })
        ]);

        // Category Refinement
        this.search.addWidgets([
            instantsearch.widgets.refinementList({
                container: '#category-list',
                attribute: 'category',
                limit: 10,
                showMore: true,
                cssClasses: {
                    showMore: 'btn btn-sm btn-secondary',
                    count: 'refinement-count'
                }
            })
        ]);

        // Price Range
        this.search.addWidgets([
            instantsearch.widgets.rangeSlider({
                container: '#price-range',
                attribute: 'price',
                tooltips: {
                    format: (value) => `$${Math.round(value)}`
                },
                cssClasses: {
                    root: 'price-range-slider'
                }
            })
        ]);

        // Rating Filter
        this.search.addWidgets([
            instantsearch.widgets.ratingMenu({
                container: '#rating-list',
                attribute: 'average_rating',
                max: 5,
                cssClasses: {
                    starIcon: 'fas fa-star',
                    count: 'refinement-count'
                }
            })
        ]);

        // Clear Refinements
        this.search.addWidgets([
            instantsearch.widgets.clearRefinements({
                container: '#clear-refinements',
                cssClasses: {
                    button: 'clear-filters-btn'
                }
            })
        ]);

        // Stats
        this.search.addWidgets([
            instantsearch.widgets.stats({
                container: '#stats',
                templates: {
                    text: (data) => {
                        const { nbHits, processingTimeMS, query } = data;
                        return `${nbHits.toLocaleString()} products found in ${processingTimeMS}ms`;
                    }
                }
            })
        ]);

        // Sort By
        this.search.addWidgets([
            instantsearch.widgets.sortBy({
                container: '#sort-by',
                items: [
                    { label: 'Relevance', value: 'sdg_products' }
                ]
            })
        ]);

        // Pagination
        this.search.addWidgets([
            instantsearch.widgets.pagination({
                container: '#pagination',
                padding: 2,
                cssClasses: {
                    list: 'pagination-list',
                    item: 'pagination-item',
                    link: 'pagination-link'
                }
            })
        ]);
    }

    setupEventListeners() {
        // Track search queries
        this.search.on('render', () => {
            this.behaviorStats.searchQueries++;
            this.updateBehaviorStats();
        });

        // Track product interactions
        document.addEventListener('click', (e) => {
            if (e.target.closest('.add-to-cart-btn')) {
                const productId = e.target.closest('.add-to-cart-btn').dataset.productId;
                this.addToCart(productId);
            }
            
            if (e.target.closest('.view-details-btn')) {
                const productId = e.target.closest('.view-details-btn').dataset.productId;
                this.viewProductDetails(productId);
            }
            
            if (e.target.closest('.product-card')) {
                const productId = e.target.closest('.product-card').dataset.productId;
                this.trackProductView(productId);
            }
        });

        // Discount banner interactions
        document.getElementById('apply-discount-btn')?.addEventListener('click', () => {
            this.applySmartDiscount();
        });

        document.getElementById('dismiss-banner')?.addEventListener('click', () => {
            this.dismissDiscountBanner();
        });

        // Behavior panel toggle
        document.getElementById('toggle-behavior')?.addEventListener('click', () => {
            this.toggleBehaviorPanel();
        });
    }

    renderStars(rating) {
        const fullStars = Math.floor(rating);
        const hasHalfStar = rating % 1 !== 0;
        let starsHtml = '';
        
        for (let i = 0; i < fullStars; i++) {
            starsHtml += '<i class="fas fa-star"></i>';
        }
        
        if (hasHalfStar) {
            starsHtml += '<i class="fas fa-star-half-alt"></i>';
        }
        
        const emptyStars = 5 - Math.ceil(rating);
        for (let i = 0; i < emptyStars; i++) {
            starsHtml += '<i class="far fa-star"></i>';
        }
        
        return starsHtml;
    }

    trackProductView(productId) {
        if (!this.viewedProducts.has(productId)) {
            this.viewedProducts.add(productId);
            this.behaviorStats.productsViewed++;
            this.updateBehaviorStats();
            
            // Track behavior for AI analysis
            this.trackUserBehavior('product_view', { productId });
            
            // Check for hesitation patterns
            this.analyzeHesitationPattern();
        }
    }

    analyzeHesitationPattern() {
        const now = Date.now();
        const timeSinceLastSearch = now - this.lastSearchTime;
        const totalBrowsingTime = now - this.searchStartTime;
        
        // Calculate hesitation score based on browsing patterns
        let hesitationScore = 0;
        
        if (this.behaviorStats.productsViewed > 5 && timeSinceLastSearch > 30000) {
            hesitationScore += 30;
        }
        
        if (totalBrowsingTime > 120000) { // 2 minutes
            hesitationScore += 20;
        }
        
        if (this.behaviorStats.searchQueries > 3) {
            hesitationScore += 25;
        }
        
        this.behaviorStats.hesitationScore = Math.min(hesitationScore, 100);
        this.updateBehaviorStats();
        
        // Trigger smart discount if hesitation is high
        if (this.behaviorStats.hesitationScore > 60) {
            this.triggerSmartDiscount();
        }
    }

    async triggerSmartDiscount() {
        try {
            const response = await fetch(`/api/get-discount?userId=user123`);
            if (response.ok) {
                const discountData = await response.json();
                this.showDiscountBanner(discountData);
            }
        } catch (error) {
            console.error('Error fetching smart discount:', error);
        }
    }

    showDiscountBanner(discountData) {
        const banner = document.getElementById('discount-banner');
        const message = document.getElementById('discount-message');
        
        if (discountData.discount) {
            message.textContent = `${discountData.discount.percentage}% off - ${discountData.discount.description}`;
            banner.style.display = 'block';
        }
    }

    dismissDiscountBanner() {
        document.getElementById('discount-banner').style.display = 'none';
    }

    async applySmartDiscount() {
        // Implementation for applying discount
        this.showNotification('Smart discount applied!', 'success');
        this.dismissDiscountBanner();
    }

    addToCart(productId) {
        // Get existing cart or create new one
        let cart = JSON.parse(localStorage.getItem('cartItems') || '[]');
        
        const existingItem = cart.find(item => item.id === productId);
        if (existingItem) {
            existingItem.quantity += 1;
        } else {
            cart.push({ id: productId, quantity: 1 });
        }
        
        localStorage.setItem('cartItems', JSON.stringify(cart));
        this.trackUserBehavior('add_to_cart', { productId });
        this.showNotification('Product added to cart!', 'success');
    }

    viewProductDetails(productId) {
        this.trackUserBehavior('view_details', { productId });
        // Navigate to product details or open modal
        window.location.href = `index.html?productId=${productId}`;
    }

    async trackUserBehavior(eventType, data = {}) {
        try {
            await fetch('/api/user-behavior', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    userId: 'user123',
                    eventType,
                    timestamp: new Date().toISOString(),
                    ...data
                })
            });
        } catch (error) {
            console.error('Error tracking user behavior:', error);
        }
    }

    updateBehaviorStats() {
        document.getElementById('products-viewed').textContent = this.behaviorStats.productsViewed;
        document.getElementById('search-queries').textContent = this.behaviorStats.searchQueries;
        document.getElementById('hesitation-score').textContent = `${this.behaviorStats.hesitationScore}%`;
        
        // Update AI insights
        this.updateAIInsights();
    }

    updateAIInsights() {
        const insightsList = document.getElementById('behavior-insights-list');
        const insights = [];
        
        if (this.behaviorStats.productsViewed > 3) {
            insights.push('High engagement detected - showing interest in multiple products');
        }
        
        if (this.behaviorStats.hesitationScore > 40) {
            insights.push('Hesitation pattern identified - consider offering discount');
        }
        
        if (this.behaviorStats.searchQueries > 2) {
            insights.push('Active search behavior - user knows what they want');
        }
        
        if (insights.length === 0) {
            insights.push('Analyzing browsing patterns...');
        }
        
        insightsList.innerHTML = insights.map(insight => `<li>${insight}</li>`).join('');
    }

    toggleBehaviorPanel() {
        const content = document.querySelector('.behavior-content');
        const icon = document.querySelector('#toggle-behavior i');
        
        if (content.style.display === 'none') {
            content.style.display = 'block';
            icon.className = 'fas fa-chevron-up';
        } else {
            content.style.display = 'none';
            icon.className = 'fas fa-chevron-down';
        }
    }

    showNotification(message, type = 'info') {
        // Create notification element
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.textContent = message;
        
        // Style the notification
        Object.assign(notification.style, {
            position: 'fixed',
            top: '20px',
            right: '20px',
            padding: '1rem 1.5rem',
            borderRadius: '8px',
            color: 'white',
            fontWeight: '500',
            zIndex: '10000',
            transform: 'translateX(100%)',
            transition: 'transform 0.3s ease'
        });
        
        if (type === 'success') {
            notification.style.background = '#28a745';
        } else if (type === 'error') {
            notification.style.background = '#dc3545';
        } else {
            notification.style.background = '#17a2b8';
        }
        
        document.body.appendChild(notification);
        
        // Animate in
        setTimeout(() => {
            notification.style.transform = 'translateX(0)';
        }, 100);
        
        // Remove after 3 seconds
        setTimeout(() => {
            notification.style.transform = 'translateX(100%)';
            setTimeout(() => {
                document.body.removeChild(notification);
            }, 300);
        }, 3000);
    }

    startBehaviorTracking() {
        // Track page load
        this.trackUserBehavior('catalog_page_load');
        
        // Update behavior stats every 5 seconds
        setInterval(() => {
            this.analyzeHesitationPattern();
        }, 5000);
    }

    fallbackToCustomSearch() {
        console.log('Falling back to custom search implementation');
        // Implement fallback search using your existing API
        // This would use your /api/products endpoint instead of InstantSearch
    }
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    new InstantSearchCatalog();
});
