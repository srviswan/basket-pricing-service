#!/bin/bash

# Docker Start Script for Basket Pricing Service (Minimal - No Redis/Kafka)

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
print_color $CYAN "║   Basket Pricing Service - Minimal Docker Start            ║"
print_color $CYAN "║   (Application + Monitoring only, no Redis/Kafka)          ║"
print_color $CYAN "╚════════════════════════════════════════════════════════════╝"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_color $RED "❌ Docker is not running. Please start Docker Desktop."
    exit 1
fi

print_color $GREEN "✅ Docker is running"
echo ""

# Parse command line arguments
BUILD=false
DETACHED=false
CLEAN=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --build|-b)
            BUILD=true
            shift
            ;;
        --detach|-d)
            DETACHED=true
            shift
            ;;
        --clean|-c)
            CLEAN=true
            shift
            ;;
        --help|-h)
            print_color $BLUE "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  -b, --build    Build images before starting"
            echo "  -d, --detach   Run in detached mode (background)"
            echo "  -c, --clean    Clean volumes and rebuild from scratch"
            echo "  -h, --help     Show this help message"
            echo ""
            echo "This starts a minimal stack:"
            echo "  - Basket Pricing Service"
            echo "  - Prometheus"
            echo "  - Grafana"
            echo ""
            echo "NO Redis or Kafka (not needed by current application)"
            exit 0
            ;;
        *)
            print_color $RED "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Clean if requested
if [ "$CLEAN" = true ]; then
    print_color $YELLOW "🧹 Cleaning up volumes and containers..."
    docker-compose -f docker-compose-minimal.yml down -v
    print_color $GREEN "✅ Cleanup complete"
    echo ""
    BUILD=true
fi

# Build if requested
if [ "$BUILD" = true ]; then
    print_color $BLUE "🔨 Building Docker images..."
    docker-compose -f docker-compose-minimal.yml build
    if [ $? -ne 0 ]; then
        print_color $RED "❌ Build failed"
        exit 1
    fi
    print_color $GREEN "✅ Build complete"
    echo ""
fi

# Start services
print_color $BLUE "🚀 Starting services..."
if [ "$DETACHED" = true ]; then
    docker-compose -f docker-compose-minimal.yml up -d
else
    docker-compose -f docker-compose-minimal.yml up
fi

if [ $? -eq 0 ] && [ "$DETACHED" = true ]; then
    echo ""
    print_color $GREEN "╔════════════════════════════════════════════════════════════╗"
    print_color $GREEN "║           Services Started Successfully!                   ║"
    print_color $GREEN "╚════════════════════════════════════════════════════════════╝"
    echo ""
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
    print_color $CYAN "📋 Useful Commands:"
    print_color $BLUE "   View logs:    docker-compose -f docker-compose-minimal.yml logs -f"
    print_color $BLUE "   Stop:         docker-compose -f docker-compose-minimal.yml stop"
    print_color $BLUE "   Restart:      docker-compose -f docker-compose-minimal.yml restart"
    print_color $BLUE "   Status:       docker-compose -f docker-compose-minimal.yml ps"
    echo ""
    print_color $GREEN "✨ Minimal stack running (no Redis/Kafka)"
    print_color $YELLOW "⏳ Note: Service may take 30-60 seconds to be fully ready"
fi

