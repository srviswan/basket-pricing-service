# Repository Analysis Scripts

This directory contains scripts to analyze Git repositories and provide recommendations for implementing trunk-based development and CI/CD practices.

## Scripts Overview

### 1. `repo_analyzer.py`
**Purpose**: Python-based repository analysis tool that examines Git repositories for development patterns and provides recommendations.

**Features**:
- Branch structure analysis
- Commit pattern analysis
- Developer activity analysis
- CI/CD infrastructure detection
- Trunk-based development scoring
- Automated recommendations
- HTML and JSON report generation

**Usage**:
```bash
python3 repo_analyzer.py <repository_url> [options]
```

**Options**:
- `--output FILE`: Output file path (default: analysis_report.html)
- `--format FORMAT`: Output format: html, json (default: html)
- `--detailed`: Include detailed analysis
- `--suggestions`: Include implementation suggestions

**Examples**:
```bash
# Basic analysis
python3 repo_analyzer.py https://github.com/user/repo.git

# Detailed analysis with JSON output
python3 repo_analyzer.py https://bitbucket.org/user/repo.git --detailed --format json --output report.json

# Local repository analysis
python3 repo_analyzer.py /path/to/local/repo --suggestions
```

### 2. `repo_analyzer.sh`
**Purpose**: Bash wrapper script that provides additional functionality and generates CI/CD templates.

**Features**:
- Dependency checking
- CI/CD template generation
- Branch protection setup scripts
- Branch naming policy generation
- Colored output and progress indicators

**Usage**:
```bash
./repo_analyzer.sh <repository_url> [options]
```

**Options**:
- `-o, --output FILE`: Output file path
- `-f, --format FORMAT`: Output format: html, json
- `-d, --detailed`: Include detailed analysis
- `-s, --suggestions`: Include implementation suggestions
- `-h, --help`: Show help message

**Examples**:
```bash
# Full analysis with templates
./repo_analyzer.sh https://github.com/user/repo.git --detailed --suggestions

# Generate CI/CD templates only
./repo_analyzer.sh https://github.com/user/repo.git --output analysis.html
```

## Generated Outputs

### 1. Analysis Report
- **HTML Report**: Interactive web-based report with metrics, scores, and recommendations
- **JSON Report**: Machine-readable format for integration with other tools

### 2. CI/CD Templates
Generated in `cicd_templates/` directory:
- `github-actions.yml`: GitHub Actions workflow template
- `gitlab-ci.yml`: GitLab CI pipeline template
- `Jenkinsfile`: Jenkins pipeline template

### 3. Branch Protection Setup
- `branch_protection_setup.sh`: Script to configure branch protection rules
- `branch_naming_policy.md`: Branch naming conventions and policies

## Analysis Metrics

### Key Metrics Analyzed
1. **Total Commits**: Number of commits in the repository
2. **Total Branches**: Number of branches (excluding main)
3. **Main Branch**: Detected main branch (main, master, trunk, develop)
4. **Merge Frequency**: Percentage of merge commits
5. **Hotfix Frequency**: Percentage of hotfix commits
6. **Branch Lifespan**: Average lifespan of feature branches

### Scoring System

#### CI/CD Score (0-100)
- **20 points**: GitHub Actions workflow (`.github/workflows`)
- **20 points**: GitLab CI configuration (`.gitlab-ci.yml`)
- **20 points**: Jenkins pipeline (`Jenkinsfile`)
- **20 points**: Azure Pipelines (`azure-pipelines.yml`)
- **20 points**: Other CI/CD files (CircleCI, Travis CI, etc.)

#### Trunk-Based Development Score (0-100)
- **30 points**: Number of long-lived branches (≤2 branches: 30pts, ≤5: 20pts, ≤10: 10pts)
- **25 points**: Merge frequency (>20%: 25pts, >10%: 15pts, >5%: 10pts)
- **25 points**: Hotfix frequency (<5%: 25pts, <10%: 15pts, <20%: 10pts)
- **20 points**: Branch naming conventions (feature/, hotfix/, bugfix/ prefixes)

## Recommendations Generated

### Trunk-Based Development
- Implement trunk-based development practices
- Establish branch naming conventions
- Implement short-lived feature branches
- Clean up long-lived branches
- Increase merge frequency

### CI/CD Implementation
- Implement CI/CD pipeline
- Add automated testing
- Implement branch protection
- Add quality gates
- Implement monitoring and alerting

### Code Quality
- Adopt conventional commit messages
- Implement pair programming
- Increase code review frequency
- Add development metrics tracking

## Implementation Guide

### Trunk-Based Development Implementation

