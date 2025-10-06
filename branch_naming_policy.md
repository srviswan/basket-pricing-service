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
