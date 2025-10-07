#!/bin/bash

# Bitbucket Repository Refactoring Analysis Script
# This script analyzes a Bitbucket repository and proposes recommendations
# for refactoring into multiple smaller repositories for trunk-based development

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Default values
REPO_URL=""
OUTPUT_FORMAT="html"
DETAILED=false
SUGGESTIONS=false
OUTPUT_FILE=""
ANALYSIS_DEPTH="medium"
MIN_REPO_SIZE=1000
MAX_REPO_SIZE=50000

# Function to print colored output
print_color() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [OPTIONS] <bitbucket_repository_url>"
    echo ""
    echo "Options:"
    echo "  -o, --output FILE        Output file path (default: refactoring_analysis.html)"
    echo "  -f, --format FORMAT      Output format: html, json, markdown (default: html)"
    echo "  -d, --detailed           Include detailed analysis"
    echo "  -s, --suggestions        Include implementation suggestions"
    echo "  --depth LEVEL            Analysis depth: shallow, medium, deep (default: medium)"
    echo "  --min-size SIZE          Minimum repository size in KB (default: 1000)"
    echo "  --max-size SIZE          Maximum repository size in KB (default: 50000)"
    echo "  -h, --help              Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 https://bitbucket.org/workspace/repo.git"
    echo "  $0 https://bitbucket.org/workspace/repo.git --detailed --suggestions"
    echo "  $0 https://bitbucket.org/workspace/repo.git --format json --output analysis.json"
    echo "  $0 https://bitbucket.org/workspace/repo.git --depth deep --min-size 500 --max-size 100000"
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
    
    if ! command -v curl &> /dev/null; then
        missing_deps+=("curl")
    fi
    
    if [ ${#missing_deps[@]} -ne 0 ]; then
        print_color $RED "Error: Missing required dependencies: ${missing_deps[*]}"
        print_color $YELLOW "Please install the missing dependencies and try again."
        exit 1
    fi
}

# Function to clone repository
clone_repository() {
    local repo_url=$1
    local clone_dir="temp_repo_analysis"
    
    print_color $BLUE "Cloning repository: $repo_url"
    
    if [ -d "$clone_dir" ]; then
        rm -rf "$clone_dir"
    fi
    
    git clone "$repo_url" "$clone_dir" --quiet 2>/dev/null
    echo "$clone_dir"
}

# Function to analyze repository structure
analyze_repository_structure() {
    local repo_path=$1
    local analysis_file="repo_structure.json"
    
    print_color $BLUE "Analyzing repository structure..."
    
    python3 << EOF
import os
import json
import subprocess
from pathlib import Path

def get_file_stats(file_path):
    """Get file statistics"""
    try:
        result = subprocess.run(['wc', '-l', file_path], capture_output=True, text=True)
        lines = int(result.stdout.split()[0]) if result.returncode == 0 else 0
        
        size = os.path.getsize(file_path)
        return {
            'lines': lines,
            'size_bytes': size,
            'size_kb': round(size / 1024, 2)
        }
    except:
        return {'lines': 0, 'size_bytes': 0, 'size_kb': 0}

def analyze_directory_structure(path, max_depth=3, current_depth=0):
    """Analyze directory structure recursively"""
    if current_depth >= max_depth:
        return None
    
    structure = {
        'name': os.path.basename(path),
        'type': 'directory',
        'children': [],
        'file_count': 0,
        'total_lines': 0,
        'total_size_kb': 0,
        'depth': current_depth
    }
    
    try:
        for item in sorted(os.listdir(path)):
            item_path = os.path.join(path, item)
            
            if os.path.isdir(item_path):
                # Skip hidden directories and common build/cache directories
                if item.startswith('.') or item in ['node_modules', 'target', 'build', 'dist', '__pycache__', '.git']:
                    continue
                
                child_structure = analyze_directory_structure(item_path, max_depth, current_depth + 1)
                if child_structure:
                    structure['children'].append(child_structure)
                    structure['file_count'] += child_structure['file_count']
                    structure['total_lines'] += child_structure['total_lines']
                    structure['total_size_kb'] += child_structure['total_size_kb']
            
            elif os.path.isfile(item_path):
                stats = get_file_stats(item_path)
                file_info = {
                    'name': item,
                    'type': 'file',
                    'lines': stats['lines'],
                    'size_kb': stats['size_kb'],
                    'extension': os.path.splitext(item)[1]
                }
                structure['children'].append(file_info)
                structure['file_count'] += 1
                structure['total_lines'] += stats['lines']
                structure['total_size_kb'] += stats['size_kb']
    
    except PermissionError:
        pass
    
    return structure

def analyze_tech_stack(path):
    """Analyze technology stack"""
    tech_stack = {
        'languages': {},
        'frameworks': {},
        'build_tools': {},
        'databases': {},
        'cloud_platforms': {}
    }
    
    # Common file patterns for technology detection
    patterns = {
        'Java': ['*.java', 'pom.xml', 'build.gradle', '*.jar'],
        'Python': ['*.py', 'requirements.txt', 'setup.py', 'Pipfile'],
        'JavaScript': ['*.js', 'package.json', '*.ts', 'tsconfig.json'],
        'C#': ['*.cs', '*.csproj', '*.sln'],
        'Go': ['*.go', 'go.mod', 'go.sum'],
        'Rust': ['*.rs', 'Cargo.toml', 'Cargo.lock'],
        'PHP': ['*.php', 'composer.json'],
        'Ruby': ['*.rb', 'Gemfile', '*.gemspec'],
        'C/C++': ['*.c', '*.cpp', '*.h', 'Makefile', 'CMakeLists.txt'],
        'Scala': ['*.scala', 'build.sbt'],
        'Kotlin': ['*.kt', '*.kts']
    }
    
    frameworks = {
        'Spring Boot': ['spring-boot-starter', 'SpringBootApplication'],
        'Django': ['django', 'Django'],
        'Flask': ['flask', 'Flask'],
        'Express': ['express', 'Express'],
        'React': ['react', 'React'],
        'Angular': ['angular', 'Angular'],
        'Vue': ['vue', 'Vue'],
        'ASP.NET': ['aspnet', 'ASP.NET'],
        'Laravel': ['laravel', 'Laravel'],
        'Rails': ['rails', 'Rails']
    }
    
    build_tools = {
        'Maven': ['pom.xml'],
        'Gradle': ['build.gradle', 'gradlew'],
        'npm': ['package.json'],
        'yarn': ['yarn.lock'],
        'pip': ['requirements.txt'],
        'composer': ['composer.json'],
        'cargo': ['Cargo.toml'],
        'go mod': ['go.mod']
    }
    
    databases = {
        'MySQL': ['mysql', 'MySQL'],
        'PostgreSQL': ['postgresql', 'PostgreSQL'],
        'MongoDB': ['mongodb', 'MongoDB'],
        'Redis': ['redis', 'Redis'],
        'Elasticsearch': ['elasticsearch', 'Elasticsearch'],
        'Oracle': ['oracle', 'Oracle'],
        'SQLite': ['sqlite', 'SQLite']
    }
    
    cloud_platforms = {
        'AWS': ['aws', 'AWS', 'amazon'],
        'Azure': ['azure', 'Azure', 'microsoft'],
        'GCP': ['gcp', 'GCP', 'google'],
        'Docker': ['docker', 'Docker', 'Dockerfile'],
        'Kubernetes': ['kubernetes', 'Kubernetes', 'k8s']
    }
    
    def scan_files(pattern_dict, result_dict):
        for tech, patterns in pattern_dict.items():
            for pattern in patterns:
                try:
                    result = subprocess.run(['find', path, '-name', pattern], 
                                          capture_output=True, text=True, timeout=30)
                    if result.returncode == 0 and result.stdout.strip():
                        result_dict[tech] = result_dict.get(tech, 0) + len(result.stdout.strip().split('\n'))
                except:
                    pass
    
    scan_files(patterns, tech_stack['languages'])
    scan_files(frameworks, tech_stack['frameworks'])
    scan_files(build_tools, tech_stack['build_tools'])
    scan_files(databases, tech_stack['databases'])
    scan_files(cloud_platforms, tech_stack['cloud_platforms'])
    
    return tech_stack

def analyze_dependencies(path):
    """Analyze project dependencies"""
    dependencies = {
        'maven': [],
        'npm': [],
        'pip': [],
        'gradle': [],
        'composer': [],
        'cargo': [],
        'go': []
    }
    
    # Maven dependencies
    pom_files = subprocess.run(['find', path, '-name', 'pom.xml'], 
                             capture_output=True, text=True)
    if pom_files.returncode == 0:
        for pom_file in pom_files.stdout.strip().split('\n'):
            if pom_file:
                try:
                    with open(pom_file, 'r') as f:
                        content = f.read()
                        # Simple regex to extract dependencies (basic implementation)
                        import re
                        deps = re.findall(r'<artifactId>([^<]+)</artifactId>', content)
                        dependencies['maven'].extend(deps[:20])  # Limit to first 20
                except:
                    pass
    
    # NPM dependencies
    package_files = subprocess.run(['find', path, '-name', 'package.json'], 
                                  capture_output=True, text=True)
    if package_files.returncode == 0:
        for package_file in package_files.stdout.strip().split('\n'):
            if package_file:
                try:
                    with open(package_file, 'r') as f:
                        content = f.read()
                        import json
                        data = json.loads(content)
                        deps = list(data.get('dependencies', {}).keys())
                        dependencies['npm'].extend(deps[:20])
                except:
                    pass
    
    return dependencies

def main():
    repo_path = sys.argv[1] if len(sys.argv) > 1 else "."
    
    print("Analyzing repository structure...")
    structure = analyze_directory_structure(repo_path)
    
    print("Analyzing technology stack...")
    tech_stack = analyze_tech_stack(repo_path)
    
    print("Analyzing dependencies...")
    dependencies = analyze_dependencies(repo_path)
    
    analysis = {
        'repository_path': repo_path,
        'analysis_timestamp': subprocess.run(['date', '-Iseconds'], 
                                            capture_output=True, text=True).stdout.strip(),
        'structure': structure,
        'tech_stack': tech_stack,
        'dependencies': dependencies,
        'summary': {
            'total_files': structure['file_count'] if structure else 0,
            'total_lines': structure['total_lines'] if structure else 0,
            'total_size_kb': structure['total_size_kb'] if structure else 0,
            'primary_languages': sorted(tech_stack['languages'].items(), 
                                      key=lambda x: x[1], reverse=True)[:5],
            'primary_frameworks': sorted(tech_stack['frameworks'].items(), 
                                       key=lambda x: x[1], reverse=True)[:5]
        }
    }
    
    with open('$analysis_file', 'w') as f:
        json.dump(analysis, f, indent=2)
    
    print(f"Analysis complete. Results saved to $analysis_file")

if __name__ == "__main__":
    main()
EOF
    
    echo "$analysis_file"
}

# Function to generate refactoring recommendations
generate_refactoring_recommendations() {
    local structure_file=$1
    local output_file="refactoring_recommendations.json"
    
    print_color $BLUE "Generating refactoring recommendations..."
    
    python3 << EOF
import json
import os
from pathlib import Path

def calculate_complexity_score(node):
    """Calculate complexity score for a directory/file"""
    if node['type'] == 'file':
        # File complexity based on lines and size
        lines_score = min(node['lines'] / 1000, 10)  # Max 10 points for lines
        size_score = min(node['size_kb'] / 1000, 5)  # Max 5 points for size
        return lines_score + size_score
    else:
        # Directory complexity based on children
        child_complexity = sum(calculate_complexity_score(child) for child in node.get('children', []))
        file_count_score = min(node['file_count'] / 100, 5)  # Max 5 points for file count
        return child_complexity + file_count_score

def identify_service_boundaries(structure):
    """Identify potential service boundaries"""
    services = []
    
    def analyze_node(node, path=""):
        current_path = f"{path}/{node['name']}" if path else node['name']
        
        # Skip common non-service directories
        skip_dirs = ['src', 'test', 'tests', 'docs', 'documentation', 'scripts', 
                    'config', 'configuration', 'resources', 'static', 'assets']
        
        if node['type'] == 'directory' and node['name'] not in skip_dirs:
            complexity = calculate_complexity_score(node)
            
            # Check for service indicators
            service_indicators = []
            
            # Check for API-related files
            api_files = ['controller', 'api', 'endpoint', 'route', 'handler']
            for child in node.get('children', []):
                if child['type'] == 'file':
                    for indicator in api_files:
                        if indicator in child['name'].lower():
                            service_indicators.append(f"API files: {child['name']}")
                            break
            
            # Check for service-related directories
            service_dirs = ['service', 'business', 'domain', 'model', 'entity', 'repository']
            for child in node.get('children', []):
                if child['type'] == 'directory':
                    for indicator in service_dirs:
                        if indicator in child['name'].lower():
                            service_indicators.append(f"Service directory: {child['name']}")
                            break
            
            # Check for configuration files
            config_files = ['application.yml', 'application.properties', 'config.yml', 
                          'settings.py', 'config.py', 'app.config']
            for child in node.get('children', []):
                if child['type'] == 'file':
                    if child['name'] in config_files:
                        service_indicators.append(f"Configuration: {child['name']}")
                        break
            
            if service_indicators or complexity > 5:
                service = {
                    'name': node['name'],
                    'path': current_path,
                    'complexity_score': complexity,
                    'file_count': node['file_count'],
                    'total_lines': node['total_lines'],
                    'total_size_kb': node['total_size_kb'],
                    'service_indicators': service_indicators,
                    'recommended_action': 'extract' if complexity > 10 else 'consider_extract'
                }
                services.append(service)
        
        # Recursively analyze children
        for child in node.get('children', []):
            analyze_node(child, current_path)
    
    if structure:
        analyze_node(structure)
    
    return sorted(services, key=lambda x: x['complexity_score'], reverse=True)

def generate_repository_recommendations(services, summary):
    """Generate repository splitting recommendations"""
    recommendations = {
        'strategy': 'microservices_decomposition',
        'target_repositories': [],
        'shared_components': [],
        'migration_plan': [],
        'estimated_effort': 'medium',
        'risk_level': 'medium'
    }
    
    # Group services by complexity and size
    high_complexity = [s for s in services if s['complexity_score'] > 15]
    medium_complexity = [s for s in services if 5 < s['complexity_score'] <= 15]
    low_complexity = [s for s in services if s['complexity_score'] <= 5]
    
    # Recommend separate repositories for high complexity services
    for service in high_complexity:
        repo = {
            'name': f"{service['name']}-service",
            'description': f"Independent microservice for {service['name']} functionality",
            'source_path': service['path'],
            'complexity_score': service['complexity_score'],
            'file_count': service['file_count'],
            'estimated_size_kb': service['total_size_kb'],
            'priority': 'high',
            'recommended_tech_stack': determine_tech_stack(service),
            'dependencies': [],
            'api_endpoints': [],
            'database_requirements': [],
            'deployment_strategy': 'containerized'
        }
        recommendations['target_repositories'].append(repo)
    
    # Group medium complexity services
    if medium_complexity:
        grouped_services = group_related_services(medium_complexity)
        for group in grouped_services:
            repo = {
                'name': f"{group['name']}-services",
                'description': f"Grouped services for {group['name']} domain",
                'source_paths': [s['path'] for s in group['services']],
                'complexity_score': sum(s['complexity_score'] for s in group['services']),
                'file_count': sum(s['file_count'] for s in group['services']),
                'estimated_size_kb': sum(s['total_size_kb'] for s in group['services']),
                'priority': 'medium',
                'recommended_tech_stack': determine_tech_stack(group['services'][0]),
                'dependencies': [],
                'api_endpoints': [],
                'database_requirements': [],
                'deployment_strategy': 'containerized'
            }
            recommendations['target_repositories'].append(repo)
    
    # Identify shared components
    shared_components = [
        {
            'name': 'common-utilities',
            'description': 'Shared utility functions and common libraries',
            'type': 'library',
            'estimated_size_kb': 5000
        },
        {
            'name': 'shared-models',
            'description': 'Common data models and DTOs',
            'type': 'library',
            'estimated_size_kb': 2000
        },
        {
            'name': 'infrastructure-config',
            'description': 'Infrastructure and deployment configurations',
            'type': 'configuration',
            'estimated_size_kb': 1000
        }
    ]
    recommendations['shared_components'] = shared_components
    
    # Generate migration plan
    recommendations['migration_plan'] = [
        {
            'phase': 1,
            'name': 'Preparation',
            'description': 'Set up CI/CD pipelines and shared components',
            'duration': '2-3 weeks',
            'tasks': [
                'Create shared component repositories',
                'Set up CI/CD pipelines',
                'Establish coding standards',
                'Create migration documentation'
            ]
        },
        {
            'phase': 2,
            'name': 'High Priority Services',
            'description': 'Extract high complexity services first',
            'duration': '4-6 weeks',
            'tasks': [
                'Extract highest complexity services',
                'Implement API contracts',
                'Set up service discovery',
                'Migrate data and dependencies'
            ]
        },
        {
            'phase': 3,
            'name': 'Medium Priority Services',
            'description': 'Extract medium complexity services',
            'duration': '6-8 weeks',
            'tasks': [
                'Extract grouped services',
                'Implement inter-service communication',
                'Set up monitoring and logging',
                'Performance optimization'
            ]
        },
        {
            'phase': 4,
            'name': 'Cleanup and Optimization',
            'description': 'Final cleanup and optimization',
            'duration': '2-3 weeks',
            'tasks': [
                'Remove unused code',
                'Optimize shared components',
                'Update documentation',
                'Performance testing'
            ]
        }
    ]
    
    return recommendations

def group_related_services(services):
    """Group related services together"""
    groups = []
    
    # Simple grouping based on name similarity
    for service in services:
        grouped = False
        for group in groups:
            if any(word in service['name'].lower() for word in group['name'].split('-')) or \
               any(word in group['name'].lower() for word in service['name'].split('-')):
                group['services'].append(service)
                grouped = True
                break
        
        if not grouped:
            groups.append({
                'name': service['name'],
                'services': [service]
            })
    
    return groups

def determine_tech_stack(service):
    """Determine recommended tech stack for a service"""
    # This would be more sophisticated in a real implementation
    return {
        'language': 'Java',
        'framework': 'Spring Boot',
        'build_tool': 'Maven',
        'database': 'PostgreSQL',
        'cache': 'Redis',
        'message_queue': 'RabbitMQ'
    }

def main():
    with open('$structure_file', 'r') as f:
        data = json.load(f)
    
    structure = data.get('structure', {})
    summary = data.get('summary', {})
    
    print("Identifying service boundaries...")
    services = identify_service_boundaries(structure)
    
    print("Generating repository recommendations...")
    recommendations = generate_repository_recommendations(services, summary)
    
    # Add analysis metadata
    recommendations['analysis_metadata'] = {
        'total_services_identified': len(services),
        'high_complexity_services': len([s for s in services if s['complexity_score'] > 15]),
        'medium_complexity_services': len([s for s in services if 5 < s['complexity_score'] <= 15]),
        'low_complexity_services': len([s for s in services if s['complexity_score'] <= 5]),
        'estimated_total_repositories': len(recommendations['target_repositories']) + len(recommendations['shared_components']),
        'estimated_migration_duration': '14-20 weeks',
        'estimated_effort_person_hours': len(services) * 40  # Rough estimate
    }
    
    with open('$output_file', 'w') as f:
        json.dump(recommendations, f, indent=2)
    
    print(f"Refactoring recommendations generated: $output_file")

if __name__ == "__main__":
    main()
EOF
    
    echo "$output_file"
}

# Function to generate HTML report
generate_html_report() {
    local structure_file=$1
    local recommendations_file=$2
    local output_file=$3
    
    print_color $BLUE "Generating HTML report..."
    
    python3 << EOF
import json
import os
from datetime import datetime

def generate_html_report(structure_file, recommendations_file, output_file):
    with open(structure_file, 'r') as f:
        structure_data = json.load(f)
    
    with open(recommendations_file, 'r') as f:
        recommendations_data = json.load(f)
    
    summary = structure_data.get('summary', {})
    tech_stack = structure_data.get('tech_stack', {})
    metadata = recommendations_data.get('analysis_metadata', {})
    
    html_content = f"""
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Bitbucket Repository Refactoring Analysis</title>
    <style>
        body {{
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            line-height: 1.6;
            margin: 0;
            padding: 20px;
            background-color: #f5f5f5;
        }}
        .container {{
            max-width: 1200px;
            margin: 0 auto;
            background: white;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 0 20px rgba(0,0,0,0.1);
        }}
        .header {{
            text-align: center;
            border-bottom: 3px solid #007acc;
            padding-bottom: 20px;
            margin-bottom: 30px;
        }}
        .header h1 {{
            color: #007acc;
            margin: 0;
            font-size: 2.5em;
        }}
        .header p {{
            color: #666;
            font-size: 1.2em;
            margin: 10px 0 0 0;
        }}
        .section {{
            margin: 30px 0;
            padding: 20px;
            border-left: 4px solid #007acc;
            background-color: #f9f9f9;
        }}
        .section h2 {{
            color: #007acc;
            margin-top: 0;
            font-size: 1.8em;
        }}
        .section h3 {{
            color: #333;
            margin-top: 25px;
            font-size: 1.4em;
        }}
        .metric-grid {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin: 20px 0;
        }}
        .metric-card {{
            background: white;
            padding: 20px;
            border-radius: 8px;
            text-align: center;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }}
        .metric-value {{
            font-size: 2em;
            font-weight: bold;
            color: #007acc;
        }}
        .metric-label {{
            color: #666;
            margin-top: 5px;
        }}
        .tech-stack {{
            display: flex;
            flex-wrap: wrap;
            gap: 10px;
            margin: 15px 0;
        }}
        .tech-tag {{
            background: #007acc;
            color: white;
            padding: 5px 15px;
            border-radius: 20px;
            font-size: 0.9em;
        }}
        .repository-card {{
            background: white;
            border: 1px solid #ddd;
            border-radius: 8px;
            padding: 20px;
            margin: 15px 0;
            box-shadow: 0 2px 5px rgba(0,0,0,0.1);
        }}
        .repository-card h4 {{
            color: #007acc;
            margin: 0 0 10px 0;
            font-size: 1.3em;
        }}
        .priority-high {{ border-left: 4px solid #e74c3c; }}
        .priority-medium {{ border-left: 4px solid #f39c12; }}
        .priority-low {{ border-left: 4px solid #27ae60; }}
        .migration-phase {{
            background: white;
            border: 1px solid #ddd;
            border-radius: 8px;
            padding: 20px;
            margin: 15px 0;
        }}
        .phase-number {{
            background: #007acc;
            color: white;
            width: 30px;
            height: 30px;
            border-radius: 50%;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-weight: bold;
            margin-right: 15px;
        }}
        .recommendation {{
            background: #e8f4fd;
            border: 1px solid #007acc;
            border-radius: 8px;
            padding: 20px;
            margin: 20px 0;
        }}
        .recommendation h4 {{
            color: #007acc;
            margin: 0 0 15px 0;
        }}
        .risk-indicator {{
            display: inline-block;
            padding: 5px 10px;
            border-radius: 15px;
            font-size: 0.9em;
            font-weight: bold;
        }}
        .risk-low {{ background: #d4edda; color: #155724; }}
        .risk-medium {{ background: #fff3cd; color: #856404; }}
        .risk-high {{ background: #f8d7da; color: #721c24; }}
        .footer {{
            text-align: center;
            margin-top: 40px;
            padding-top: 20px;
            border-top: 1px solid #ddd;
            color: #666;
        }}
        @media (max-width: 768px) {{
            .container {{ padding: 15px; }}
            .header h1 {{ font-size: 2em; }}
            .metric-grid {{ grid-template-columns: 1fr; }}
        }}
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🔧 Repository Refactoring Analysis</h1>
            <p>Bitbucket Repository Decomposition for Trunk-Based Development</p>
            <p><strong>Generated:</strong> {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</p>
        </div>

        <div class="section">
            <h2>📊 Repository Overview</h2>
            <div class="metric-grid">
                <div class="metric-card">
                    <div class="metric-value">{summary.get('total_files', 0):,}</div>
                    <div class="metric-label">Total Files</div>
                </div>
                <div class="metric-card">
                    <div class="metric-value">{summary.get('total_lines', 0):,}</div>
                    <div class="metric-label">Total Lines of Code</div>
                </div>
                <div class="metric-card">
                    <div class="metric-value">{summary.get('total_size_kb', 0):,} KB</div>
                    <div class="metric-label">Total Size</div>
                </div>
                <div class="metric-card">
                    <div class="metric-value">{metadata.get('total_services_identified', 0)}</div>
                    <div class="metric-label">Services Identified</div>
                </div>
            </div>
        </div>

        <div class="section">
            <h2>🛠️ Technology Stack</h2>
            <h3>Primary Languages</h3>
            <div class="tech-stack">
"""
    
    # Add primary languages
    for lang, count in summary.get('primary_languages', []):
        html_content += f'                <span class="tech-tag">{lang} ({count} files)</span>\n'
    
    html_content += """
            </div>
            <h3>Frameworks & Tools</h3>
            <div class="tech-stack">
"""
    
    # Add frameworks
    for framework, count in summary.get('primary_frameworks', []):
        html_content += f'                <span class="tech-tag">{framework} ({count} occurrences)</span>\n'
    
    html_content += f"""
            </div>
        </div>

        <div class="section">
            <h2>🎯 Refactoring Strategy</h2>
            <div class="recommendation">
                <h4>Strategy: {recommendations_data.get('strategy', 'microservices_decomposition').replace('_', ' ').title()}</h4>
                <p><strong>Estimated Effort:</strong> {recommendations_data.get('estimated_effort', 'medium').title()}</p>
                <p><strong>Risk Level:</strong> <span class="risk-indicator risk-{recommendations_data.get('risk_level', 'medium')}">{recommendations_data.get('risk_level', 'medium').title()}</span></p>
                <p><strong>Estimated Duration:</strong> {metadata.get('estimated_migration_duration', '14-20 weeks')}</p>
                <p><strong>Estimated Person Hours:</strong> {metadata.get('estimated_effort_person_hours', 0):,}</p>
            </div>
        </div>

        <div class="section">
            <h2>📦 Recommended Repository Structure</h2>
            <h3>Target Repositories ({len(recommendations_data.get('target_repositories', []))})</h3>
"""
    
    # Add target repositories
    for repo in recommendations_data.get('target_repositories', []):
        priority_class = f"priority-{repo.get('priority', 'medium')}"
        html_content += f"""
            <div class="repository-card {priority_class}">
                <h4>{repo.get('name', 'Unknown')}</h4>
                <p><strong>Description:</strong> {repo.get('description', 'No description')}</p>
                <p><strong>Source Path:</strong> {', '.join(repo.get('source_paths', [repo.get('source_path', 'Unknown')]))}</p>
                <p><strong>Complexity Score:</strong> {repo.get('complexity_score', 0):.1f}</p>
                <p><strong>File Count:</strong> {repo.get('file_count', 0):,}</p>
                <p><strong>Estimated Size:</strong> {repo.get('estimated_size_kb', 0):,} KB</p>
                <p><strong>Priority:</strong> {repo.get('priority', 'medium').title()}</p>
            </div>
"""
    
    html_content += f"""
            <h3>Shared Components ({len(recommendations_data.get('shared_components', []))})</h3>
"""
    
    # Add shared components
    for component in recommendations_data.get('shared_components', []):
        html_content += f"""
            <div class="repository-card">
                <h4>{component.get('name', 'Unknown')}</h4>
                <p><strong>Description:</strong> {component.get('description', 'No description')}</p>
                <p><strong>Type:</strong> {component.get('type', 'unknown').title()}</p>
                <p><strong>Estimated Size:</strong> {component.get('estimated_size_kb', 0):,} KB</p>
            </div>
"""
    
    html_content += """
        </div>

        <div class="section">
            <h2>🚀 Migration Plan</h2>
"""
    
    # Add migration phases
    for phase in recommendations_data.get('migration_plan', []):
        html_content += f"""
            <div class="migration-phase">
                <div>
                    <span class="phase-number">{phase.get('phase', 0)}</span>
                    <h4>{phase.get('name', 'Unknown Phase')}</h4>
                </div>
                <p><strong>Description:</strong> {phase.get('description', 'No description')}</p>
                <p><strong>Duration:</strong> {phase.get('duration', 'Unknown')}</p>
                <h5>Tasks:</h5>
                <ul>
"""
        
        for task in phase.get('tasks', []):
            html_content += f"                    <li>{task}</li>\n"
        
        html_content += """
                </ul>
            </div>
"""
    
    html_content += f"""
        </div>

        <div class="section">
            <h2>📈 Service Analysis Summary</h2>
            <div class="metric-grid">
                <div class="metric-card">
                    <div class="metric-value">{metadata.get('high_complexity_services', 0)}</div>
                    <div class="metric-label">High Complexity Services</div>
                </div>
                <div class="metric-card">
                    <div class="metric-value">{metadata.get('medium_complexity_services', 0)}</div>
                    <div class="metric-label">Medium Complexity Services</div>
                </div>
                <div class="metric-card">
                    <div class="metric-value">{metadata.get('low_complexity_services', 0)}</div>
                    <div class="metric-label">Low Complexity Services</div>
                </div>
                <div class="metric-card">
                    <div class="metric-value">{metadata.get('estimated_total_repositories', 0)}</div>
                    <div class="metric-label">Total Repositories</div>
                </div>
            </div>
        </div>

        <div class="footer">
            <p>Generated by Bitbucket Repository Refactoring Analysis Tool</p>
            <p>For questions or support, contact your development team</p>
        </div>
    </div>
</body>
</html>
"""
    
    with open(output_file, 'w') as f:
        f.write(html_content)
    
    print(f"HTML report generated: {output_file}")

if __name__ == "__main__":
    generate_html_report('$structure_file', '$recommendations_file', '$output_file')
EOF
}

# Function to generate implementation suggestions
generate_implementation_suggestions() {
    local output_file="implementation_suggestions.md"
    
    print_color $BLUE "Generating implementation suggestions..."
    
    cat > "$output_file" << 'EOF'
# Implementation Suggestions for Repository Refactoring

## 🎯 Overview
This document provides detailed implementation suggestions for refactoring your monolithic Bitbucket repository into multiple smaller repositories to enable trunk-based development.

## 📋 Pre-Migration Checklist

### 1. Infrastructure Setup
- [ ] Set up CI/CD pipelines for all target repositories
- [ ] Configure shared component repositories
- [ ] Establish coding standards and review processes
- [ ] Set up monitoring and logging infrastructure
- [ ] Configure service discovery and API gateway

### 2. Team Preparation
- [ ] Train team on microservices architecture patterns
- [ ] Establish communication protocols between services
- [ ] Define API contracts and versioning strategies
- [ ] Set up cross-team collaboration processes

### 3. Technical Preparation
- [ ] Identify and document all external dependencies
- [ ] Create comprehensive test suites
- [ ] Set up database migration strategies
- [ ] Plan data consistency and transaction management

## 🏗️ Implementation Phases

### Phase 1: Foundation (Weeks 1-3)
**Objective**: Establish the foundation for microservices architecture

#### Tasks:
1. **Create Shared Component Repositories**
   ```bash
   # Create shared libraries
   git clone <original-repo> shared-utilities
   git clone <original-repo> shared-models
   git clone <original-repo> infrastructure-config
   ```

2. **Set up CI/CD Pipelines**
   - Configure GitHub Actions/GitLab CI for each repository
   - Set up automated testing and deployment
   - Configure code quality checks and security scanning

3. **Establish Standards**
   - Define API design standards (OpenAPI/Swagger)
   - Set up code formatting and linting rules
   - Create documentation templates

#### Deliverables:
- [ ] Shared component repositories created
- [ ] CI/CD pipelines configured
- [ ] Coding standards documented
- [ ] Team training completed

### Phase 2: High Priority Services (Weeks 4-9)
**Objective**: Extract the most complex and independent services

#### Tasks:
1. **Service Extraction**
   - Identify service boundaries using domain-driven design
   - Extract service code into separate repositories
   - Implement API contracts and interfaces
   - Set up service-specific databases

2. **API Implementation**
   - Design RESTful APIs using OpenAPI
   - Implement API versioning strategies
   - Set up API documentation
   - Configure API rate limiting and authentication

3. **Data Migration**
   - Plan database schema changes
   - Implement data migration scripts
   - Set up data consistency checks
   - Configure backup and recovery procedures

#### Deliverables:
- [ ] High complexity services extracted
- [ ] APIs implemented and documented
- [ ] Data migration completed
- [ ] Service monitoring configured

### Phase 3: Medium Priority Services (Weeks 10-17)
**Objective**: Extract remaining services and implement inter-service communication

#### Tasks:
1. **Service Grouping**
   - Group related services by domain
   - Implement service-to-service communication
   - Set up event-driven architecture
   - Configure message queues and event streaming

2. **Integration Testing**
   - Implement end-to-end testing
   - Set up contract testing
   - Configure integration test environments
   - Implement chaos engineering practices

3. **Performance Optimization**
   - Implement caching strategies
   - Optimize database queries
   - Set up load balancing
   - Configure auto-scaling

#### Deliverables:
- [ ] Medium complexity services extracted
- [ ] Inter-service communication implemented
- [ ] Integration testing completed
- [ ] Performance optimization completed

### Phase 4: Cleanup and Optimization (Weeks 18-20)
**Objective**: Final cleanup and optimization

#### Tasks:
1. **Code Cleanup**
   - Remove unused code and dependencies
   - Optimize shared components
   - Refactor legacy code patterns
   - Update documentation

2. **Monitoring and Observability**
   - Implement distributed tracing
   - Set up centralized logging
   - Configure alerting and dashboards
   - Implement health checks

3. **Documentation and Training**
   - Update architecture documentation
   - Create operational runbooks
   - Conduct team training sessions
   - Document troubleshooting procedures

#### Deliverables:
- [ ] Code cleanup completed
- [ ] Monitoring and observability implemented
- [ ] Documentation updated
- [ ] Team training completed

## 🛠️ Technical Implementation Details

### Repository Structure
Each microservice repository should follow this structure:
```
service-name/
├── src/
│   ├── main/
│   │   ├── java/          # Source code
│   │   └── resources/     # Configuration files
│   └── test/              # Test code
├── docs/                  # Documentation
├── scripts/               # Deployment scripts
├── docker/                # Docker configuration
├── .github/               # CI/CD workflows
├── README.md
├── pom.xml                # Build configuration
└── Dockerfile
```

### API Design Standards
- Use RESTful API design principles
- Implement OpenAPI/Swagger documentation
- Use semantic versioning for APIs
- Implement proper HTTP status codes
- Use consistent error response formats

### Database Strategy
- Each service should have its own database
- Use database-per-service pattern
- Implement eventual consistency where needed
- Use database migration tools
- Set up database monitoring and backup

### Communication Patterns
- Use synchronous communication for real-time operations
- Use asynchronous communication for event processing
- Implement circuit breakers for resilience
- Use API gateways for external communication
- Implement service mesh for internal communication

## 🔧 Tools and Technologies

### CI/CD Tools
- **GitHub Actions** or **GitLab CI** for pipeline automation
- **Docker** for containerization
- **Kubernetes** for orchestration
- **Helm** for package management

### Monitoring and Observability
- **Prometheus** for metrics collection
- **Grafana** for visualization
- **Jaeger** for distributed tracing
- **ELK Stack** for centralized logging

### API Management
- **Kong** or **Istio** for API gateway
- **Swagger/OpenAPI** for API documentation
- **Postman** for API testing

### Database Tools
- **Flyway** or **Liquibase** for migrations
- **pgAdmin** or **MySQL Workbench** for management
- **Redis** for caching

## 📊 Success Metrics

### Development Metrics
- **Deployment Frequency**: Target daily deployments
- **Lead Time**: Reduce from weeks to hours
- **Mean Time to Recovery**: Target < 1 hour
- **Change Failure Rate**: Target < 5%

### Quality Metrics
- **Code Coverage**: Target > 80%
- **Technical Debt**: Monitor and reduce over time
- **Security Vulnerabilities**: Zero critical vulnerabilities
- **Performance**: Response time < 200ms

### Team Metrics
- **Developer Productivity**: Measure story points per sprint
- **Code Review Time**: Target < 24 hours
- **Bug Resolution Time**: Target < 2 days
- **Team Satisfaction**: Regular surveys

## 🚨 Risk Mitigation

### Technical Risks
- **Data Consistency**: Implement eventual consistency patterns
- **Service Dependencies**: Use circuit breakers and timeouts
- **Performance**: Implement caching and optimization
- **Security**: Implement proper authentication and authorization

### Process Risks
- **Team Coordination**: Establish clear communication protocols
- **Knowledge Transfer**: Document everything and conduct training
- **Timeline Delays**: Build in buffer time and prioritize features
- **Quality Issues**: Implement comprehensive testing strategies

## 📞 Support and Resources

### Documentation
- [Microservices Architecture Patterns](https://microservices.io/)
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [Trunk-Based Development](https://trunkbaseddevelopment.com/)

### Tools and Frameworks
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Docker](https://www.docker.com/)
- [Kubernetes](https://kubernetes.io/)
- [Prometheus](https://prometheus.io/)

### Training Resources
- Microservices architecture courses
- Domain-driven design workshops
- CI/CD pipeline training
- Container orchestration training

---

**Note**: This implementation plan should be customized based on your specific requirements, team size, and technical constraints. Regular reviews and adjustments are recommended throughout the migration process.
EOF

    echo "$output_file"
}

# Main execution
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    if [ $# -eq 0 ]; then
        print_color $RED "Error: Bitbucket repository URL required"
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
            --depth)
                ANALYSIS_DEPTH="$2"
                shift 2
                ;;
            --min-size)
                MIN_REPO_SIZE="$2"
                shift 2
                ;;
            --max-size)
                MAX_REPO_SIZE="$2"
                shift 2
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
            OUTPUT_FILE="refactoring_analysis.html"
        elif [ "$OUTPUT_FORMAT" = "json" ]; then
            OUTPUT_FILE="refactoring_analysis.json"
        else
            OUTPUT_FILE="refactoring_analysis.md"
        fi
    fi

    # Check dependencies
    check_dependencies

    print_color $GREEN "🚀 Starting Bitbucket Repository Refactoring Analysis"
    print_color $BLUE "Repository: $REPO_URL"
    print_color $BLUE "Output format: $OUTPUT_FORMAT"
    print_color $BLUE "Output file: $OUTPUT_FILE"
    print_color $BLUE "Analysis depth: $ANALYSIS_DEPTH"

    # Clone repository
    REPO_PATH=$(clone_repository "$REPO_URL")

    # Analyze repository structure
    STRUCTURE_FILE=$(analyze_repository_structure "$REPO_PATH")

    # Generate refactoring recommendations
    RECOMMENDATIONS_FILE=$(generate_refactoring_recommendations "$STRUCTURE_FILE")

    # Generate implementation suggestions if requested
    if [ "$SUGGESTIONS" = true ]; then
        IMPLEMENTATION_FILE=$(generate_implementation_suggestions)
    fi

    # Generate final report
    if [ "$OUTPUT_FORMAT" = "html" ]; then
        generate_html_report "$STRUCTURE_FILE" "$RECOMMENDATIONS_FILE" "$OUTPUT_FILE"
    elif [ "$OUTPUT_FORMAT" = "json" ]; then
        cp "$RECOMMENDATIONS_FILE" "$OUTPUT_FILE"
    else
        # Generate markdown report
        cat > "$OUTPUT_FILE" << EOF
# Bitbucket Repository Refactoring Analysis

## Overview
This analysis was generated for repository: $REPO_URL

## Files Generated
- Repository Structure: $STRUCTURE_FILE
- Refactoring Recommendations: $RECOMMENDATIONS_FILE
EOF
        if [ "$SUGGESTIONS" = true ]; then
            echo "- Implementation Suggestions: $IMPLEMENTATION_FILE" >> "$OUTPUT_FILE"
        fi
    fi

    # Cleanup
    rm -rf "$REPO_PATH"

    print_color $GREEN "✅ Analysis complete!"
    print_color $GREEN "📄 Report saved to: $OUTPUT_FILE"
    print_color $GREEN "📊 Structure analysis: $STRUCTURE_FILE"
    print_color $GREEN "🎯 Recommendations: $RECOMMENDATIONS_FILE"
    if [ "$SUGGESTIONS" = true ]; then
        print_color $GREEN "📋 Implementation suggestions: $IMPLEMENTATION_FILE"
    fi
    print_color $BLUE "🔧 Use these recommendations to plan your repository refactoring strategy"
fi
