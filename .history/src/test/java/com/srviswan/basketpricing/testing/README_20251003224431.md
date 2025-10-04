# Testing Suite Documentation

This directory contains comprehensive testing scripts for the basket pricing service, including regression, performance, load, and stress tests.

## Test Suites Overview

### 1. RegressionTestSuite.java
**Purpose**: Comprehensive regression testing to ensure system stability and functionality.

**Features**:
- API endpoint validation
- Error handling verification
- Edge case testing
- Concurrent access testing
- Data consistency validation
- Performance regression detection

**Test Categories**:
- **API Endpoint Tests**: GET, POST, DELETE operations
- **Error Handling Tests**: Invalid symbols, empty requests, malformed requests
- **Edge Case Tests**: Large symbol lists, special characters, duplicate symbols
- **Concurrent Access Tests**: Race conditions, simultaneous operations
- **Data Consistency Tests**: Subscription consistency, price data validation
- **Performance Regression Tests**: Response time and memory usage monitoring

### 2. PerformanceTestSuite.java
**Purpose**: Performance benchmarking and optimization validation.

**Features**:
- Response time measurement
- Throughput analysis
- Resource utilization monitoring
- Connection pooling evaluation
- Memory and CPU usage tracking

**Test Categories**:
- **Latency Tests**: Single request, concurrent request latency
- **Throughput Tests**: Requests per second under various loads
- **Resource Tests**: Memory, CPU, network utilization
- **Connection Tests**: Pooling, keep-alive performance
- **Data Volume Tests**: Large payload handling
- **Streaming Tests**: Real-time update performance

### 3. LoadTestSuite.java
**Purpose**: Load testing with concurrent users to validate system behavior under normal and peak loads.

**Features**:
- Concurrent user simulation
- Gradual load increase
- Load spike testing
- Sustained load validation
- Recovery testing

**Test Categories**:
- **Light Load**: 10 concurrent users
- **Medium Load**: 50 concurrent users
- **Heavy Load**: 100 concurrent users
- **Peak Load**: 200 concurrent users
- **Gradual Increase**: Step-by-step load escalation
- **Load Spikes**: Sudden load increases
- **Sustained Load**: Long-running load tests
- **Recovery Tests**: System recovery after high load

### 4. StressTestSuite.java
**Purpose**: Stress testing to identify system breaking points and limits.

**Features**:
- Extreme load conditions
- Resource exhaustion testing
- System limit identification
- Recovery validation
- Breaking point analysis

**Test Categories**:
- **Memory Stress**: Memory-intensive operations
- **CPU Stress**: CPU-intensive processing
- **Network Stress**: High network utilization
- **Connection Stress**: Maximum concurrent connections
- **Data Volume Stress**: Large data processing
- **Rate Limit Stress**: Rate limiting validation
- **System Limits**: Resource limit testing
- **Recovery Tests**: System recovery after stress

### 5. TestAutomationSuite.java
**Purpose**: Test orchestration and CI/CD integration.

**Features**:
- Automated test execution
- Parallel test execution
- Test reporting
- CI/CD integration
- Smoke testing

**Test Categories**:
- **Sequential Execution**: Ordered test suite execution
- **Parallel Execution**: Concurrent test execution
- **Smoke Tests**: Quick validation tests
- **Report Generation**: HTML, JSON, XML, CSV reports
- **CI/CD Integration**: GitHub Actions workflow

## Running the Tests

### Prerequisites
1. **Java 21** SDK installed
2. **Maven 3.8+** installed
3. **Basket Pricing Service** running on `localhost:8080`
4. **Refinitiv EMA** connection (for full functionality)

### Individual Test Execution

#### Regression Tests
```bash
# Compile test classes
mvn test-compile

# Run regression tests
java -cp "target/classes:target/test-classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" \
     com.srviswan.basketpricing.testing.RegressionTestSuite
```

#### Performance Tests
```bash
# Run performance tests
java -cp "target/classes:target/test-classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" \
     com.srviswan.basketpricing.testing.PerformanceTestSuite
```

#### Load Tests
```bash
# Run load tests
java -cp "target/classes:target/test-classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" \
     com.srviswan.basketpricing.testing.LoadTestSuite
```

#### Stress Tests
```bash
# Run stress tests
java -cp "target/classes:target/test-classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" \
     com.srviswan.basketpricing.testing.StressTestSuite
```

#### Test Automation Suite
```bash
# Run complete test automation suite
java -cp "target/classes:target/test-classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" \
     com.srviswan.basketpricing.testing.TestAutomationSuite
```

### Using Maven
```bash
# Run all unit tests
mvn test

# Run specific test suite
mvn test -Dtest=RegressionTestSuite

# Run tests with specific profile
mvn test -Ptesting

# Run tests with debug output
mvn test -X
```

## Test Configuration

### Environment Variables
```bash
# Refinitiv EMA Configuration
export REFINITIV_HOST=your_host
export REFINITIV_PORT=14002
export REFINITIV_USER=your_user
export REFINITIV_SERVICE=ELEKTRON_DD

# Test Configuration
export TEST_TIMEOUT=300000
export TEST_CONCURRENT_USERS=50
export TEST_DURATION=300
```

### Test Properties
Create `src/test/resources/test.properties`:
```properties
# Test Configuration
test.timeout=300000
test.concurrent.users=50
test.duration=300
test.ramp.up=30

# Service Configuration
service.host=localhost
service.port=8080
service.grpc.port=9090

# Load Test Configuration
load.test.light.users=10
load.test.medium.users=50
load.test.heavy.users=100
load.test.peak.users=200

# Performance Test Configuration
performance.test.iterations=100
performance.test.warmup=10
performance.test.measurement=90
```

