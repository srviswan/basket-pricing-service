#!/bin/bash

# Branch Protection Setup Script
# This script helps set up branch protection rules for trunk-based development

set -e

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

# Function to setup GitHub branch protection
setup_github_protection() {
    local repo=$1
    local main_branch=${2:-main}
    
    print_color $BLUE "Setting up GitHub branch protection for $repo..."
    
    # Note: This requires GitHub CLI (gh) to be installed and authenticated
    if command -v gh &> /dev/null; then
        gh api repos/$repo/branches/$main_branch/protection \
            --method PUT \
            --field required_status_checks='{"strict":true,"contexts":["ci/tests","ci/build"]}' \
            --field enforce_admins=true \
            --field required_pull_request_reviews='{"required_approving_review_count":2,"dismiss_stale_reviews":true}' \
            --field restrictions='{"users":[],"teams":[]}'
        
        print_color $GREEN "Branch protection enabled for $main_branch"
    else
        print_color $YELLOW "GitHub CLI not found. Please install 'gh' and authenticate to enable branch protection."
        print_color $YELLOW "Manual setup required:"
        print_color $YELLOW "1. Go to repository settings"
        print_color $YELLOW "2. Navigate to Branches"
        print_color $YELLOW "3. Add rule for $main_branch"
        print_color $YELLOW "4. Enable: Require pull request reviews, Require status checks, Restrict pushes"
    fi
}

# Function to setup GitLab branch protection
setup_gitlab_protection() {
    local repo=$1
    local main_branch=${2:-main}
    
    print_color $BLUE "Setting up GitLab branch protection for $repo..."
    
    # Note: This requires GitLab API access
    print_color $YELLOW "GitLab branch protection setup requires API access."
    print_color $YELLOW "Manual setup required:"
    print_color $YELLOW "1. Go to repository settings"
    print_color $YELLOW "2. Navigate to Repository > Protected branches"
    print_color $YELLOW "3. Add protection rule for $main_branch"
    print_color $YELLOW "4. Set allowed to push/merge to Maintainers"
    print_color $YELLOW "5. Enable: Require approval from code owners"
}

# Function to generate branch naming policy
generate_branch_naming_policy() {
    local output_file="branch_naming_policy.md"
    
    print_color $BLUE "Generating branch naming policy..."
    
    cat > "$output_file" << 'EOF'
# Branch Naming Policy

## Overview
This document defines the branch naming conventions for trunk-based development.

## Branch Types

### Feature Branches
- **Prefix**: `feature/` or `feat/`
- **Format**: `feature/description` or `feat/description`
- **Examples**:
  - `feature/user-authentication`
  - `feat/payment-integration`
  - `feature/add-search-functionality`

### Bug Fix Branches
- **Prefix**: `bugfix/` or `fix/`
- **Format**: `bugfix/description` or `fix/description`
- **Examples**:
  - `bugfix/login-validation-error`
  - `fix/memory-leak-in-cache`
  - `bugfix/race-condition-in-api`

### Hotfix Branches
- **Prefix**: `hotfix/`
- **Format**: `hotfix/description`
- **Examples**:
  - `hotfix/security-vulnerability`
  - `hotfix/critical-production-bug`
  - `hotfix/urgent-performance-issue`

### Release Branches
- **Prefix**: `release/`
- **Format**: `release/version`
- **Examples**:
  - `release/1.0.0`
  - `release/2.1.0`
  - `release/1.2.3`

## Rules

1. **Use lowercase letters and hyphens**
2. **Be descriptive but concise**
3. **Include issue/ticket number if applicable**
4. **Keep branch names under 50 characters**
5. **Avoid special characters except hyphens**

## Examples of Good Branch Names

```
feature/user-profile-management
feat/oauth-integration
bugfix/validation-error-handling
fix/memory-optimization
hotfix/security-patch
release/1.0.0
```

## Examples of Bad Branch Names

```
Feature_User_Profile  # Mixed case and underscores
new-feature          # Too generic
fix                  # Too short
very-long-descriptive-branch-name-that-exceeds-recommended-length  # Too long
feature/user@profile  # Special characters
```

## Enforcement

Branch naming conventions can be enforced using:
- Git hooks
- CI/CD pipeline checks
- Repository settings
- Code review requirements
