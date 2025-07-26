# Smart Discount Generator (SDG)

An AI-powered real-time discount engine that uses Algolia's MCP Server to analyze user behavior and generate personalized discount offers to boost e-commerce conversions.

## 🚀 Features

- **Real-time Behavior Analysis**: Tracks user interactions (cart abandonment, price hesitation, multiple product views)
- **AI-Powered Discount Generation**: Uses Google Gemini Pro API for intelligent discount optimization
- **MCP Server Implementation**: Compliant with Algolia's Model Context Protocol for seamless integration
- **Dynamic Frontend Interface**: Interactive e-commerce simulation with real-time discount offers
- **Profit Margin Protection**: Ensures discounts don't hurt business profitability
- **Personalized Messaging**: AI-generated discount copy tailored to user behavior

## 🏗️ Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Frontend UI   │────│  Spring Boot API │────│  Algolia Search │
│  (HTML/CSS/JS)  │    │   (MCP Server)   │    │   (Data Store)  │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │
                                │
                       ┌──────────────────┐
                       │   Gemini Pro AI  │
                       │ (Discount Logic) │
                       └──────────────────┘
```

## 🛠️ Technology Stack

### Backend
- **Spring Boot 3.5.4** - Main application framework
- **Algolia Java Client 4.2.0** - Search and analytics
- **Spring WebFlux** - HTTP client for Gemini API
- **Jackson** - JSON processing
- **Lombok** - Code generation

### Frontend
- **Vanilla JavaScript** - Interactive UI logic
- **CSS3** - Modern responsive design
- **Font Awesome** - Icons and visual elements

### AI & Data
- **Google Gemini Pro** - AI discount generation
- **Algolia Search** - User behavior storage and product data
- **MCP Protocol** - Model Context Protocol compliance

## 📋 Prerequisites

1. **Java 17+** installed
2. **Maven 3.6+** for dependency management
3. **Algolia Account** - [Sign up here](https://www.algolia.com/)
4. **Google AI Studio Account** - [Get API key here](https://makersuite.google.com/)

## 🚀 Quick Start

### 1. Clone and Setup

```bash
git clone <repository-url>
cd sdg
```

### 2. Configure Environment Variables

Copy the environment template:
```bash
cp .env.template .env
```

Edit `.env` with your API keys:
```bash
ALGOLIA_APPLICATION_ID=your_algolia_app_id
ALGOLIA_ADMIN_API_KEY=your_algolia_admin_key
ALGOLIA_SEARCH_API_KEY=your_algolia_search_key
GEMINI_API_KEY=your_gemini_api_key
```

### 3. Run the Application

```bash
# Using Maven
./mvnw spring-boot:run

