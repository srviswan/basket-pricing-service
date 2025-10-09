#!/bin/bash

# Test Docker Service Script

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

print_color() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

test_passed=0
test_failed=0

run_test() {
    local test_name=$1
    local test_command=$2
    
    print_color $BLUE "▶ Testing: $test_name"
    
    if eval "$test_command" > /dev/null 2>&1; then
        print_color $GREEN "  ✅ PASS"
        ((test_passed++))
        return 0
    else
        print_color $RED "  ❌ FAIL"
        ((test_failed++))
        return 1
    fi
}

print_color $CYAN "╔════════════════════════════════════════════════════════════╗"
print_color $CYAN "║        Basket Pricing Service - Docker Tests               ║"
print_color $CYAN "╚════════════════════════════════════════════════════════════╝"
echo ""

# Test 1: Docker is running
run_test "Docker daemon is running" "docker info"

# Test 2: Service is running
run_test "Service container is running" "docker-compose ps | grep -q 'basket-pricing-service.*Up'"

# Test 3: Health check endpoint
print_color $BLUE "▶ Testing: Health check endpoint"
health_response=$(curl -s http://localhost:8080/actuator/health 2>/dev/null)
if echo "$health_response" | grep -q '"status":"UP"'; then
    print_color $GREEN "  ✅ PASS - Service is UP"
    ((test_passed++))
else
    print_color $RED "  ❌ FAIL - Service is not healthy"
    echo "  Response: $health_response"
    ((test_failed++))
fi

# Test 4: Actuator metrics endpoint
run_test "Actuator metrics endpoint" "curl -s http://localhost:8081/actuator/metrics | grep -q 'names'"

# Test 5: Prometheus metrics endpoint
run_test "Prometheus metrics endpoint" "curl -s http://localhost:8081/actuator/prometheus | grep -q 'jvm_'"

# Test 6: REST API - Get subscriptions
run_test "REST API - Get subscriptions" "curl -s http://localhost:8080/api/prices/subscriptions"

# Test 7: REST API - Subscribe to symbols
print_color $BLUE "▶ Testing: REST API - Subscribe to symbols"
subscribe_response=$(curl -s -X POST "http://localhost:8080/api/prices/subscribe?symbols=IBM.N,MSFT.O" 2>/dev/null)
if [ $? -eq 0 ]; then
    print_color $GREEN "  ✅ PASS"
    ((test_passed++))
else
    print_color $RED "  ❌ FAIL"
    ((test_failed++))
fi

# Wait a bit for prices to arrive
sleep 2

# Test 8: REST API - Get prices
print_color $BLUE "▶ Testing: REST API - Get prices"
prices_response=$(curl -s "http://localhost:8080/api/prices?symbols=IBM.N,MSFT.O" 2>/dev/null)
if [ $? -eq 0 ]; then
    print_color $GREEN "  ✅ PASS"
    echo "  Response: $prices_response"
    ((test_passed++))
else
    print_color $RED "  ❌ FAIL"
    ((test_failed++))
fi

# Test 9: Redis is running
run_test "Redis container is running" "docker-compose ps | grep -q 'basket-pricing-redis.*Up'"

# Test 10: Redis connectivity
run_test "Redis connectivity" "docker-compose exec -T redis redis-cli ping | grep -q 'PONG'"

# Test 11: Kafka is running
run_test "Kafka container is running" "docker-compose ps | grep -q 'basket-pricing-kafka.*Up'"

# Test 12: Prometheus is running
run_test "Prometheus container is running" "docker-compose ps | grep -q 'basket-pricing-prometheus.*Up'"

# Test 13: Prometheus UI
run_test "Prometheus UI accessible" "curl -s http://localhost:9091 | grep -q 'Prometheus'"

# Test 14: Grafana is running
run_test "Grafana container is running" "docker-compose ps | grep -q 'basket-pricing-grafana.*Up'"

# Test 15: Grafana UI
run_test "Grafana UI accessible" "curl -s http://localhost:3000 | grep -q 'Grafana'"

echo ""
print_color $CYAN "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
print_color $CYAN "                         Test Results"
print_color $CYAN "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

total_tests=$((test_passed + test_failed))
success_rate=$(awk "BEGIN {printf \"%.1f\", ($test_passed/$total_tests)*100}")

print_color $GREEN "Passed:  $test_passed"
print_color $RED   "Failed:  $test_failed"
print_color $BLUE  "Total:   $total_tests"
print_color $CYAN  "Success: ${success_rate}%"

echo ""

if [ $test_failed -eq 0 ]; then
    print_color $GREEN "╔════════════════════════════════════════════════════════════╗"
    print_color $GREEN "║              ALL TESTS PASSED! 🎉                          ║"
    print_color $GREEN "╚════════════════════════════════════════════════════════════╝"
    exit 0
else
    print_color $YELLOW "╔════════════════════════════════════════════════════════════╗"
    print_color $YELLOW "║              SOME TESTS FAILED ⚠️                          ║"
    print_color $YELLOW "╚════════════════════════════════════════════════════════════╝"
    echo ""
    print_color $YELLOW "Troubleshooting tips:"
    print_color $BLUE "  1. Check logs: ./scripts/docker-logs.sh --follow"
    print_color $BLUE "  2. Check status: ./scripts/docker-status.sh"
    print_color $BLUE "  3. Restart service: docker-compose restart basket-pricing-service"
    print_color $BLUE "  4. Wait longer: Service may still be starting up"
    exit 1
fi

