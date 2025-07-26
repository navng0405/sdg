# Gemini Prompt for Smart Discount Generator Algolia Index Content

## Context
You are helping to generate comprehensive sample data for a Smart Discount Generator application that uses Algolia for data storage. The application analyzes user behavior to generate AI-powered personalized discounts for e-commerce. 

## Required Indexes and Schemas

### 1. Products Index (`sdg_products`)
Generate 20-25 diverse e-commerce products with the following JSON schema:

```json
{
  "objectID": "PROD001",
  "name": "Product Name",
  "description": "Detailed product description",
  "price": 199.99,
  "category": "Electronics|Sports|Fashion|Home|Books|Beauty|Automotive",
  "profit_margin": 0.35,
  "inventory_level": 50,
  "image_url": "https://example.com/product-image.jpg",
  "average_rating": 4.5,
  "number_of_reviews": 1250,
  "brand": "Brand Name",
  "tags": ["tag1", "tag2", "tag3"],
  "specifications": {
    "color": "Black",
    "size": "Medium",
    "weight": "1.2kg"
  }
}
```

**Requirements:**
- Mix of different categories (Electronics, Sports, Fashion, Home, Books, Beauty, Automotive)
- Price range: $19.99 to $899.99
- Profit margins: 0.15 to 0.50 (15% to 50%)
- Inventory levels: 5 to 200 units
- Ratings: 3.0 to 5.0
- Reviews: 10 to 5000
- Realistic product names and descriptions
- Include popular items that would trigger discount behaviors

### 2. User Events Index (`sdg_user_events`)
Generate 30-40 sample user behavior events with this schema:

```json
{
  "objectID": "event-uuid-123",
  "userId": "user-123",
  "eventType": "cart_abandon|product_view|search_query|no_results_search|price_hover|multiple_product_views",
  "timestamp": "2025-01-26T13:30:00Z",
  "productId": "PROD001",
  "query": "wireless headphones",
  "details": {
    "abandonment_reason": "price_concern",
    "cart_value": 199.99,
    "time_in_cart": 45,
    "hover_count": 3,
    "view_duration": 30
  }
}
```

**Requirements:**
- Mix of event types showing discount-triggering behaviors
- Multiple users: user-001 through user-010
- Recent timestamps (last 7 days)
- Realistic search queries and abandonment reasons
- Include patterns that would trigger AI discount generation

### 3. Discount Templates Index (`sdg_discount_templates`)
Generate 10-15 discount templates with this schema:

```json
{
  "objectID": "TEMPLATE001",
  "type": "percentage|flat_amount|free_shipping|buy_one_get_one",
  "base_copy": "Still thinking about this? Get {discount}% off!",
  "triggers": {
    "hesitation_level": "high|medium|low",
    "user_segment": "new|returning|vip",
    "cart_value": "high|medium|low",
    "product_category": "Electronics"
  },
  "discount_range": {
    "min": 5,
    "max": 25
  },
  "urgency_copy": ["Limited time!", "Only today!", "While supplies last!"],
  "personalization_variables": ["user_name", "product_name", "savings_amount"]
}
```

**Requirements:**
- Various discount types and trigger conditions
- Compelling marketing copy templates
- Different urgency levels
- Personalization placeholders

## Output Format
Please provide the data in three separate JSON arrays, one for each index:

```json
{
  "products": [...],
  "user_events": [...],
  "discount_templates": [...]
}
```

## Additional Instructions
1. Make the data realistic and diverse
2. Ensure product IDs in user_events match products in the products index
3. Create behavior patterns that would realistically trigger discount offers
4. Include edge cases (high-value items, low-inventory products, etc.)
5. Use current timestamps for user events (within last 7 days)
6. Make search queries realistic and relevant to the products
7. Include both successful and unsuccessful user journeys

## Business Context
This is for an AI-powered discount system that:
- Analyzes user hesitation signals (cart abandonment, price hovering, multiple views)
- Generates personalized offers using Google Gemini AI
- Protects profit margins while maximizing conversions
- Provides real-time discount recommendations

Generate comprehensive, realistic data that would showcase the system's ability to detect user intent and generate appropriate discount offers.
