#!/bin/bash

# Docker Logs Script for Basket Pricing Service

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

# Default values
SERVICE="basket-pricing-service"
FOLLOW=false
TAIL="100"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --service|-s)
            SERVICE="$2"
            shift 2
            ;;
        --follow|-f)
            FOLLOW=true
            shift
            ;;
        --tail|-n)
            TAIL="$2"
            shift 2
            ;;
        --all|-a)
            SERVICE=""
            shift
            ;;
        --help|-h)
            print_color $BLUE "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  -s, --service <name>  Service name (default: basket-pricing-service)"
            echo "  -f, --follow          Follow log output"
            echo "  -n, --tail <number>   Number of lines to show (default: 100)"
            echo "  -a, --all             Show logs from all services"
            echo "  -h, --help            Show this help message"
            echo ""
            echo "Available services:"
            echo "  basket-pricing-service"
            echo "  redis"
            echo "  kafka"
            echo "  zookeeper"
            echo "  prometheus"
            echo "  grafana"
            echo ""
            echo "Examples:"
            echo "  $0                           # Show last 100 lines"
            echo "  $0 --follow                  # Follow logs"
            echo "  $0 --tail 500                # Show last 500 lines"
            echo "  $0 --service redis --follow  # Follow redis logs"
            echo "  $0 --all                     # Show all services logs"
            exit 0
            ;;
        *)
            print_color $RED "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Build docker-compose logs command
CMD="docker-compose logs"

if [ "$FOLLOW" = true ]; then
    CMD="$CMD -f"
fi

if [ -n "$TAIL" ]; then
    CMD="$CMD --tail=$TAIL"
fi

if [ -n "$SERVICE" ]; then
    CMD="$CMD $SERVICE"
fi

# Show what we're doing
if [ -n "$SERVICE" ]; then
    print_color $CYAN "📋 Showing logs for: $SERVICE"
else
    print_color $CYAN "📋 Showing logs for: all services"
fi

if [ "$FOLLOW" = true ]; then
    print_color $YELLOW "   (Following - Press Ctrl+C to stop)"
fi

echo ""

# Execute
eval $CMD