#### 1. Branch Naming Conventions
```
feature/description     # New features
hotfix/description      # Urgent fixes
bugfix/description      # Bug fixes
release/version         # Release branches
```

#### 2. Short-Lived Branches
- Keep feature branches active for 1-3 days maximum
- Merge frequently to main branch
- Delete branches after merging
- Use feature flags for incomplete features

#### 3. Branch Protection
- Require pull request reviews
- Require status checks to pass
- Restrict pushes to main branch
- Enable automatic branch deletion

### CI/CD Implementation

#### 1. Pipeline Stages
- **Test**: Unit tests, integration tests, code quality checks
- **Build**: Compilation, packaging, artifact creation
- **Deploy**: Automated deployment to staging/production

#### 2. Quality Gates
- Code coverage requirements (>80%)
- Static code analysis (SonarQube, Checkstyle)
- Security vulnerability scanning
- Performance testing

#### 3. Monitoring and Alerting
- Build status notifications
- Deployment success/failure alerts
- Performance monitoring
- Error tracking and alerting

## Dependencies

### Required Tools
- **Git**: For repository analysis
- **Python 3**: For the analysis script
- **Bash**: For the wrapper script

### Optional Tools
- **GitHub CLI (gh)**: For automated branch protection setup
- **GitLab API**: For GitLab-specific features
- **Docker**: For containerized CI/CD pipelines

## Troubleshooting

### Common Issues

#### 1. Repository Access
**Problem**: Cannot clone or access repository
**Solution**: 
- Check repository URL format
- Verify authentication credentials
- Ensure repository is publicly accessible or you have proper permissions

#### 2. Git Command Failures
**Problem**: Git commands fail during analysis
**Solution**:
- Ensure Git is installed and accessible
- Check repository integrity
- Verify branch references exist

#### 3. Python Dependencies
**Problem**: Python script fails to run
**Solution**:
- Ensure Python 3 is installed
- Check Python path and permissions
- Verify script has execute permissions

### Debug Mode
```bash
# Enable verbose output
bash -x repo_analyzer.sh https://github.com/user/repo.git

# Check Git status
git status
git log --oneline -10
git branch -a
```

## Integration Examples

### GitHub Actions Integration
```yaml
name: Repository Analysis
on:
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM
  workflow_dispatch:

jobs:
  analyze:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - name: Run Repository Analysis
      run: |
        chmod +x scripts/repo_analyzer.sh
        ./scripts/repo_analyzer.sh ${{ github.repository }} --detailed --suggestions
    - name: Upload Analysis Report
      uses: actions/upload-artifact@v3
      with:
        name: analysis-report
        path: analysis_report.html
```

### GitLab CI Integration
```yaml
analyze:
  stage: analyze
  script:
    - chmod +x scripts/repo_analyzer.sh
    - ./scripts/repo_analyzer.sh $CI_PROJECT_URL --detailed --suggestions
  artifacts:
    reports:
      junit: analysis_report.json
    paths:
      - analysis_report.html
    expire_in: 1 week
```

## Contributing

### Adding New Analysis Features
1. Extend the `RepositoryAnalyzer` class in `repo_analyzer.py`
2. Add new metrics to the `AnalysisResult` dataclass
3. Update the HTML report template
4. Add corresponding tests

### Adding New CI/CD Templates
1. Create new template files in the `generate_cicd_templates()` function
2. Follow existing template patterns
3. Include comprehensive pipeline stages
4. Add documentation for the new template

### Improving Recommendations
1. Add new recommendation logic to `generate_recommendations()`
2. Include implementation examples
3. Add priority levels for recommendations
4. Provide specific action items

## License

This analysis tool is part of the basket pricing service project and follows the same license terms.

## Support

For issues with the repository analysis scripts:
1. Check the troubleshooting section
2. Review script logs and error messages
3. Validate repository access and permissions
4. Contact the development team

## Examples

### Analyzing a GitHub Repository
```bash
# Basic analysis
./repo_analyzer.sh https://github.com/microsoft/vscode.git

# Detailed analysis with suggestions
./repo_analyzer.sh https://github.com/spring-projects/spring-boot.git --detailed --suggestions --output spring-boot-analysis.html
```

### Analyzing a Bitbucket Repository
```bash
# Bitbucket analysis
./repo_analyzer.sh https://bitbucket.org/atlassian/stash.git --format json --output stash-analysis.json
```

### Analyzing a Local Repository
```bash
# Local repository analysis
./repo_analyzer.sh /path/to/local/repo --detailed --suggestions
```

### Using Python Script Directly
```bash
# Python script with custom options
python3 repo_analyzer.py https://github.com/user/repo.git --detailed --format json --output custom-report.json
```
