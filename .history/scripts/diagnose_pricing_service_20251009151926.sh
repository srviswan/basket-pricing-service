#!/bin/bash

# Diagnostic Script for Basket Pricing Service
# This script helps diagnose why the snapshotByRic map is empty

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

BASE_URL="http://localhost:8080"

print_color() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

print_section() {
    echo ""
    print_color $BLUE "=========================================="
    print_color $BLUE "$1"
    print_color $BLUE "=========================================="
}

# Function to check if service is running
check_service_running() {
    print_section "1. Checking if service is running"
    
    if curl -s -f "$BASE_URL/actuator/health" > /dev/null 2>&1; then
        print_color $GREEN "✅ Service is running"
        curl -s "$BASE_URL/actuator/health" | jq '.'
    else
        print_color $RED "❌ Service is not running or not responding"
        print_color $YELLOW "Please start the service with: mvn spring-boot:run"
        exit 1
    fi
}

# Function to check diagnostics endpoint
check_diagnostics() {
    print_section "2. Checking Service Diagnostics"
    
    response=$(curl -s "$BASE_URL/api/prices/diagnostics")
    echo "$response" | jq '.'
    
    provider_class=$(echo "$response" | jq -r '.marketDataProviderClass')
    is_resilient=$(echo "$response" | jq -r '.isResilientProvider')
    subscription_count=$(echo "$response" | jq -r '.subscriptionCount')
    available_prices=$(echo "$response" | jq -r '.availablePrices')
    
    print_color $BLUE "Provider Class: $provider_class"
    
    if [ "$is_resilient" = "true" ]; then
        print_color $GREEN "✅ Using ResilientMarketDataProvider (CORRECT)"
    else
        print_color $RED "❌ NOT using ResilientMarketDataProvider (INCORRECT)"
    fi
    
    print_color $BLUE "Subscriptions: $subscription_count"
    print_color $BLUE "Available Prices: $available_prices"
}

# Function to check current subscriptions
check_subscriptions() {
    print_section "3. Checking Current Subscriptions"
    
    response=$(curl -s "$BASE_URL/api/prices/subscriptions")
    echo "$response" | jq '.'
    
    count=$(echo "$response" | jq -r '.count')
    
    if [ "$count" -eq 0 ]; then
        print_color $YELLOW "⚠️  No subscriptions found"
        print_color $YELLOW "This is likely why snapshotByRic is empty!"
        return 1
    else
        print_color $GREEN "✅ Found $count subscriptions"
        return 0
    fi
}

# Function to subscribe to test symbols
subscribe_test_symbols() {
    print_section "4. Subscribing to Test Symbols"
    
    symbols="IBM.N,MSFT.O,AAPL.O"
    print_color $BLUE "Subscribing to: $symbols"
    
    response=$(curl -s -X POST "$BASE_URL/api/prices/subscribe?symbols=$symbols")
    echo "$response" | jq '.'
    
    success=$(echo "$response" | jq -r '.subscribed | length')
    
    if [ "$success" -gt 0 ]; then
        print_color $GREEN "✅ Successfully subscribed to $success symbols"
    else
        print_color $RED "❌ Subscription failed"
        return 1
    fi
}

# Function to wait for price updates
wait_for_updates() {
    print_section "5. Waiting for Price Updates"
    
    print_color $YELLOW "Waiting 10 seconds for Refinitiv to send price updates..."
    
    for i in {10..1}; do
        echo -n "$i... "
        sleep 1
    done
    echo ""
    
    print_color $GREEN "✅ Wait complete"
}

# Function to check for price updates
check_price_updates() {
    print_section "6. Checking for Price Updates"
    
    symbols="IBM.N,MSFT.O,AAPL.O"
    print_color $BLUE "Requesting prices for: $symbols"
    
    response=$(curl -s "$BASE_URL/api/prices?symbols=$symbols")
    echo "$response" | jq '.'
    
    price_count=$(echo "$response" | jq '. | length')
    
    if [ "$price_count" -gt 0 ]; then
        print_color $GREEN "✅ Received $price_count price snapshots"
        print_color $GREEN "snapshotByRic map is populated!"
    else
        print_color $RED "❌ No price snapshots received"
        print_color $RED "snapshotByRic map is still empty!"
        return 1
    fi
}

