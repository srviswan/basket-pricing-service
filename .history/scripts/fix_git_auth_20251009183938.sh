#!/bin/bash

# Script to fix Git authentication issues

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
print_color $CYAN "║           Git Authentication Fix Script                    ║"
print_color $CYAN "╚════════════════════════════════════════════════════════════╝"
echo ""

# Check current configuration
print_color $BLUE "1. Current Git Configuration:"
echo "   Remote URL: $(git remote get-url origin)"
echo "   User Name: $(git config user.name)"
echo "   User Email: $(git config user.email)"
echo "   Credential Helper: $(git config credential.helper)"
echo ""

# Check if we're using HTTPS or SSH
remote_url=$(git remote get-url origin)
if [[ "$remote_url" == https://* ]]; then
    print_color $YELLOW "📌 Using HTTPS authentication"
    echo ""
    
    print_color $BLUE "2. Clearing cached credentials..."
    # Clear GitHub credentials from osxkeychain
    printf "host=github.com\nprotocol=https\n\n" | git credential-osxkeychain erase 2>/dev/null || true
    print_color $GREEN "   ✅ Credentials cleared"
    echo ""
    
    print_color $BLUE "3. Next steps:"
    print_color $YELLOW "   Option A: Use IntelliJ to push (easiest)"
    print_color $BLUE "      - In IntelliJ: Cmd+Shift+K (Mac) or Ctrl+Shift+K (Windows)"
    print_color $BLUE "      - Click 'Push'"
    echo ""
    
    print_color $YELLOW "   Option B: Get a personal access token and use it from command line"
    print_color $BLUE "      1. Go to: https://github.com/settings/tokens"
    print_color $BLUE "      2. Generate new token (classic)"
    print_color $BLUE "      3. Select 'repo' scope"
    print_color $BLUE "      4. Copy the token"
    print_color $BLUE "      5. Run: git push origin main"
    print_color $BLUE "         Username: srviswan"
    print_color $BLUE "         Password: <paste your token>"
    echo ""
    
    print_color $YELLOW "   Option C: Switch to SSH (recommended for long-term)"
    print_color $BLUE "      Run: ./scripts/setup_ssh_auth.sh"
    echo ""
    
elif [[ "$remote_url" == git@* ]]; then
    print_color $YELLOW "📌 Using SSH authentication"
    echo ""
    
    print_color $BLUE "2. Checking SSH key..."
    if [ -f ~/.ssh/id_ed25519 ] || [ -f ~/.ssh/id_rsa ]; then
        print_color $GREEN "   ✅ SSH key exists"
        
        print_color $BLUE "3. Testing GitHub SSH connection..."
        if ssh -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
            print_color $GREEN "   ✅ SSH authentication working"
            print_color $BLUE "   You should be able to push now:"
            print_color $BLUE "      git push origin main"
        else
            print_color $RED "   ❌ SSH authentication failed"
            print_color $YELLOW "   Your SSH key might not be added to GitHub"
            print_color $BLUE "   Add your key at: https://github.com/settings/keys"
            echo ""
            print_color $BLUE "   Your public key:"
            cat ~/.ssh/id_ed25519.pub 2>/dev/null || cat ~/.ssh/id_rsa.pub 2>/dev/null
        fi
    else
        print_color $YELLOW "   ⚠️  No SSH key found"
        print_color $BLUE "   Generate one with:"
        print_color $BLUE "      ssh-keygen -t ed25519 -C \"your-email@example.com\""
    fi
else
    print_color $YELLOW "⚠️  Unknown remote URL format: $remote_url"
fi

echo ""
print_color $CYAN "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
print_color $GREEN "For more details, see: GIT_AUTHENTICATION_GUIDE.md"
print_color $CYAN "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

