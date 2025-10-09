#!/bin/bash

# Script to set up SSH authentication for Git

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
print_color $CYAN "║         Git SSH Authentication Setup Script                ║"
print_color $CYAN "╚════════════════════════════════════════════════════════════╝"
echo ""

# Step 1: Check if SSH key exists
print_color $BLUE "Step 1: Checking for existing SSH keys..."
if [ -f ~/.ssh/id_ed25519 ]; then
    print_color $GREEN "✅ Ed25519 SSH key found: ~/.ssh/id_ed25519"
    SSH_KEY_FILE=~/.ssh/id_ed25519.pub
elif [ -f ~/.ssh/id_rsa ]; then
    print_color $GREEN "✅ RSA SSH key found: ~/.ssh/id_rsa"
    SSH_KEY_FILE=~/.ssh/id_rsa.pub
else
    print_color $YELLOW "⚠️  No SSH key found. Generating new one..."
    
    # Get email for key
    email=$(git config user.email)
    if [ -z "$email" ]; then
        email="your-email@example.com"
    fi
    
    print_color $BLUE "Generating Ed25519 SSH key with email: $email"
    ssh-keygen -t ed25519 -C "$email" -f ~/.ssh/id_ed25519 -N ""
    
    if [ $? -eq 0 ]; then
        print_color $GREEN "✅ SSH key generated successfully"
        SSH_KEY_FILE=~/.ssh/id_ed25519.pub
    else
        print_color $RED "❌ Failed to generate SSH key"
        exit 1
    fi
fi

echo ""

# Step 2: Display public key
print_color $BLUE "Step 2: Your SSH Public Key:"
print_color $CYAN "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
cat $SSH_KEY_FILE
print_color $CYAN "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Step 3: Copy to clipboard (macOS)
if command -v pbcopy > /dev/null 2>&1; then
    cat $SSH_KEY_FILE | pbcopy
    print_color $GREEN "✅ Public key copied to clipboard!"
else
    print_color $YELLOW "⚠️  pbcopy not available. Please manually copy the key above."
fi

echo ""

# Step 4: Instructions to add to GitHub
print_color $BLUE "Step 3: Add SSH Key to GitHub:"
print_color $YELLOW "   1. Go to: https://github.com/settings/keys"
print_color $YELLOW "   2. Click 'New SSH key'"
print_color $YELLOW "   3. Title: 'MacBook' or any name you like"
print_color $YELLOW "   4. Paste the key (already in clipboard)"
print_color $YELLOW "   5. Click 'Add SSH key'"
echo ""

# Step 5: Wait for user to add key
print_color $YELLOW "Press Enter after you've added the key to GitHub..."
read -r

# Step 6: Test SSH connection
print_color $BLUE "Step 4: Testing SSH connection to GitHub..."
ssh_test=$(ssh -T git@github.com 2>&1)

if echo "$ssh_test" | grep -q "successfully authenticated"; then
    print_color $GREEN "✅ SSH authentication successful!"
    user=$(echo "$ssh_test" | grep -oP "Hi \K[^!]+")
    print_color $GREEN "   Authenticated as: $user"
else
    print_color $RED "❌ SSH authentication failed"
    print_color $YELLOW "   Response: $ssh_test"
    print_color $YELLOW "   Please verify you added the key correctly to GitHub"
    exit 1
fi

echo ""

# Step 7: Update Git remote to use SSH
print_color $BLUE "Step 5: Updating Git remote to use SSH..."
current_remote=$(git remote get-url origin)

# Extract repo path from HTTPS URL
repo_path=$(echo "$current_remote" | sed 's|https://github.com/||' | sed 's|\.git||')
new_remote="git@github.com:${repo_path}.git"

print_color $BLUE "   Current: $current_remote"
print_color $BLUE "   New:     $new_remote"

git remote set-url origin "$new_remote"

if [ $? -eq 0 ]; then
    print_color $GREEN "✅ Remote URL updated to SSH"
else
    print_color $RED "❌ Failed to update remote URL"
    exit 1
fi

echo ""

# Step 8: Verify and test push
print_color $BLUE "Step 6: Verifying configuration..."
print_color $BLUE "   Remote URL: $(git remote get-url origin)"

echo ""
print_color $GREEN "╔════════════════════════════════════════════════════════════╗"
print_color $GREEN "║              SSH Authentication Setup Complete!             ║"
print_color $GREEN "╚════════════════════════════════════════════════════════════╝"
echo ""

print_color $GREEN "✅ You can now push from command line:"
print_color $BLUE "   git push origin main"
echo ""

print_color $YELLOW "📝 Note: IntelliJ will automatically detect and use SSH as well"
echo ""