# Function to check metrics
check_metrics() {
    print_section "7. Checking Service Metrics"
    
    print_color $BLUE "Active Subscriptions:"
    curl -s "$BASE_URL/actuator/metrics/pricing.subscriptions.active" | jq '.measurements[0].value'
    
    print_color $BLUE "Price Updates Count:"
    curl -s "$BASE_URL/actuator/metrics/pricing.updates.count" | jq '.measurements[0].value'
    
    print_color $BLUE "Connection Status:"
    curl -s "$BASE_URL/actuator/metrics/pricing.connection.status" | jq '.measurements[0].value'
    
    print_color $BLUE "Connection Errors:"
    curl -s "$BASE_URL/actuator/metrics/pricing.connection.errors" | jq '.measurements[0].value' 2>/dev/null || echo "0"
    
    print_color $BLUE "Backpressure Utilization:"
    curl -s "$BASE_URL/actuator/metrics/pricing.backpressure.utilization" | jq '.measurements[0].value'
    
    print_color $BLUE "Backpressure Processed:"
    curl -s "$BASE_URL/actuator/metrics/pricing.backpressure.processed" | jq '.measurements[0].value'
    
    print_color $BLUE "Backpressure Dropped:"
    curl -s "$BASE_URL/actuator/metrics/pricing.backpressure.dropped" | jq '.measurements[0].value'
}

# Function to analyze logs
analyze_logs() {
    print_section "8. Analyzing Application Logs"
    
    if [ -f "logs/application.log" ]; then
        print_color $BLUE "Checking for EMA startup messages:"
        grep "Starting Refinitiv EMA provider\|EMA provider started successfully\|Failed to start Refinitiv" logs/application.log | tail -5
        
        print_color $BLUE "Checking for subscription messages:"
        grep "RefinitivEmaProvider.subscribe()\|Successfully subscribed" logs/application.log | tail -10
        
        print_color $BLUE "Checking for price update messages:"
        grep "Received RefreshMsg\|Received UpdateMsg\|Stored price snapshot" logs/application.log | tail -10
        
        print_color $BLUE "Checking for errors:"
        grep "ERROR\|Failed\|Cannot" logs/application.log | tail -10
    else
        print_color $YELLOW "⚠️  No log file found at logs/application.log"
        print_color $YELLOW "Check console output or configure file logging"
    fi
}

# Function to provide recommendations
provide_recommendations() {
    print_section "9. Recommendations"
    
    # Check if we have subscriptions
    sub_count=$(curl -s "$BASE_URL/api/prices/subscriptions" | jq -r '.count')
    
    if [ "$sub_count" -eq 0 ]; then
        print_color $YELLOW "⚠️  Issue: No subscriptions found"
        print_color $YELLOW "Solution: Subscribe to symbols before requesting prices"
        print_color $BLUE "Example: curl -X POST \"$BASE_URL/api/prices/subscribe?symbols=IBM.N,MSFT.O\""
        return
    fi
    
    # Check if we have prices
    price_count=$(curl -s "$BASE_URL/api/prices?symbols=IBM.N,MSFT.O,AAPL.O" | jq '. | length')
    
    if [ "$price_count" -eq 0 ]; then
        print_color $YELLOW "⚠️  Issue: Subscriptions exist but no prices available"
        print_color $YELLOW "Possible causes:"
        print_color $YELLOW "  1. Refinitiv connection not established"
        print_color $YELLOW "  2. No price updates received yet (market closed?)"
        print_color $YELLOW "  3. EMA dispatcher thread not running"
        print_color $YELLOW "  4. Backpressure queue not processing"
        print_color $BLUE "Check logs for: 'Received RefreshMsg' or 'Received UpdateMsg'"
        print_color $BLUE "Check metrics: curl $BASE_URL/actuator/metrics/pricing.updates.count"
    else
        print_color $GREEN "✅ Everything is working correctly!"
        print_color $GREEN "Prices are being received and cached"
    fi
}

# Main execution
main() {
    print_color $PURPLE "╔════════════════════════════════════════╗"
    print_color $PURPLE "║  Basket Pricing Service Diagnostics   ║"
    print_color $PURPLE "╚════════════════════════════════════════╝"
    
    check_service_running
    check_diagnostics
    
    if ! check_subscriptions; then
        print_color $YELLOW "No subscriptions found. Creating test subscriptions..."
        subscribe_test_symbols
        wait_for_updates
        check_price_updates || true
    else
        check_price_updates || true
    fi
    
    check_metrics
    analyze_logs
    provide_recommendations
    
    print_section "Diagnostics Complete"
    print_color $GREEN "Review the output above to identify the issue"
}

# Run main function
main