## Test Reports

### Generated Reports
The test suites generate multiple report formats:

1. **HTML Report** (`test-report.html`)
   - Interactive web-based report
   - Test results with pass/fail status
   - Duration and performance metrics

2. **JSON Report** (`test-report.json`)
   - Machine-readable format
   - Structured test results
   - Easy integration with other tools

3. **XML Report** (`test-report.xml`)
   - JUnit format compatible
   - CI/CD integration
   - Standard test reporting

4. **CSV Report** (`test-report.csv`)
   - Spreadsheet compatible
   - Data analysis friendly
   - Performance trending

### Report Location
Reports are generated in the project root directory:
```
basket-pricing-service/
├── test-report.html
├── test-report.json
├── test-report.xml
└── test-report.csv
```

## CI/CD Integration

### GitHub Actions
The project includes a comprehensive GitHub Actions workflow (`.github/workflows/test-automation.yml`) that:

1. **Runs on**: Push to main/develop, pull requests, daily schedule
2. **Executes**: All test suites in sequence
3. **Generates**: Test reports and artifacts
4. **Deploys**: Docker containers for testing
5. **Notifies**: Success/failure notifications

### Workflow Jobs
1. **Unit Tests**: Basic functionality validation
2. **Integration Tests**: Service integration testing
3. **Regression Tests**: Comprehensive regression validation
4. **Performance Tests**: Performance benchmarking
5. **Load Tests**: Load testing with concurrent users
6. **Stress Tests**: System limit testing
7. **Test Automation**: Orchestrated test execution
8. **Security Tests**: Security vulnerability scanning
9. **Code Quality**: Code quality checks
10. **Build and Package**: Application packaging
11. **Docker Build**: Container image creation
12. **Deployment Tests**: Deployment validation
13. **Notification**: Test result notifications

### Local CI/CD Simulation
```bash
# Run complete CI/CD pipeline locally
./scripts/run-ci-pipeline.sh

# Run specific CI/CD phase
./scripts/run-ci-phase.sh regression
./scripts/run-ci-phase.sh performance
./scripts/run-ci-phase.sh load
./scripts/run-ci-phase.sh stress
```

## Test Data Management

### Test Data Setup
```bash
# Create test data directory
mkdir -p src/test/resources/data

# Generate test symbols
./scripts/generate-test-symbols.sh

# Setup test database
./scripts/setup-test-database.sh
```

### Test Data Cleanup
```bash
# Cleanup test data
./scripts/cleanup-test-data.sh

# Reset test environment
./scripts/reset-test-environment.sh
```

## Performance Benchmarks

### Expected Performance Metrics
Based on the test suites, the service should achieve:

#### Response Time
- **P50**: < 100ms
- **P90**: < 200ms
- **P95**: < 300ms
- **P99**: < 500ms

#### Throughput
- **Light Load**: > 100 req/sec
- **Medium Load**: > 200 req/sec
- **Heavy Load**: > 300 req/sec
- **Peak Load**: > 400 req/sec

#### Resource Utilization
- **CPU**: < 80% under normal load
- **Memory**: < 85% under normal load
- **Network**: < 90% under normal load

#### Error Rates
- **Normal Load**: < 0.1%
- **Heavy Load**: < 1%
- **Peak Load**: < 5%

## Troubleshooting

### Common Issues

#### 1. Connection Refused
**Problem**: Tests fail with connection refused errors
**Solution**: Ensure the pricing service is running on `localhost:8080`

#### 2. Timeout Errors
**Problem**: Tests timeout during execution
**Solution**: Increase timeout values in test configuration

#### 3. Memory Issues
**Problem**: OutOfMemoryError during test execution
**Solution**: Increase JVM heap size: `-Xmx2048m`

#### 4. Refinitiv Connection Issues
**Problem**: EMA connection failures
**Solution**: Verify Refinitiv credentials and network connectivity

### Debug Mode
```bash
# Enable debug logging
export LOG_LEVEL=DEBUG

# Run tests with debug output
mvn test -X

# Run specific test with debug
java -Dlogback.configurationFile=logback-test.xml \
     -cp "target/classes:target/test-classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" \
     com.srviswan.basketpricing.testing.RegressionTestSuite
```

### Test Environment Validation
```bash
# Validate test environment
./scripts/validate-test-environment.sh

# Check service health
curl -f http://localhost:8080/actuator/health

# Check gRPC service
grpcurl -plaintext localhost:9090 list
```

## Contributing

### Adding New Tests
1. Create test class in appropriate package
2. Follow naming convention: `*TestSuite.java`
3. Implement comprehensive test coverage
4. Add test documentation
5. Update CI/CD pipeline if needed

### Test Guidelines
1. **Isolation**: Tests should be independent
2. **Deterministic**: Tests should produce consistent results
3. **Fast**: Tests should complete quickly
4. **Reliable**: Tests should not flake
5. **Maintainable**: Tests should be easy to understand and modify

### Code Review
1. Review test coverage
2. Validate test logic
3. Check performance impact
4. Verify CI/CD integration
5. Update documentation

## Support

For issues with the testing suite:
1. Check the troubleshooting section
2. Review test logs and reports
3. Validate test environment
4. Contact the development team

## License

This testing suite is part of the basket pricing service project and follows the same license terms.
