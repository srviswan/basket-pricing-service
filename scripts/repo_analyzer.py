#!/usr/bin/env python3
"""
Repository Analysis Script for Trunk-Based Development and CI/CD Implementation

This script analyzes Git repositories to understand development patterns and suggests
improvements for implementing trunk-based development and CI/CD practices.

Usage:
    python repo_analyzer.py <repository_url> [options]

Examples:
    python repo_analyzer.py https://github.com/user/repo.git
    python repo_analyzer.py https://bitbucket.org/user/repo.git --output analysis_report.html
    python repo_analyzer.py /path/to/local/repo --detailed --suggestions
"""

import os
import sys
import json
import argparse
import subprocess
import tempfile
import shutil
from datetime import datetime, timedelta
from collections import defaultdict, Counter
from typing import Dict, List, Tuple, Optional, Any
import re
import urllib.parse
from dataclasses import dataclass, asdict
import html

@dataclass
class BranchInfo:
    name: str
    last_commit_date: str
    commit_count: int
    author_count: int
    is_protected: bool = False
    is_main: bool = False

@dataclass
class CommitPattern:
    pattern: str
    count: int
    percentage: float

@dataclass
class DeveloperActivity:
    author: str
    commits: int
    branches_created: int
    avg_commit_size: float
    last_activity: str

@dataclass
class AnalysisResult:
    repository_url: str
    analysis_date: str
    total_commits: int
    total_branches: int
    main_branch: str
    branch_lifespan_avg: float
    merge_frequency: float
    hotfix_frequency: float
    ci_cd_score: float
    trunk_based_score: float
    recommendations: List[str]
    branch_info: List[BranchInfo]
    commit_patterns: List[CommitPattern]
    developer_activity: List[DeveloperActivity]