# Or using Java directly
./mvnw clean package
java -jar target/sdg-0.0.1-SNAPSHOT.jar
```

### 4. Access the Application

Open your browser and navigate to:
```
http://localhost:8080
```

## 🎯 How It Works

### 1. User Behavior Tracking
The system tracks various user behaviors:
- **Cart Abandonment**: User adds items but doesn't complete purchase
- **Price Hesitation**: Multiple hovers over product price
- **Multiple Product Views**: Browsing through several similar products
- **Search Frustration**: Searches returning no results

### 2. AI Analysis
When discount criteria are met:
1. User behavior history is retrieved from Algolia
2. Product information and profit margins are analyzed
3. Gemini AI generates personalized discount suggestions
4. System validates discount against business rules

### 3. Dynamic Offer Display
- Real-time discount banners appear based on behavior
- Personalized messaging and discount amounts
- Countdown timers create urgency
- Seamless application to product pricing

## 🔧 API Endpoints

### Frontend APIs
- `POST /api/user-behavior` - Track user behavior events
- `GET /api/get-discount?userId={id}` - Generate personalized discount
- `POST /api/validate-discount` - Validate discount codes
- `POST /api/apply-discount` - Apply discount to purchase

### MCP Server APIs
- `POST /mcp` - Main MCP protocol endpoint
  - `initialize` - Server initialization
  - `tools/list` - Available tools listing
  - `tools/call` - Tool execution

### Available MCP Tools
- `algolia.analytics.getBehavioralSignals` - Retrieve user behavior data
- `product.getDetails` - Get product information
- `discount.generate` - Generate personalized discount
- `discount.validate` - Validate discount codes

## 📊 Data Models

### Product Schema
```json
{
  "objectID": "PROD001",
  "name": "Wireless Bluetooth Headphones",
  "price": 199.99,
  "category": "Electronics",
  "profit_margin": 0.35,
  "inventory_level": 50,
  "average_rating": 4.5,
  "number_of_reviews": 1250
}
```

### User Event Schema
```json
{
  "objectID": "event-uuid",
  "userId": "user-123",
  "eventType": "cart_abandon",
  "timestamp": "2025-01-26T13:30:00Z",
  "productId": "PROD001",
  "details": {
    "abandonment_reason": "price_concern",
    "cart_value": 199.99
  }
}
```

### Discount Schema
```json
{
  "code": "SAVE15-ABC",
  "userId": "user-123",
  "type": "percentage",
  "amount": "15% off",
  "value": 15.0,
  "headline": "Still Thinking About This?",
  "message": "Get 15% off your entire cart!",
  "expiresInSeconds": 1800
}
```

## 🎮 Demo Usage

### Simulate User Behaviors
1. **Cart Abandonment**: Click "Add to Cart" then simulate abandonment
2. **Price Hesitation**: Hover over the product price multiple times
3. **Multiple Views**: Click "Multiple Product Views" simulation
4. **Search Issues**: Simulate searches with no results

### Generate AI Discounts
1. Perform behavior simulations to build user history
2. Click "Generate AI Discount" to trigger analysis
3. Watch as personalized offers appear in real-time
4. Apply discounts and see price updates

## 🔒 Security Considerations

- API keys stored in environment variables
- CORS enabled for frontend integration
- Input validation on all API endpoints
- Discount validation prevents unauthorized usage

## 🚀 Production Deployment

### Environment Variables
Set the following in your production environment:
```bash
ALGOLIA_APPLICATION_ID=prod_app_id
ALGOLIA_ADMIN_API_KEY=prod_admin_key
ALGOLIA_SEARCH_API_KEY=prod_search_key
GEMINI_API_KEY=prod_gemini_key
```

### Recommended Enhancements
- Replace in-memory discount storage with Redis
- Add user authentication and session management
- Implement rate limiting for API endpoints
- Add comprehensive logging and monitoring
- Set up automated testing pipeline

## 🧪 Testing

### Manual Testing
1. Start the application
2. Open browser to `http://localhost:8080`
3. Use the simulation buttons to trigger behaviors
4. Verify discount generation and application

### API Testing
Use tools like Postman or curl to test endpoints:
```bash
# Track behavior
curl -X POST http://localhost:8080/api/user-behavior \
  -H "Content-Type: application/json" \
  -d '{"userId":"test-user","eventType":"cart_abandon","productId":"PROD001"}'

# Generate discount
curl "http://localhost:8080/api/get-discount?userId=test-user"
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For questions or issues:
1. Check the troubleshooting section below
2. Review the API documentation
3. Open an issue on GitHub

## 🔧 Troubleshooting

### Common Issues

**Application won't start**
- Verify Java 17+ is installed
- Check that ports 8080 is available
- Ensure environment variables are set correctly

**Algolia connection errors**
- Verify API keys are correct
- Check network connectivity
- Ensure Algolia application is active

**Gemini API errors**
- Verify API key is valid and active
- Check API quota and billing
- Review request format in logs

**Frontend not loading**
- Clear browser cache
- Check browser console for errors
- Verify static resources are served correctly

### Debug Mode
Enable debug logging by adding to `application.yml`:
```yaml
logging:
  level:
    com.dev.challenge.sdg: DEBUG
```

## 🎯 Future Enhancements

- **n8n Integration**: Automated workflow for discount performance tracking
- **A/B Testing**: Compare different discount strategies
- **Machine Learning**: Advanced behavior prediction models
- **Real-time Analytics**: Dashboard for discount performance metrics
- **Multi-tenant Support**: Support for multiple e-commerce stores
