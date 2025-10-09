#!/bin/bash

# Docker Stop Script for Basket Pricing Service

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
print_color $CYAN "║        Basket Pricing Service - Docker Stop                ║"
print_color $CYAN "╚════════════════════════════════════════════════════════════╝"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_color $RED "❌ Docker is not running"
    exit 1
fi

# Parse command line arguments
REMOVE_VOLUMES=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --volumes|-v)
            REMOVE_VOLUMES=true
            shift
            ;;
        --help|-h)
            print_color $BLUE "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  -v, --volumes  Remove volumes (deletes data)"
            echo "  -h, --help     Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0              # Stop services (keep data)"
            echo "  $0 --volumes    # Stop and remove all data"
            exit 0
            ;;
        *)
            print_color $RED "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

print_color $BLUE "🛑 Stopping services..."

if [ "$REMOVE_VOLUMES" = true ]; then
    print_color $YELLOW "⚠️  This will remove all volumes and data!"
    docker-compose down -v
else
    docker-compose down
fi

if [ $? -eq 0 ]; then
    print_color $GREEN "✅ Services stopped successfully"
    
    if [ "$REMOVE_VOLUMES" = true ]; then
        print_color $YELLOW "⚠️  All volumes and data removed"
    fi
else
    print_color $RED "❌ Failed to stop services"
    exit 1
fi

