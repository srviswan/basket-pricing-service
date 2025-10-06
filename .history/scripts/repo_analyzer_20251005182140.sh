#!/bin/bash

# Repository Analysis Script for Trunk-Based Development and CI/CD Implementation
# This script analyzes Git repositories and provides recommendations for improvement

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
REPO_URL=""
OUTPUT_FORMAT="html"
DETAILED=false
SUGGESTIONS=false
OUTPUT_FILE=""

# Function to print colored output
print_color() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [OPTIONS] <repository_url_or_path>"
    echo ""
    echo "Options:"
    echo "  -o, --output FILE     Output file path (default: analysis_report.html)"
    echo "  -f, --format FORMAT   Output format: html, json (default: html)"
    echo "  -d, --detailed        Include detailed analysis"
    echo "  -s, --suggestions     Include implementation suggestions"
    echo "  -h, --help           Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 https://github.com/user/repo.git"
    echo "  $0 https://bitbucket.org/user/repo.git --output analysis.html"
    echo "  $0 /path/to/local/repo --detailed --suggestions"
    echo "  $0 https://github.com/user/repo.git --format json --output report.json"
}

# Function to check dependencies
check_dependencies() {
    local missing_deps=()
    
    if ! command -v git &> /dev/null; then
        missing_deps+=("git")
    fi
    
    if ! command -v python3 &> /dev/null; then
        missing_deps+=("python3")
    fi
    
    if [ ${#missing_deps[@]} -ne 0 ]; then
        print_color $RED "Error: Missing required dependencies: ${missing_deps[*]}"
        print_color $YELLOW "Please install the missing dependencies and try again."
        exit 1
    fi
}

# Function to analyze repository
analyze_repository() {
    local repo_path=$1
    local output_file=$2
    local format=$3
    local detailed_flag=""
    local suggestions_flag=""
    
    if [ "$DETAILED" = true ]; then
        detailed_flag="--detailed"
    fi
    
    if [ "$SUGGESTIONS" = true ]; then
        suggestions_flag="--suggestions"
    fi
    
    print_color $BLUE "Starting repository analysis..."
    print_color $BLUE "Repository: $repo_path"
    print_color $BLUE "Output format: $format"
    print_color $BLUE "Output file: $output_file"
    
    # Run Python analysis script
    python3 "$(dirname "$0")/repo_analyzer.py" \
        "$repo_path" \
        --output "$output_file" \
        --format "$format" \
        $detailed_flag \
        $suggestions_flag
}

# Function to generate CI/CD templates
generate_cicd_templates() {
    local output_dir="cicd_templates"
    
    print_color $BLUE "Generating CI/CD templates..."
    
    mkdir -p "$output_dir"
    
    # GitHub Actions template
    cat > "$output_dir/github-actions.yml" << 'EOF'
name: CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

env:
  JAVA_VERSION: '21'
  MAVEN_OPTS: '-Xmx2048m'

jobs:
  test:
    name: Test
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up JDK
      uses: actions/setup-java@v4
      with:
        java-version: ${{ env.JAVA_VERSION }}
        distribution: 'temurin'
        
    - name: Cache Maven dependencies
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
        
    - name: Run tests
      run: mvn test
      
    - name: Upload test results
      uses: actions/upload-artifact@v3
      if: always()
      with:
        name: test-results
        path: target/surefire-reports/

  build:
    name: Build
    runs-on: ubuntu-latest
    needs: test
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up JDK
      uses: actions/setup-java@v4
      with:
        java-version: ${{ env.JAVA_VERSION }}
        distribution: 'temurin'
        
    - name: Build application
      run: mvn clean package -DskipTests
      
    - name: Upload build artifacts
      uses: actions/upload-artifact@v3
      with:
        name: build-artifacts
        path: target/*.jar

  deploy:
    name: Deploy
    runs-on: ubuntu-latest
    needs: build
    if: github.ref == 'refs/heads/main'
    
    steps:
    - name: Deploy to production
      run: echo "Deploy to production"
EOF

    # GitLab CI template
    cat > "$output_dir/gitlab-ci.yml" << 'EOF'
stages:
  - test
  - build
  - deploy

variables:
  JAVA_VERSION: "21"
  MAVEN_OPTS: "-Xmx2048m"

test:
  stage: test
  image: maven:3.9.6-eclipse-temurin-21
  script:
    - mvn test
  artifacts:
    reports:
      junit: target/surefire-reports/TEST-*.xml
    paths:
      - target/surefire-reports/
    expire_in: 1 week

build:
  stage: build
  image: maven:3.9.6-eclipse-temurin-21
  script:
    - mvn clean package -DskipTests
  artifacts:
    paths:
      - target/*.jar
    expire_in: 1 week

deploy:
  stage: deploy
  script:
    - echo "Deploy to production"
  only:
    - main
EOF

    # Jenkinsfile template
    cat > "$output_dir/Jenkinsfile" << 'EOF'
pipeline {
    agent any
    
    environment {
        JAVA_VERSION = '21'
        MAVEN_OPTS = '-Xmx2048m'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/TEST-*.xml'
                }
            }
        }
        
        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }
        
        stage('Deploy') {
            when {
                branch 'main'
            }
            steps {
                echo 'Deploy to production'
            }
        }
    }
    
    post {
        always {
            archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
        }
    }
}
EOF

    print_color $GREEN "CI/CD templates generated in $output_dir/"
}

# Function to generate branch protection script
generate_branch_protection() {
    local output_file="branch_protection_setup.sh"
    
    print_color $BLUE "Generating branch protection setup script..."
    
    cat > "$output_file" << 'EOF'
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
EOF

    print_color $GREEN "Branch naming policy generated: $output_file"
}

# Main execution
if [ $# -eq 0 ]; then
    print_color $RED "Error: Repository URL or path required"
    show_usage
    exit 1
fi

REPO_URL=$1

# Parse command line arguments
while [[ $# -gt 1 ]]; do
    case $1 in
        -o|--output)
            OUTPUT_FILE="$2"
            shift 2
            ;;
        -f|--format)
            OUTPUT_FORMAT="$2"
            shift 2
            ;;
        -d|--detailed)
            DETAILED=true
            shift
            ;;
        -s|--suggestions)
            SUGGESTIONS=true
            shift
            ;;
        -h|--help)
            show_usage
            exit 0
            ;;
        *)
            print_color $RED "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Set default output file if not provided
if [ -z "$OUTPUT_FILE" ]; then
    if [ "$OUTPUT_FORMAT" = "html" ]; then
        OUTPUT_FILE="analysis_report.html"
    else
        OUTPUT_FILE="analysis_report.json"
    fi
fi

# Check dependencies
check_dependencies

# Generate CI/CD templates
generate_cicd_templates

# Generate branch protection setup
generate_branch_protection

# Generate branch naming policy
generate_branch_naming_policy

# Analyze repository
analyze_repository "$REPO_URL" "$OUTPUT_FILE" "$OUTPUT_FORMAT"

print_color $GREEN "Analysis complete!"
print_color $GREEN "Report saved to: $OUTPUT_FILE"
print_color $GREEN "CI/CD templates generated in: cicd_templates/"
print_color $GREEN "Branch protection setup: branch_protection_setup.sh"
print_color $GREEN "Branch naming policy: branch_naming_policy.md"
