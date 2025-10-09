#!/bin/bash

# Reliable build script that handles protobuf generation correctly

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

print_color() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

print_color $BLUE "🔧 Building Basket Pricing Service..."

# Step 1: Clean (but preserve generated sources if possible)
print_color $BLUE "Step 1: Cleaning previous build..."
rm -rf target/classes target/test-classes target/*.jar target/surefire-reports 2>/dev/null || true

# Step 2: Generate protobuf sources
print_color $BLUE "Step 2: Generating protobuf sources..."
mvn protobuf:compile protobuf:compile-custom

# Step 3: Compile
print_color $BLUE "Step 3: Compiling Java sources..."
mvn compile -DskipTests

print_color $GREEN "✅ Build completed successfully!"
print_color $BLUE "To run the application: mvn spring-boot:run"

