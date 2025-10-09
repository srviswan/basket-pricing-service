#!/bin/bash

# Test all REST API endpoints

BASE_URL="http://localhost:8080"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m'

print_color() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

print_color $CYAN "╔════════════════════════════════════════════════════════════╗"
print_color $CYAN "║           Testing All REST API Endpoints                   ║"
print_color $CYAN "╚════════════════════════════════════════════════════════════╝"
echo ""

# Test 1: Health Check
print_color $BLUE "1. Testing Health Check:"
print_color $BLUE "   GET $BASE_URL/actuator/health"
response=$(curl -s -w "\n%{http_code}" "$BASE_URL/actuator/health")
http_code=$(echo "$response" | tail -n 1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    print_color $GREEN "   ✅ Status: $http_code"
    echo "$body" | jq '.status'
else
    print_color $RED "   ❌ Status: $http_code"
    echo "$body"
fi

# Test 2: Diagnostics
echo ""
print_color $BLUE "2. Testing Diagnostics Endpoint:"
print_color $BLUE "   GET $BASE_URL/api/prices/diagnostics"
response=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/prices/diagnostics")
http_code=$(echo "$response" | tail -n 1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    print_color $GREEN "   ✅ Status: $http_code"
    echo "$body" | jq '.'
else
    print_color $RED "   ❌ Status: $http_code"
    print_color $RED "   Response: $body"
    print_color $RED "   ERROR: Diagnostics endpoint not accessible!"
    echo ""
    print_color $BLUE "   Troubleshooting:"
    print_color $BLUE "   - Is the service running? curl http://localhost:8080/actuator/health"
    print_color $BLUE "   - Was the code compiled? mvn compile -DskipTests"
    print_color $BLUE "   - Check logs for errors"
fi

# Test 3: Get Subscriptions
echo ""
print_color $BLUE "3. Testing Get Subscriptions:"
print_color $BLUE "   GET $BASE_URL/api/prices/subscriptions"
response=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/prices/subscriptions")
http_code=$(echo "$response" | tail -n 1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    print_color $GREEN "   ✅ Status: $http_code"
    echo "$body" | jq '.'
else
    print_color $RED "   ❌ Status: $http_code"
fi

# Test 4: Subscribe
echo ""
print_color $BLUE "4. Testing Subscribe:"
print_color $BLUE "   POST $BASE_URL/api/prices/subscribe?symbols=IBM.N,MSFT.O"
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/prices/subscribe?symbols=IBM.N,MSFT.O")
http_code=$(echo "$response" | tail -n 1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    print_color $GREEN "   ✅ Status: $http_code"
    echo "$body" | jq '.'
else
    print_color $RED "   ❌ Status: $http_code"
fi

# Test 5: Get Prices
echo ""
print_color $BLUE "5. Testing Get Prices:"
print_color $BLUE "   GET $BASE_URL/api/prices?symbols=IBM.N,MSFT.O"
response=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/prices?symbols=IBM.N,MSFT.O")
http_code=$(echo "$response" | tail -n 1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    print_color $GREEN "   ✅ Status: $http_code"
    echo "$body" | jq '.'
else
    print_color $RED "   ❌ Status: $http_code"
fi

# Test 6: Unsubscribe
echo ""
print_color $BLUE "6. Testing Unsubscribe:"
print_color $BLUE "   DELETE $BASE_URL/api/prices/unsubscribe?symbols=IBM.N"
response=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL/api/prices/unsubscribe?symbols=IBM.N")
http_code=$(echo "$response" | tail -n 1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    print_color $GREEN "   ✅ Status: $http_code"
    echo "$body" | jq '.'
else
    print_color $RED "   ❌ Status: $http_code"
fi

echo ""
print_color $CYAN "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
print_color $GREEN "All endpoint tests completed!"
print_color $CYAN "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