class RepositoryAnalyzer:
    def __init__(self, repo_path: str, detailed: bool = False):
        self.repo_path = repo_path
        self.detailed = detailed
        self.temp_dir = None
        
    def __enter__(self):
        return self
        
    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.temp_dir and os.path.exists(self.temp_dir):
            shutil.rmtree(self.temp_dir)
    
    def clone_repository(self, repo_url: str) -> str:
        """Clone repository to temporary directory"""
        self.temp_dir = tempfile.mkdtemp(prefix="repo_analysis_")
        
        try:
            print(f"Cloning repository: {repo_url}")
            subprocess.run([
                "git", "clone", "--mirror", repo_url, 
                os.path.join(self.temp_dir, "repo.git")
            ], check=True, capture_output=True)
            
            # Create a working copy
            working_dir = os.path.join(self.temp_dir, "working")
            subprocess.run([
                "git", "clone", os.path.join(self.temp_dir, "repo.git"), working_dir
            ], check=True, capture_output=True)
            
            return working_dir
            
        except subprocess.CalledProcessError as e:
            print(f"Error cloning repository: {e}")
            sys.exit(1)
    
    def get_git_command(self, command: List[str]) -> str:
        """Execute git command and return output"""
        try:
            result = subprocess.run(
                ["git"] + command,
                cwd=self.repo_path,
                capture_output=True,
                text=True,
                check=True
            )
            return result.stdout.strip()
        except subprocess.CalledProcessError as e:
            print(f"Git command failed: {' '.join(command)}")
            return ""
    
    def analyze_branches(self) -> Tuple[List[BranchInfo], str]:
        """Analyze branch structure and patterns"""
        print("Analyzing branch structure...")
        
        # Get all branches
        branches_output = self.get_git_command(["branch", "-a", "--format=%(refname:short)|%(committerdate:iso)|%(authorname)"])
        
        branch_info = []
        main_branch = "main"  # Default
        
        # Try to detect main branch
        main_candidates = ["main", "master", "trunk", "develop"]
        for candidate in main_candidates:
            if self.get_git_command(["rev-parse", "--verify", candidate]):
                main_branch = candidate
                break
        
        for line in branches_output.split('\n'):
            if not line or 'HEAD' in line:
                continue
                
            parts = line.split('|')
            if len(parts) >= 3:
                branch_name = parts[0].replace('origin/', '')
                commit_date = parts[1]
                author = parts[2]
                
                # Get commit count for this branch
                commit_count = len(self.get_git_command([
                    "rev-list", "--count", f"{main_branch}..{branch_name}"
                ]).split('\n'))
                
                # Get unique authors for this branch
                authors_output = self.get_git_command([
                    "log", f"{main_branch}..{branch_name}", "--format=%an"
                ])
                author_count = len(set(authors_output.split('\n'))) if authors_output else 0
                
                branch_info.append(BranchInfo(
                    name=branch_name,
                    last_commit_date=commit_date,
                    commit_count=commit_count,
                    author_count=author_count,
                    is_main=(branch_name == main_branch)
                ))
        
        return branch_info, main_branch
    
    def analyze_commit_patterns(self) -> List[CommitPattern]:
        """Analyze commit message patterns"""
        print("Analyzing commit patterns...")
        
        # Get commit messages
        commits_output = self.get_git_command([
            "log", "--oneline", "--format=%s"
        ])
        
        patterns = defaultdict(int)
        total_commits = 0
        
        for commit_msg in commits_output.split('\n'):
            if not commit_msg:
                continue
                
            total_commits += 1
            
            # Extract common patterns
            msg_lower = commit_msg.lower()
            
            if msg_lower.startswith('feat'):
                patterns['feat:'] += 1
            elif msg_lower.startswith('fix'):
                patterns['fix:'] += 1
            elif msg_lower.startswith('hotfix'):
                patterns['hotfix:'] += 1
            elif msg_lower.startswith('merge'):
                patterns['merge:'] += 1
            elif msg_lower.startswith('chore'):
                patterns['chore:'] += 1
            elif msg_lower.startswith('docs'):
                patterns['docs:'] += 1
            elif msg_lower.startswith('test'):
                patterns['test:'] += 1
            elif msg_lower.startswith('refactor'):
                patterns['refactor:'] += 1
            elif 'wip' in msg_lower:
                patterns['WIP'] += 1
            elif 'temp' in msg_lower or 'temporary' in msg_lower:
                patterns['temporary'] += 1
            else:
                patterns['other'] += 1
        
        commit_patterns = []
        for pattern, count in patterns.items():
            commit_patterns.append(CommitPattern(
                pattern=pattern,
                count=count,
                percentage=(count / total_commits * 100) if total_commits > 0 else 0
            ))
        
        return sorted(commit_patterns, key=lambda x: x.count, reverse=True)
    
    def analyze_developer_activity(self) -> List[DeveloperActivity]:
        """Analyze developer activity patterns"""
        print("Analyzing developer activity...")
        
        # Get author statistics
        authors_output = self.get_git_command([
            "log", "--format=%an|%ae", "--all"
        ])
        
        author_stats = defaultdict(lambda: {
            'commits': 0,
            'branches': set(),
            'commit_sizes': [],
            'last_activity': ''
        })
        
        for line in authors_output.split('\n'):
            if not line:
                continue
                
            parts = line.split('|')
            if len(parts) >= 2:
                author = parts[0]
                author_stats[author]['commits'] += 1
        
        # Get branch creation info
        branches_output = self.get_git_command([
            "for-each-ref", "--format=%(refname:short)|%(authorname)", "refs/heads"
        ])
        
        for line in branches_output.split('\n'):
            if not line:
                continue
                
            parts = line.split('|')
            if len(parts) >= 2:
                branch_name = parts[0]
                author = parts[1]
                author_stats[author]['branches'].add(branch_name)
        
        # Get last activity dates
        last_activity_output = self.get_git_command([
            "log", "--format=%an|%ad", "--date=iso", "--all", "-1"
        ])
        
        if last_activity_output:
            parts = last_activity_output.split('|')
            if len(parts) >= 2:
                author = parts[0]
                last_date = parts[1]
                author_stats[author]['last_activity'] = last_date
        
        developer_activity = []
        for author, stats in author_stats.items():
            avg_commit_size = sum(stats['commit_sizes']) / len(stats['commit_sizes']) if stats['commit_sizes'] else 0
            
            developer_activity.append(DeveloperActivity(
                author=author,
                commits=stats['commits'],
                branches_created=len(stats['branches']),
                avg_commit_size=avg_commit_size,
                last_activity=stats['last_activity']
            ))
        
        return sorted(developer_activity, key=lambda x: x.commits, reverse=True)
    
    def calculate_metrics(self, branch_info: List[BranchInfo], commit_patterns: List[CommitPattern]) -> Tuple[float, float, float, float]:
        """Calculate key metrics"""
        print("Calculating metrics...")
        
        # Branch lifespan (simplified)
        total_branches = len([b for b in branch_info if not b.is_main])
        branch_lifespan_avg = 0.0  # Would need more complex analysis
        
        # Merge frequency
        merge_commits = sum(p.count for p in commit_patterns if 'merge' in p.pattern.lower())
        total_commits = sum(p.count for p in commit_patterns)
        merge_frequency = (merge_commits / total_commits * 100) if total_commits > 0 else 0
        
        # Hotfix frequency
        hotfix_commits = sum(p.count for p in commit_patterns if 'hotfix' in p.pattern.lower())
        hotfix_frequency = (hotfix_commits / total_commits * 100) if total_commits > 0 else 0
        
        # CI/CD Score (based on file presence)
        ci_cd_files = [
            '.github/workflows',
            '.gitlab-ci.yml',
            'Jenkinsfile',
            'azure-pipelines.yml',
            'circle.yml',
            '.travis.yml'
        ]
        
        ci_cd_score = 0
        for file_path in ci_cd_files:
            if os.path.exists(os.path.join(self.repo_path, file_path)):
                ci_cd_score += 20
        
        # Trunk-based development score
        trunk_based_score = 0
        
        # Factor 1: Number of long-lived branches (lower is better)
        long_lived_branches = len([b for b in branch_info if b.commit_count > 10])
        if long_lived_branches <= 2:
            trunk_based_score += 30
        elif long_lived_branches <= 5:
            trunk_based_score += 20
        elif long_lived_branches <= 10:
            trunk_based_score += 10
        
        # Factor 2: Merge frequency (higher is better for trunk-based)
        if merge_frequency > 20:
            trunk_based_score += 25
        elif merge_frequency > 10:
            trunk_based_score += 15
        elif merge_frequency > 5:
            trunk_based_score += 10
        
        # Factor 3: Hotfix frequency (lower is better)
        if hotfix_frequency < 5:
            trunk_based_score += 25
        elif hotfix_frequency < 10:
            trunk_based_score += 15
        elif hotfix_frequency < 20:
            trunk_based_score += 10
        
        # Factor 4: Branch naming conventions
        feature_branches = len([b for b in branch_info if b.name.startswith('feature/') or b.name.startswith('feat/')])
        if feature_branches > 0:
            trunk_based_score += 20
        
        return branch_lifespan_avg, merge_frequency, hotfix_frequency, ci_cd_score, trunk_based_score
    
    def generate_recommendations(self, branch_info: List[BranchInfo], commit_patterns: List[CommitPattern], 
                               ci_cd_score: float, trunk_based_score: float) -> List[str]:
        """Generate recommendations for improvement"""
        recommendations = []
        
        # Trunk-based development recommendations
        if trunk_based_score < 50:
            recommendations.append("🔀 Implement trunk-based development: Reduce long-lived branches and increase merge frequency")
            recommendations.append("📝 Establish branch naming conventions: Use feature/, hotfix/, bugfix/ prefixes")
            recommendations.append("⏱️ Implement short-lived feature branches: Keep branches active for less than 2-3 days")
        
        # CI/CD recommendations
        if ci_cd_score < 60:
            recommendations.append("🚀 Implement CI/CD pipeline: Add automated testing, building, and deployment")
            recommendations.append("✅ Add automated testing: Unit tests, integration tests, and code quality checks")
            recommendations.append("🔒 Implement branch protection: Require PR reviews and status checks")
        
        # Commit message recommendations
        conventional_commits = sum(p.count for p in commit_patterns if p.pattern in ['feat:', 'fix:', 'chore:', 'docs:', 'test:', 'refactor:'])
        total_commits = sum(p.count for p in commit_patterns)
        
        if conventional_commits / total_commits < 0.7:
            recommendations.append("📋 Adopt conventional commit messages: Use feat:, fix:, chore:, docs:, test:, refactor: prefixes")
        
        # Branch management recommendations
        long_lived_branches = [b for b in branch_info if b.commit_count > 20 and not b.is_main]
        if len(long_lived_branches) > 3:
            recommendations.append("🌿 Clean up long-lived branches: Merge or delete branches with >20 commits")
        
        # Developer activity recommendations
        recommendations.append("👥 Implement pair programming: Reduce single-developer branches")
        recommendations.append("🔄 Increase code review frequency: Require reviews for all changes")
        recommendations.append("📊 Add development metrics: Track cycle time, lead time, and deployment frequency")
        
        return recommendations
    
    def analyze(self, repo_url: str) -> AnalysisResult:
        """Main analysis method"""
        print(f"Starting analysis of repository: {repo_url}")
        
        # Clone repository if needed
        if not os.path.exists(self.repo_path):
            self.repo_path = self.clone_repository(repo_url)
        
        # Perform analysis
        branch_info, main_branch = self.analyze_branches()
        commit_patterns = self.analyze_commit_patterns()
        developer_activity = self.analyze_developer_activity()
        
        # Calculate metrics
        branch_lifespan_avg, merge_frequency, hotfix_frequency, ci_cd_score, trunk_based_score = self.calculate_metrics(branch_info, commit_patterns)
        
        # Generate recommendations
        recommendations = self.generate_recommendations(branch_info, commit_patterns, ci_cd_score, trunk_based_score)
        
        # Create result
        result = AnalysisResult(
            repository_url=repo_url,
            analysis_date=datetime.now().isoformat(),
            total_commits=sum(p.count for p in commit_patterns),
            total_branches=len(branch_info),
            main_branch=main_branch,
            branch_lifespan_avg=branch_lifespan_avg,
            merge_frequency=merge_frequency,
            hotfix_frequency=hotfix_frequency,
            ci_cd_score=ci_cd_score,
            trunk_based_score=trunk_based_score,
            recommendations=recommendations,
            branch_info=branch_info,
            commit_patterns=commit_patterns,
            developer_activity=developer_activity
        )
        
        return result

