#!/bin/bash

# Build Docker image using pre-built local JAR
# This is FASTER for development (2-4x speedup)

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
print_color $CYAN "║     Build Docker Image from Local JAR (Fast Build)        ║"
print_color $CYAN "╚════════════════════════════════════════════════════════════╝"
echo ""

# Step 1: Build JAR locally
print_color $BLUE "🔨 Step 1: Building JAR with Maven..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    print_color $RED "❌ Maven build failed"
    exit 1
fi

print_color $GREEN "✅ JAR built successfully"
echo ""

# Verify JAR exists
JAR_FILE=$(ls target/*.jar 2>/dev/null | head -1)
if [ -z "$JAR_FILE" ]; then
    print_color $RED "❌ No JAR file found in target/"
    exit 1
fi

JAR_SIZE=$(ls -lh "$JAR_FILE" | awk '{print $5}')
print_color $BLUE "   JAR file: $JAR_FILE ($JAR_SIZE)"
echo ""

# Step 2: Temporarily use local .dockerignore
print_color $BLUE "🔧 Step 2: Configuring Docker build context..."

# Backup current .dockerignore
if [ -f .dockerignore ]; then
    cp .dockerignore .dockerignore.backup
fi

# Use local version that allows target/
if [ -f .dockerignore.local ]; then
    cp .dockerignore.local .dockerignore
    print_color $GREEN "✅ Using .dockerignore.local (allows target/ directory)"
else
    print_color $YELLOW "⚠️  .dockerignore.local not found, using current .dockerignore"
fi
echo ""

# Step 3: Build Docker image
print_color $BLUE "🐳 Step 3: Building Docker image..."
docker build -f Dockerfile.local -t basket-pricing-service:dev .

if [ $? -ne 0 ]; then
    print_color $RED "❌ Docker build failed"
    
    # Restore .dockerignore
    if [ -f .dockerignore.backup ]; then
        mv .dockerignore.backup .dockerignore
    fi
    
    exit 1
fi

print_color $GREEN "✅ Docker image built successfully"
echo ""

# Restore .dockerignore
if [ -f .dockerignore.backup ]; then
    mv .dockerignore.backup .dockerignore
fi

# Step 4: Show image details
print_color $BLUE "📦 Step 4: Image details"
IMAGE_SIZE=$(docker images basket-pricing-service:dev --format "{{.Size}}" | head -1)
print_color $BLUE "   Image: basket-pricing-service:dev"
print_color $BLUE "   Size: $IMAGE_SIZE"
echo ""

print_color $GREEN "╔════════════════════════════════════════════════════════════╗"
print_color $GREEN "║              Build Complete! 🎉                            ║"
print_color $GREEN "╚════════════════════════════════════════════════════════════╝"
echo ""

print_color $CYAN "🚀 Next steps:"
print_color $BLUE "   Test locally:  docker run --rm -p 8080:8080 basket-pricing-service:dev"
print_color $BLUE "   With compose:  docker-compose -f docker-compose-local.yml up"
print_color $BLUE "   Check image:   docker images | grep basket-pricing"
echo ""

print_color $YELLOW "💡 Tip: This method is 2-4x faster than multi-stage build for development!"

