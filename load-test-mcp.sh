#!/bin/bash

# 🏃‍♂️ MCP Performance Load Testing
echo "🚀 MCP Performance Load Testing"
echo "================================"

BASE_URL="http://localhost:8080"
USERS=("user123" "user456" "user789" "user001" "user002")
PRODUCTS=("PROD001" "PROD018" "PROD021" "PROD002" "PROD003")

echo "Testing concurrent MCP requests..."

# Function to test MCP discount generation
test_mcp_discount() {
    local user=${USERS[$RANDOM % ${#USERS[@]}]}
    local product=${PRODUCTS[$RANDOM % ${#PRODUCTS[@]}]}
    local discount=$((RANDOM % 20 + 5))
    
    start_time=$(python3 -c "import time; print(int(time.time() * 1000))")
    response=$(curl -s -X GET "$BASE_URL/api/mcp/intelligent-discount?userId=$user&productId=$product&requestedDiscount=$discount.0" \
      -H "Content-Type: application/json")
    end_time=$(python3 -c "import time; print(int(time.time() * 1000))")
    
    duration=$((end_time - start_time))
    confidence=$(echo $response | jq -r '.discount.confidence_score // 0')
    
    echo "User: $user, Product: $product, Discount: $discount%, Time: ${duration}ms, Confidence: $confidence"
}

# Run 10 concurrent tests
echo "Running 10 concurrent MCP discount tests..."
for i in {1..10}; do
    test_mcp_discount &
done

# Wait for all background jobs to complete
wait

echo ""
echo "✅ Load testing completed!"
