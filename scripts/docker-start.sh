#!/bin/bash

# Docker Start Script for Basket Pricing Service

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
print_color $CYAN "║        Basket Pricing Service - Docker Start               ║"
print_color $CYAN "╚════════════════════════════════════════════════════════════╝"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_color $RED "❌ Docker is not running. Please start Docker Desktop."
    exit 1
fi

print_color $GREEN "✅ Docker is running"
echo ""

# Check if .env file exists
if [ ! -f .env ]; then
    print_color $YELLOW "⚠️  No .env file found. Creating from env.example..."
    if [ -f env.example ]; then
        cp env.example .env
        print_color $GREEN "✅ Created .env file. Please update it with your credentials."
        print_color $YELLOW "   Edit .env file to set your Refinitiv credentials."
        echo ""
    else
        print_color $RED "❌ env.example not found. Please create .env file manually."
        exit 1
    fi
fi

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
            echo "Examples:"
            echo "  $0              # Start services"
            echo "  $0 --build      # Build and start"
            echo "  $0 --detach     # Start in background"
            echo "  $0 --clean      # Clean and rebuild"
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
    docker-compose down -v
    print_color $GREEN "✅ Cleanup complete"
    echo ""
    BUILD=true
fi

# Build if requested
if [ "$BUILD" = true ]; then
    print_color $BLUE "🔨 Building Docker images..."
    docker-compose build
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
    docker-compose up -d
else
    docker-compose up
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
    print_color $CYAN "💾 Data Stores:"
    print_color $BLUE "   Redis:        localhost:6379"
    print_color $BLUE "   Kafka:        localhost:29092"
    echo ""
    print_color $CYAN "📋 Useful Commands:"
    print_color $BLUE "   View logs:    docker-compose logs -f basket-pricing-service"
    print_color $BLUE "   Stop:         docker-compose stop"
    print_color $BLUE "   Restart:      docker-compose restart basket-pricing-service"
    print_color $BLUE "   Status:       docker-compose ps"
    print_color $BLUE "   Shell:        docker-compose exec basket-pricing-service sh"
    echo ""
    print_color $YELLOW "⏳ Note: Service may take 30-60 seconds to be fully ready"
    print_color $YELLOW "   Check health: curl http://localhost:8080/actuator/health"
fi

