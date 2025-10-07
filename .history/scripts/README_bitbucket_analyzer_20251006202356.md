# Bitbucket Repository Refactoring Analyzer

A comprehensive script to analyze Bitbucket repositories and propose recommendations for refactoring monolithic repositories into multiple smaller repositories to enable trunk-based development.

## 🎯 Purpose

This tool helps development teams:
- **Analyze** monolithic repository structure and complexity
- **Identify** service boundaries and extraction opportunities
- **Recommend** repository splitting strategies
- **Plan** migration to microservices architecture
- **Enable** trunk-based development practices

## 🚀 Features

### Repository Analysis
- **Structure Analysis**: Deep analysis of directory structure and file organization
- **Technology Stack Detection**: Automatic detection of programming languages, frameworks, and tools
- **Complexity Scoring**: Calculate complexity scores for different parts of the codebase
- **Service Boundary Identification**: Identify potential microservice boundaries
- **Dependency Analysis**: Analyze project dependencies and relationships

### Refactoring Recommendations
- **Repository Splitting Strategy**: Recommend how to split the monolithic repository
- **Service Grouping**: Group related services by domain and complexity
- **Migration Planning**: Detailed phase-by-phase migration plan
- **Risk Assessment**: Evaluate risks and provide mitigation strategies
- **Effort Estimation**: Estimate time and effort required for migration

### Output Formats
- **HTML Report**: Comprehensive visual report with charts and recommendations
- **JSON Output**: Machine-readable data for integration with other tools
- **Markdown Report**: Human-readable documentation format

## 📋 Prerequisites

- **Git**: For cloning repositories
- **Python 3**: For analysis scripts
- **curl**: For API calls (if needed)
- **Bash**: For script execution

## 🛠️ Installation

1. **Clone or download** the script to your local machine
2. **Make it executable**:
   ```bash
   chmod +x scripts/bitbucket_refactoring_analyzer.sh
   ```
3. **Verify dependencies**:
   ```bash
   ./scripts/bitbucket_refactoring_analyzer.sh --help
   ```

## 📖 Usage

### Basic Usage
```bash
./scripts/bitbucket_refactoring_analyzer.sh https://bitbucket.org/workspace/repo.git
```

### Advanced Usage
```bash
# Detailed analysis with suggestions
./scripts/bitbucket_refactoring_analyzer.sh https://bitbucket.org/workspace/repo.git \
  --detailed --suggestions --depth deep

# Custom output format and file
./scripts/bitbucket_refactoring_analyzer.sh https://bitbucket.org/workspace/repo.git \
  --format json --output custom_analysis.json

# Custom size constraints
./scripts/bitbucket_refactoring_analyzer.sh https://bitbucket.org/workspace/repo.git \
  --min-size 500 --max-size 100000
```

### Command Line Options

| Option | Description | Default |
|--------|-------------|---------|
| `-o, --output FILE` | Output file path | `refactoring_analysis.html` |
| `-f, --format FORMAT` | Output format (html, json, markdown) | `html` |
| `-d, --detailed` | Include detailed analysis | `false` |
| `-s, --suggestions` | Include implementation suggestions | `false` |
| `--depth LEVEL` | Analysis depth (shallow, medium, deep) | `medium` |
| `--min-size SIZE` | Minimum repository size in KB | `1000` |
| `--max-size SIZE` | Maximum repository size in KB | `50000` |
| `-h, --help` | Show help message | - |

## 📊 Output Files

The script generates several output files:

### 1. Main Report
- **HTML**: `refactoring_analysis.html` - Comprehensive visual report
- **JSON**: `refactoring_analysis.json` - Machine-readable data
- **Markdown**: `refactoring_analysis.md` - Documentation format

### 2. Analysis Data
- **`repo_structure.json`**: Detailed repository structure analysis
- **`refactoring_recommendations.json`**: Service extraction recommendations

### 3. Implementation Guide (if `--suggestions` is used)
- **`implementation_suggestions.md`**: Detailed implementation guide

## 🎯 Analysis Process

### 1. Repository Cloning
- Clones the target Bitbucket repository
- Creates temporary analysis directory
- Handles authentication if required

### 2. Structure Analysis
- **File System Analysis**: Analyzes directory structure and file organization
- **Technology Detection**: Identifies programming languages, frameworks, and tools
- **Complexity Calculation**: Calculates complexity scores for different components
- **Dependency Mapping**: Maps dependencies and relationships