def generate_html_report(result: AnalysisResult) -> str:
    """Generate HTML report"""
    html_template = """
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Repository Analysis Report</title>
        <style>
            body { font-family: Arial, sans-serif; margin: 40px; line-height: 1.6; }
            .header { background: #f4f4f4; padding: 20px; border-radius: 8px; margin-bottom: 30px; }
            .metric { background: #e8f4fd; padding: 15px; margin: 10px 0; border-radius: 5px; border-left: 4px solid #2196F3; }
            .score { font-size: 24px; font-weight: bold; color: #2196F3; }
            .recommendations { background: #fff3cd; padding: 20px; border-radius: 5px; border-left: 4px solid #ffc107; }
            .recommendations ul { margin: 10px 0; }
            .recommendations li { margin: 8px 0; }
            table { width: 100%; border-collapse: collapse; margin: 20px 0; }
            th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }
            th { background-color: #f2f2f2; }
            .good { color: #28a745; }
            .warning { color: #ffc107; }
            .danger { color: #dc3545; }
        </style>
    </head>
    <body>
        <div class="header">
            <h1>🔍 Repository Analysis Report</h1>
            <p><strong>Repository:</strong> {repository_url}</p>
            <p><strong>Analysis Date:</strong> {analysis_date}</p>
        </div>
        
        <h2>📊 Key Metrics</h2>
        <div class="metric">
            <strong>Total Commits:</strong> {total_commits:,}
        </div>
        <div class="metric">
            <strong>Total Branches:</strong> {total_branches}
        </div>
        <div class="metric">
            <strong>Main Branch:</strong> {main_branch}
        </div>
        <div class="metric">
            <strong>Merge Frequency:</strong> {merge_frequency:.1f}%
        </div>
        <div class="metric">
            <strong>Hotfix Frequency:</strong> {hotfix_frequency:.1f}%
        </div>
        
        <h2>🎯 Scores</h2>
        <div class="metric">
            <strong>CI/CD Score:</strong> <span class="score">{ci_cd_score}/100</span>
            <p>Measures the presence of CI/CD infrastructure and automation.</p>
        </div>
        <div class="metric">
            <strong>Trunk-Based Development Score:</strong> <span class="score">{trunk_based_score}/100</span>
            <p>Measures adherence to trunk-based development practices.</p>
        </div>
        
        <h2>💡 Recommendations</h2>
        <div class="recommendations">
            <ul>
                {recommendations_html}
            </ul>
        </div>
        
        <h2>🌿 Branch Analysis</h2>
        <table>
            <tr>
                <th>Branch Name</th>
                <th>Commits</th>
                <th>Authors</th>
                <th>Last Activity</th>
                <th>Type</th>
            </tr>
            {branches_html}
        </table>
        
        <h2>📝 Commit Patterns</h2>
        <table>
            <tr>
                <th>Pattern</th>
                <th>Count</th>
                <th>Percentage</th>
            </tr>
            {commit_patterns_html}
        </table>
        
        <h2>👥 Developer Activity</h2>
        <table>
            <tr>
                <th>Developer</th>
                <th>Commits</th>
                <th>Branches Created</th>
                <th>Last Activity</th>
            </tr>
            {developer_activity_html}
        </table>
        
        <h2>🚀 Implementation Guide</h2>
        <div class="recommendations">
            <h3>Trunk-Based Development Implementation:</h3>
            <ol>
                <li><strong>Establish Branch Naming Conventions:</strong>
                    <ul>
                        <li>feature/description - for new features</li>
                        <li>hotfix/description - for urgent fixes</li>
                        <li>bugfix/description - for bug fixes</li>
                    </ul>
                </li>
                <li><strong>Implement Short-Lived Branches:</strong>
                    <ul>
                        <li>Keep feature branches active for 1-3 days maximum</li>
                        <li>Merge frequently to main branch</li>
                        <li>Delete branches after merging</li>
                    </ul>
                </li>
                <li><strong>Enable Branch Protection:</strong>
                    <ul>
                        <li>Require pull request reviews</li>
                        <li>Require status checks to pass</li>
                        <li>Restrict pushes to main branch</li>
                    </ul>
                </li>
            </ol>
            
            <h3>CI/CD Implementation:</h3>
            <ol>
                <li><strong>Add CI/CD Pipeline:</strong>
                    <ul>
                        <li>Automated testing on every commit</li>
                        <li>Automated building and packaging</li>
                        <li>Automated deployment to staging/production</li>
                    </ul>
                </li>
                <li><strong>Quality Gates:</strong>
                    <ul>
                        <li>Code coverage requirements</li>
                        <li>Static code analysis</li>
                        <li>Security vulnerability scanning</li>
                    </ul>
                </li>
                <li><strong>Monitoring and Alerting:</strong>
                    <ul>
                        <li>Build status notifications</li>
                        <li>Deployment success/failure alerts</li>
                        <li>Performance monitoring</li>
                    </ul>
                </li>
            </ol>
        </div>
    </body>
    </html>
    """
    
    # Generate HTML content
    recommendations_html = ""
    for rec in result.recommendations:
        recommendations_html += f"<li>{html.escape(rec)}</li>"
    
    branches_html = ""
    for branch in result.branch_info[:20]:  # Limit to top 20 branches
        branch_type = "Main" if branch.is_main else "Feature" if branch.name.startswith(('feature/', 'feat/')) else "Other"
        branches_html += f"""
        <tr>
            <td>{html.escape(branch.name)}</td>
            <td>{branch.commit_count}</td>
            <td>{branch.author_count}</td>
            <td>{branch.last_commit_date}</td>
            <td>{branch_type}</td>
        </tr>
        """
    
    commit_patterns_html = ""
    for pattern in result.commit_patterns:
        commit_patterns_html += f"""
        <tr>
            <td>{html.escape(pattern.pattern)}</td>
            <td>{pattern.count}</td>
            <td>{pattern.percentage:.1f}%</td>
        </tr>
        """
    
    developer_activity_html = ""
    for dev in result.developer_activity[:10]:  # Limit to top 10 developers
        developer_activity_html += f"""
        <tr>
            <td>{html.escape(dev.author)}</td>
            <td>{dev.commits}</td>
            <td>{dev.branches_created}</td>
            <td>{dev.last_activity}</td>
        </tr>
        """
    
    return html_template.format(
        repository_url=html.escape(result.repository_url),
        analysis_date=result.analysis_date,
        total_commits=result.total_commits,
        total_branches=result.total_branches,
        main_branch=html.escape(result.main_branch),
        merge_frequency=result.merge_frequency,
        hotfix_frequency=result.hotfix_frequency,
        ci_cd_score=result.ci_cd_score,
        trunk_based_score=result.trunk_based_score,
        recommendations_html=recommendations_html,
        branches_html=branches_html,
        commit_patterns_html=commit_patterns_html,
        developer_activity_html=developer_activity_html
    )

