#!/bin/bash

# Test Chat Frontend Functionality
echo "🎭 Testing Frontend Chat Integration"
echo "=================================="

# Test the chat endpoint is accessible
echo "1️⃣ Testing Chat Endpoint Accessibility..."
curl -s -X POST "http://localhost:8080/api/mcp/chat/intelligent-assistant" \
  -H "Content-Type: application/json" \
  -d '{"message": "I need a discount on outdoor gear for camping", "user_id": "demo-user"}' | jq -r '.message, .suggestions[0].discount'

echo ""
echo "2️⃣ Testing with specific outdoor gear request..."
curl -s -X POST "http://localhost:8080/api/mcp/chat/intelligent-assistant" \
  -H "Content-Type: application/json" \
  -d '{"message": "Find me the best outdoor gear deals for hiking backpacks", "user_id": "demo-user"}' | jq -r '.message, .suggestions[]?.discount, .suggestions[]?.reasoning'

echo ""
echo "3️⃣ Testing general product query..."
curl -s -X POST "http://localhost:8080/api/mcp/chat/intelligent-assistant" \
  -H "Content-Type: application/json" \
  -d '{"message": "What discounts are available?", "user_id": "demo-user"}' | jq -r '.message, .confidence_score'

echo ""
echo "✅ Chat backend tests completed!"
echo "💡 To test frontend:"
echo "   1. Open: http://localhost:8080/algolia-mcp-showcase.html"
echo "   2. Click the blue chat button in bottom right"
echo "   3. Type: 'I need a discount on outdoor gear for camping'"
echo "   4. You should see AI response with discount suggestions"