### 3. Service Boundary Identification
- **Domain Analysis**: Identifies business domains and service boundaries
- **Coupling Analysis**: Analyzes coupling between different parts of the codebase
- **Cohesion Assessment**: Evaluates cohesion within potential services
- **API Detection**: Identifies existing API endpoints and interfaces

### 4. Refactoring Recommendations
- **Service Extraction**: Recommends which services to extract
- **Repository Structure**: Proposes new repository organization
- **Migration Strategy**: Develops phase-by-phase migration plan
- **Risk Assessment**: Identifies risks and mitigation strategies

## 📈 Metrics and Scoring

### Complexity Scoring
The script uses a multi-factor complexity scoring system:

- **File Count**: Number of files in a directory
- **Lines of Code**: Total lines of code
- **Size**: Total file size in KB
- **Depth**: Directory nesting depth
- **Dependencies**: Number of external dependencies

### Service Classification
Services are classified based on complexity scores:

- **High Complexity** (>15): Immediate extraction recommended
- **Medium Complexity** (5-15): Group with related services
- **Low Complexity** (<5): Consider keeping in monolith or grouping

## 🏗️ Refactoring Strategies

### 1. Microservices Decomposition
- Extract independent services
- Implement API-first design
- Use domain-driven design principles
- Implement service mesh architecture

### 2. Repository Organization
- **Service Repositories**: One repository per microservice
- **Shared Components**: Common libraries and utilities
- **Infrastructure**: Configuration and deployment scripts
- **Documentation**: Centralized documentation repository

### 3. Migration Phases
- **Phase 1**: Foundation setup (2-3 weeks)
- **Phase 2**: High priority services (4-6 weeks)
- **Phase 3**: Medium priority services (6-8 weeks)
- **Phase 4**: Cleanup and optimization (2-3 weeks)

## 🔧 Technology Recommendations

### Backend Technologies
- **Java**: Spring Boot, Micronaut
- **Python**: FastAPI, Django, Flask
- **Node.js**: Express, NestJS
- **Go**: Gin, Echo
- **C#**: ASP.NET Core

### Infrastructure
- **Containerization**: Docker, Podman
- **Orchestration**: Kubernetes, Docker Swarm
- **CI/CD**: GitHub Actions, GitLab CI, Jenkins
- **Monitoring**: Prometheus, Grafana, Jaeger

### Databases
- **Relational**: PostgreSQL, MySQL
- **NoSQL**: MongoDB, Redis
- **Search**: Elasticsearch
- **Message Queues**: RabbitMQ, Apache Kafka

## 📋 Implementation Checklist

### Pre-Migration
- [ ] Set up CI/CD pipelines
- [ ] Configure shared components
- [ ] Establish coding standards
- [ ] Set up monitoring infrastructure
- [ ] Train development team

### During Migration
- [ ] Extract high complexity services first
- [ ] Implement API contracts
- [ ] Set up service discovery
- [ ] Migrate data and dependencies
- [ ] Implement comprehensive testing

### Post-Migration
- [ ] Remove unused code
- [ ] Optimize shared components
- [ ] Update documentation
- [ ] Conduct performance testing
- [ ] Monitor system health

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

## 🔍 Example Output

### HTML Report Features
- **Repository Overview**: Key metrics and statistics
- **Technology Stack**: Detected languages and frameworks
- **Refactoring Strategy**: Recommended approach and timeline
- **Repository Structure**: Proposed new organization
- **Migration Plan**: Phase-by-phase implementation plan
- **Risk Assessment**: Identified risks and mitigation strategies

### JSON Output Structure
```json
{
  "strategy": "microservices_decomposition",
  "target_repositories": [
    {
      "name": "user-service",
      "description": "User management microservice",
      "complexity_score": 18.5,
      "priority": "high",
      "estimated_size_kb": 15000
    }
  ],
  "shared_components": [
    {
      "name": "common-utilities",
      "type": "library",
      "estimated_size_kb": 5000
    }
  ],
  "migration_plan": [
    {
      "phase": 1,
      "name": "Preparation",
      "duration": "2-3 weeks"
    }
  ]
}
```

## 🤝 Contributing

To contribute to this tool:

1. **Fork** the repository
2. **Create** a feature branch
3. **Make** your changes
4. **Test** thoroughly
5. **Submit** a pull request

## 📞 Support

For questions or support:
- **Documentation**: Check this README and generated reports
- **Issues**: Report bugs and feature requests
- **Community**: Join discussions and share experiences

## 📄 License

This tool is provided as-is for educational and development purposes. Please review and adapt the recommendations based on your specific requirements and constraints.

---

**Note**: This tool provides recommendations based on automated analysis. Always review and validate recommendations with your development team before implementing changes.
