#!/bin/bash

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_color() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Function to generate branch naming policy
generate_branch_naming_policy() {
    local output_file="branch_naming_policy.md"
    
    print_color $BLUE "Generating branch naming policy..."
    
    cat > "$output_file" << 'EOF'
# Branch Naming Policy
EOF

    print_color $GREEN "Branch naming policy generated: $output_file"
}

# Test the function
generate_branch_naming_policy
