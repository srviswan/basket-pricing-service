#!/bin/bash

# Quick Repository Analysis Script
# A simplified version for quick analysis and recommendations

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

# Function to analyze repository quickly
quick_analyze() {
    local repo_path=$1
    
    print_color $BLUE "🔍 Quick Repository Analysis"
    print_color $BLUE "Repository: $repo_path"
    echo ""
    
    # Check if it's a Git repository
    if [ ! -d "$repo_path/.git" ]; then
        print_color $RED "❌ Not a Git repository"
        return 1
    fi
    
    # Basic Git information
    cd "$repo_path"
    
    # Get basic stats
    local total_commits=$(git rev-list --count HEAD 2>/dev/null || echo "0")
    local total_branches=$(git branch -a | wc -l)
    local main_branch=$(git symbolic-ref refs/remotes/origin/HEAD 2>/dev/null | sed 's@^refs/remotes/origin/@@' || echo "main")
    
    print_color $GREEN "📊 Basic Statistics"
    echo "  Total Commits: $total_commits"
    echo "  Total Branches: $total_branches"
    echo "  Main Branch: $main_branch"
    echo ""
    
    # Check for CI/CD files
    print_color $GREEN "🚀 CI/CD Infrastructure"
    local cicd_score=0
    local cicd_files=()
    
    if [ -d ".github/workflows" ]; then
        cicd_score=$((cicd_score + 30))
        cicd_files+=("GitHub Actions")
    fi
    
    if [ -f ".gitlab-ci.yml" ]; then
        cicd_score=$((cicd_score + 30))
        cicd_files+=("GitLab CI")
    fi
    
    if [ -f "Jenkinsfile" ]; then
        cicd_score=$((cicd_score + 30))
        cicd_files+=("Jenkins")
    fi
    
    if [ -f "azure-pipelines.yml" ]; then
        cicd_score=$((cicd_score + 20))
        cicd_files+=("Azure Pipelines")
    fi
    
    if [ -f ".travis.yml" ]; then
        cicd_score=$((cicd_score + 20))
        cicd_files+=("Travis CI")
    fi
    
    if [ -f "circle.yml" ]; then
        cicd_score=$((cicd_score + 20))
        cicd_files+=("CircleCI")
    fi
    
    if [ ${#cicd_files[@]} -eq 0 ]; then
        print_color $YELLOW "  ❌ No CI/CD infrastructure detected"
    else
        print_color $GREEN "  ✅ CI/CD infrastructure found:"
        for file in "${cicd_files[@]}"; do
            echo "    - $file"
        done
    fi
    
    echo "  CI/CD Score: $cicd_score/100"
    echo ""
    
    # Check branch structure
    print_color $GREEN "🌿 Branch Analysis"
    local feature_branches=$(git branch -a | grep -E "(feature|feat)/" | wc -l)
    local hotfix_branches=$(git branch -a | grep -E "hotfix/" | wc -l)
    local long_lived_branches=$(git for-each-ref --format='%(refname:short) %(committerdate)' refs/heads | awk -v cutoff="$(date -d '30 days ago' '+%Y-%m-%d')" '$2 < cutoff {count++} END {print count+0}')
    
    echo "  Feature Branches: $feature_branches"
    echo "  Hotfix Branches: $hotfix_branches"
    echo "  Long-lived Branches (>30 days): $long_lived_branches"
    echo ""
    
    # Calculate trunk-based score
    local trunk_score=0
    
    if [ $feature_branches -gt 0 ]; then
        trunk_score=$((trunk_score + 25))
    fi
    
    if [ $long_lived_branches -le 2 ]; then
        trunk_score=$((trunk_score + 30))
    elif [ $long_lived_branches -le 5 ]; then
        trunk_score=$((trunk_score + 20))
    fi
    
    if [ $hotfix_branches -lt 3 ]; then
        trunk_score=$((trunk_score + 25))
    fi
    
    # Check for conventional commits
    local conventional_commits=$(git log --oneline --format=%s | grep -E "^(feat|fix|chore|docs|test|refactor):" | wc -l)
    local total_recent_commits=$(git log --oneline --since="30 days ago" | wc -l)
    
    if [ $total_recent_commits -gt 0 ]; then
        local conventional_percentage=$((conventional_commits * 100 / total_recent_commits))
        if [ $conventional_percentage -gt 70 ]; then
            trunk_score=$((trunk_score + 20))
        fi
    fi
    
    echo "  Trunk-Based Development Score: $trunk_score/100"
    echo ""
    
    # Generate quick recommendations
    print_color $GREEN "💡 Quick Recommendations"
    
    if [ $cicd_score -lt 50 ]; then
        print_color $YELLOW "  🚀 Implement CI/CD pipeline"
        echo "    - Add automated testing"
        echo "    - Add automated building"
        echo "    - Add automated deployment"
    fi
    
    if [ $trunk_score -lt 50 ]; then
        print_color $YELLOW "  🔀 Implement trunk-based development"
        echo "    - Use feature/ prefix for branches"
        echo "    - Keep branches short-lived (<3 days)"
        echo "    - Merge frequently to main"
    fi
    
    if [ $long_lived_branches -gt 5 ]; then
        print_color $YELLOW "  🌿 Clean up long-lived branches"
        echo "    - Merge or delete old branches"
        echo "    - Implement branch cleanup policy"
    fi
    
    if [ $conventional_percentage -lt 70 ]; then
        print_color $YELLOW "  📝 Adopt conventional commits"
        echo "    - Use feat:, fix:, chore: prefixes"
        echo "    - Follow conventional commit format"
    fi
    
    echo ""
    print_color $GREEN "✅ Quick analysis complete!"
    print_color $BLUE "For detailed analysis, run: ./repo_analyzer.sh $repo_path --detailed"
}

# Main execution
if [ $# -eq 0 ]; then
    print_color $RED "Usage: $0 <repository_path>"
    print_color $YELLOW "Example: $0 /path/to/repo"
    print_color $YELLOW "Example: $0 https://github.com/user/repo.git"
    exit 1
fi

REPO_PATH=$1

# Check if it's a URL or local path
if [[ $REPO_PATH == http* ]]; then
    print_color $YELLOW "⚠️  URL provided. For detailed analysis of remote repositories, use:"
    print_color $YELLOW "   ./repo_analyzer.sh $REPO_PATH --detailed"
    print_color $YELLOW ""
    print_color $YELLOW "This script works best with local repositories."
    exit 1
fi

# Check if path exists
if [ ! -d "$REPO_PATH" ]; then
    print_color $RED "❌ Directory does not exist: $REPO_PATH"
    exit 1
fi

# Run quick analysis
quick_analyze "$REPO_PATH"
