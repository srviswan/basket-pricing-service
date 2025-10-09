#!/bin/bash

# Docker Status Script for Basket Pricing Service

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

print_color $CYAN "╔════════════════════════════════════════════════════════════╗"
print_color $CYAN "║        Basket Pricing Service - Docker Status              ║"
print_color $CYAN "╚════════════════════════════════════════════════════════════╝"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_color $RED "❌ Docker is not running"
    exit 1
fi

# Show container status
print_color $BLUE "📦 Container Status:"
docker-compose ps
echo ""

# Check if main service is running
if docker-compose ps | grep -q "basket-pricing-service.*Up"; then
    print_color $GREEN "✅ Basket Pricing Service is running"
    
    # Check health status
    print_color $BLUE "🏥 Health Check:"
    health_response=$(curl -s http://localhost:8080/actuator/health 2>/dev/null)
    
    if [ $? -eq 0 ]; then
        status=$(echo $health_response | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
        if [ "$status" = "UP" ]; then
            print_color $GREEN "   Status: $status ✅"
        else
            print_color $YELLOW "   Status: $status ⚠️"
        fi
        echo "   Response: $health_response"
    else
        print_color $RED "   ❌ Cannot connect to health endpoint"
    fi
    echo ""
    
    # Show service URLs
    print_color $CYAN "📍 Service URLs:"
    print_color $BLUE "   REST API:     http://localhost:8080"
    print_color $BLUE "   gRPC:         localhost:9090"
    print_color $BLUE "   Actuator:     http://localhost:8081/actuator"
    print_color $BLUE "   Health:       http://localhost:8080/actuator/health"
    print_color $BLUE "   Metrics:      http://localhost:8081/actuator/prometheus"
    echo ""
    
    print_color $CYAN "📊 Monitoring:"
    print_color $BLUE "   Grafana:      http://localhost:3000 (admin/admin)"
    print_color $BLUE "   Prometheus:   http://localhost:9091"
    echo ""
    
    # Quick metrics check
    print_color $BLUE "📈 Quick Metrics:"
    
    # JVM memory
    jvm_memory=$(curl -s http://localhost:8081/actuator/metrics/jvm.memory.used 2>/dev/null | grep -o '"value":[0-9.]*' | cut -d':' -f2)
    if [ -n "$jvm_memory" ]; then
        jvm_memory_mb=$(echo "scale=2; $jvm_memory / 1048576" | bc 2>/dev/null || echo "N/A")
        print_color $BLUE "   JVM Memory:   ${jvm_memory_mb} MB"
    fi
    
    # HTTP requests
    http_requests=$(curl -s http://localhost:8081/actuator/metrics/http.server.requests 2>/dev/null | grep -o '"count":[0-9]*' | cut -d':' -f2 | head -1)
    if [ -n "$http_requests" ]; then
        print_color $BLUE "   HTTP Requests: $http_requests"
    fi
    
    echo ""
else
    print_color $RED "❌ Basket Pricing Service is not running"
    echo ""
    print_color $YELLOW "To start services, run:"
    print_color $BLUE "   ./scripts/docker-start.sh"
fi

# Show volumes
print_color $BLUE "💾 Volumes:"
docker volume ls | grep basket-pricing
echo ""

# Show networks
print_color $BLUE "🌐 Networks:"
docker network ls | grep basket-pricing

