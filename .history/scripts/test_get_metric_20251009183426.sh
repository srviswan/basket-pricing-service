#!/bin/bash

# Test get_metric function

BASE_URL="http://localhost:8080"

get_metric() {
    local metric_name=$1
    local response=$(curl -s -f "$BASE_URL/actuator/metrics/$metric_name" 2>/dev/null)
    
    if [ $? -eq 0 ] && [ -n "$response" ]; then
        echo "$response" | jq -r '.measurements[0].value' 2>/dev/null || echo "N/A"
    else
        echo "N/A"
    fi
}

echo "Testing get_metric function..."
echo ""

# Check if service is running
echo "1. Checking if service is running..."
if curl -s -f "$BASE_URL/actuator/health" > /dev/null 2>&1; then
    echo "✅ Service is running"
else
    echo "❌ Service is NOT running"
    echo "Please start with: mvn spring-boot:run"
    exit 1
fi

# Test the function
echo ""
echo "2. Testing get_metric function..."
echo "   Getting pricing.subscriptions.active:"
result=$(get_metric "pricing.subscriptions.active")
echo "   Result: $result"

echo ""
echo "3. Testing with a metric that might not exist:"
result=$(get_metric "nonexistent.metric")
echo "   Result: $result"

echo ""
echo "4. Direct curl test:"
curl -s "$BASE_URL/actuator/metrics/pricing.subscriptions.active" | jq '.'

echo ""
echo "5. Testing jq parsing:"
curl -s "$BASE_URL/actuator/metrics/pricing.subscriptions.active" | jq -r '.measurements[0].value'

echo ""
echo "✅ Test complete!"