def main():
    parser = argparse.ArgumentParser(description="Analyze Git repository for trunk-based development and CI/CD implementation")
    parser.add_argument("repository", help="Repository URL or local path")
    parser.add_argument("--output", "-o", help="Output file path (default: analysis_report.html)")
    parser.add_argument("--format", "-f", choices=["html", "json"], default="html", help="Output format")
    parser.add_argument("--detailed", "-d", action="store_true", help="Include detailed analysis")
    parser.add_argument("--suggestions", "-s", action="store_true", help="Include implementation suggestions")
    
    args = parser.parse_args()
    
    # Determine if it's a URL or local path
    if args.repository.startswith(('http://', 'https://', 'git@')):
        repo_path = None
        repo_url = args.repository
    else:
        repo_path = args.repository
        repo_url = args.repository
    
    # Perform analysis
    with RepositoryAnalyzer(repo_path, args.detailed) as analyzer:
        result = analyzer.analyze(repo_url)
    
    # Generate output
    if args.format == "html":
        output_content = generate_html_report(result)
        output_file = args.output or "analysis_report.html"
    else:  # JSON
        output_content = json.dumps(asdict(result), indent=2)
        output_file = args.output or "analysis_report.json"
    
    # Write output
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(output_content)
    
    print(f"Analysis complete! Report saved to: {output_file}")
    print(f"CI/CD Score: {result.ci_cd_score}/100")
    print(f"Trunk-Based Development Score: {result.trunk_based_score}/100")
    print(f"Total Recommendations: {len(result.recommendations)}")

if __name__ == "__main__":
    main()
