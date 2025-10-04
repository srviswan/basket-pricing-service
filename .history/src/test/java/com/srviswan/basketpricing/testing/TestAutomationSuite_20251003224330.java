package com.srviswan.basketpricing.testing;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Test automation suite that orchestrates all testing phases.
 * Provides CI/CD integration, test reporting, and automated test execution.
 */
@Slf4j
public class TestAutomationSuite {

    private final TestResults results = new TestResults();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public static void main(String[] args) {
        try {
            log.info("Starting Test Automation Suite");
            
            TestAutomationSuite suite = new TestAutomationSuite();
            boolean allTestsPassed = suite.runAllTestSuites();
            
            suite.generateReports();
            suite.shutdown();
            
            if (allTestsPassed) {
                log.info("All test suites PASSED");
                System.exit(0);
            } else {
                log.error("Some test suites FAILED");
                System.exit(1);
            }
            
        } catch (Exception e) {
            log.error("Test automation suite failed", e);
            System.exit(1);
        }
    }

    public boolean runAllTestSuites() {
        log.info("=== Running Test Automation Suite ===");
        
        Instant startTime = Instant.now();
        
        // Phase 1: Unit Tests
        boolean unitTestsPassed = runUnitTests();
        
        // Phase 2: Integration Tests
        boolean integrationTestsPassed = runIntegrationTests();
        
        // Phase 3: Regression Tests
        boolean regressionTestsPassed = runRegressionTests();
        
        // Phase 4: Performance Tests
        boolean performanceTestsPassed = runPerformanceTests();
        
        // Phase 5: Load Tests
        boolean loadTestsPassed = runLoadTests();
        
        // Phase 6: Stress Tests
        boolean stressTestsPassed = runStressTests();
        
        Instant endTime = Instant.now();
        Duration totalDuration = Duration.between(startTime, endTime);
        
        // Record overall results
        results.recordOverallResult("Total Duration", totalDuration);
        results.recordOverallResult("Unit Tests", unitTestsPassed);
        results.recordOverallResult("Integration Tests", integrationTestsPassed);
        results.recordOverallResult("Regression Tests", regressionTestsPassed);
        results.recordOverallResult("Performance Tests", performanceTestsPassed);
        results.recordOverallResult("Load Tests", loadTestsPassed);
        results.recordOverallResult("Stress Tests", stressTestsPassed);
        
        boolean allTestsPassed = unitTestsPassed && integrationTestsPassed && 
                                regressionTestsPassed && performanceTestsPassed && 
                                loadTestsPassed && stressTestsPassed;
        
        log.info("Test automation completed in {}ms", totalDuration.toMillis());
        log.info("Overall result: {}", allTestsPassed ? "PASSED" : "FAILED");
        
        return allTestsPassed;
    }

    /**
     * Run unit tests
     */
    private boolean runUnitTests() {
        log.info("=== Phase 1: Unit Tests ===");
        
        try {
            Instant startTime = Instant.now();
            
            // Run unit tests using Maven
            ProcessBuilder pb = new ProcessBuilder("mvn", "test", "-Dtest=*Test", "-DfailIfNoTests=false");
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);
            
            boolean passed = exitCode == 0;
            results.recordTestSuite("Unit Tests", passed, duration);
            
            log.info("Unit tests {} in {}ms", passed ? "PASSED" : "FAILED", duration.toMillis());
            return passed;
            
        } catch (Exception e) {
            log.error("Unit tests failed", e);
            results.recordTestSuite("Unit Tests", false, Duration.ZERO);
            return false;
        }
    }

    /**
     * Run integration tests
     */
    private boolean runIntegrationTests() {
        log.info("=== Phase 2: Integration Tests ===");
        
        try {
            Instant startTime = Instant.now();
            
            // Run integration tests
            ProcessBuilder pb = new ProcessBuilder("mvn", "test", "-Dtest=*IntegrationTest", "-DfailIfNoTests=false");
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);
            
            boolean passed = exitCode == 0;
            results.recordTestSuite("Integration Tests", passed, duration);
            
            log.info("Integration tests {} in {}ms", passed ? "PASSED" : "FAILED", duration.toMillis());
            return passed;
            
        } catch (Exception e) {
            log.error("Integration tests failed", e);
            results.recordTestSuite("Integration Tests", false, Duration.ZERO);
            return false;
        }
    }

    /**
     * Run regression tests
     */
    private boolean runRegressionTests() {
        log.info("=== Phase 3: Regression Tests ===");
        
        try {
            Instant startTime = Instant.now();
            
            // Run regression test suite
            RegressionTestSuite regressionSuite = new RegressionTestSuite();
            boolean passed = regressionSuite.runAllTests();
            
            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);
            
            results.recordTestSuite("Regression Tests", passed, duration);
            
            log.info("Regression tests {} in {}ms", passed ? "PASSED" : "FAILED", duration.toMillis());
            return passed;
            
        } catch (Exception e) {
            log.error("Regression tests failed", e);
            results.recordTestSuite("Regression Tests", false, Duration.ZERO);
            return false;
        }
    }

    /**
     * Run performance tests
     */
    private boolean runPerformanceTests() {
        log.info("=== Phase 4: Performance Tests ===");
        
        try {
            Instant startTime = Instant.now();
            
            // Run performance test suite
            PerformanceTestSuite performanceSuite = new PerformanceTestSuite();
            performanceSuite.runAllPerformanceTests();
            
            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);
            
            // Performance tests are considered passed if they complete without exceptions
            boolean passed = true;
            results.recordTestSuite("Performance Tests", passed, duration);
            
            log.info("Performance tests {} in {}ms", passed ? "PASSED" : "FAILED", duration.toMillis());
            return passed;
            
        } catch (Exception e) {
            log.error("Performance tests failed", e);
            results.recordTestSuite("Performance Tests", false, Duration.ZERO);
            return false;
        }
    }

    /**
     * Run load tests
     */
    private boolean runLoadTests() {
        log.info("=== Phase 5: Load Tests ===");
        
        try {
            Instant startTime = Instant.now();
            
            // Run load test suite
            LoadTestSuite loadSuite = new LoadTestSuite();
            loadSuite.runAllLoadTests();
            
            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);
            
            // Load tests are considered passed if they complete without exceptions
            boolean passed = true;
            results.recordTestSuite("Load Tests", passed, duration);
            
            log.info("Load tests {} in {}ms", passed ? "PASSED" : "FAILED", duration.toMillis());
            return passed;
            
        } catch (Exception e) {
            log.error("Load tests failed", e);
            results.recordTestSuite("Load Tests", false, Duration.ZERO);
            return false;
        }
    }

    /**
     * Run stress tests
     */
    private boolean runStressTests() {
        log.info("=== Phase 6: Stress Tests ===");
        
        try {
            Instant startTime = Instant.now();
            
            // Run stress test suite
            StressTestSuite stressSuite = new StressTestSuite();
            stressSuite.runAllStressTests();
            
            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);
            
            // Stress tests are considered passed if they complete without exceptions
            boolean passed = true;
            results.recordTestSuite("Stress Tests", passed, duration);
            
            log.info("Stress tests {} in {}ms", passed ? "PASSED" : "FAILED", duration.toMillis());
            return passed;
            
        } catch (Exception e) {
            log.error("Stress tests failed", e);
            results.recordTestSuite("Stress Tests", false, Duration.ZERO);
            return false;
        }
    }

    /**
     * Run tests in parallel
     */
    public boolean runTestsInParallel() {
        log.info("=== Running Tests in Parallel ===");
        
        try {
            Instant startTime = Instant.now();
            
            // Start all test suites in parallel
            CompletableFuture<Boolean> unitTestsFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return runUnitTests();
                } catch (Exception e) {
                    log.error("Unit tests failed in parallel execution", e);
                    return false;
                }
            }, executor);
            
            CompletableFuture<Boolean> integrationTestsFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return runIntegrationTests();
                } catch (Exception e) {
                    log.error("Integration tests failed in parallel execution", e);
                    return false;
                }
            }, executor);
            
            CompletableFuture<Boolean> regressionTestsFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return runRegressionTests();
                } catch (Exception e) {
                    log.error("Regression tests failed in parallel execution", e);
                    return false;
                }
            }, executor);
            
            CompletableFuture<Boolean> performanceTestsFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return runPerformanceTests();
                } catch (Exception e) {
                    log.error("Performance tests failed in parallel execution", e);
                    return false;
                }
            }, executor);
            
            // Wait for all tests to complete
            CompletableFuture<Void> allTests = CompletableFuture.allOf(
                    unitTestsFuture, integrationTestsFuture, regressionTestsFuture, performanceTestsFuture);
            
            allTests.get(); // Wait for completion
            
            // Get results
            boolean unitTestsPassed = unitTestsFuture.get();
            boolean integrationTestsPassed = integrationTestsFuture.get();
            boolean regressionTestsPassed = regressionTestsFuture.get();
            boolean performanceTestsPassed = performanceTestsFuture.get();
            
            Instant endTime = Instant.now();
            Duration totalDuration = Duration.between(startTime, endTime);
            
            boolean allTestsPassed = unitTestsPassed && integrationTestsPassed && 
                                    regressionTestsPassed && performanceTestsPassed;
            
            results.recordOverallResult("Parallel Execution Duration", totalDuration);
            results.recordOverallResult("Parallel Unit Tests", unitTestsPassed);
            results.recordOverallResult("Parallel Integration Tests", integrationTestsPassed);
            results.recordOverallResult("Parallel Regression Tests", regressionTestsPassed);
            results.recordOverallResult("Parallel Performance Tests", performanceTestsPassed);
            
            log.info("Parallel test execution completed in {}ms", totalDuration.toMillis());
            log.info("Overall result: {}", allTestsPassed ? "PASSED" : "FAILED");
            
            return allTestsPassed;
            
        } catch (Exception e) {
            log.error("Parallel test execution failed", e);
            return false;
        }
    }

    /**
     * Run smoke tests (quick validation)
     */
    public boolean runSmokeTests() {
        log.info("=== Running Smoke Tests ===");
        
        try {
            Instant startTime = Instant.now();
            
            // Quick validation tests
            boolean serviceHealthCheck = checkServiceHealth();
            boolean basicApiTest = runBasicApiTest();
            boolean connectivityTest = runConnectivityTest();
            
            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);
            
            boolean passed = serviceHealthCheck && basicApiTest && connectivityTest;
            results.recordTestSuite("Smoke Tests", passed, duration);
            
            log.info("Smoke tests {} in {}ms", passed ? "PASSED" : "FAILED", duration.toMillis());
            return passed;
            
        } catch (Exception e) {
            log.error("Smoke tests failed", e);
            results.recordTestSuite("Smoke Tests", false, Duration.ZERO);
            return false;
        }
    }

    /**
     * Check service health
     */
    private boolean checkServiceHealth() {
        try {
            // Check if service is running and responsive
            ProcessBuilder pb = new ProcessBuilder("curl", "-f", "http://localhost:8080/actuator/health");
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            boolean healthy = exitCode == 0;
            log.info("Service health check: {}", healthy ? "HEALTHY" : "UNHEALTHY");
            return healthy;
            
        } catch (Exception e) {
            log.error("Service health check failed", e);
            return false;
        }
    }

    /**
     * Run basic API test
     */
    private boolean runBasicApiTest() {
        try {
            // Test basic API functionality
            ProcessBuilder pb = new ProcessBuilder("curl", "-f", "http://localhost:8080/api/prices?symbols=IBM.N");
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            boolean passed = exitCode == 0;
            log.info("Basic API test: {}", passed ? "PASSED" : "FAILED");
            return passed;
            
        } catch (Exception e) {
            log.error("Basic API test failed", e);
            return false;
        }
    }

    /**
     * Run connectivity test
     */
    private boolean runConnectivityTest() {
        try {
            // Test network connectivity
            ProcessBuilder pb = new ProcessBuilder("ping", "-c", "1", "localhost");
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            boolean passed = exitCode == 0;
            log.info("Connectivity test: {}", passed ? "PASSED" : "FAILED");
            return passed;
            
        } catch (Exception e) {
            log.error("Connectivity test failed", e);
            return false;
        }
    }

    /**
     * Generate test reports
     */
    private void generateReports() {
        log.info("=== Generating Test Reports ===");
        
        try {
            // Generate HTML report
            generateHtmlReport();
            
            // Generate JSON report
            generateJsonReport();
            
            // Generate XML report (JUnit format)
            generateXmlReport();
            
            // Generate CSV report
            generateCsvReport();
            
            log.info("Test reports generated successfully");
            
        } catch (Exception e) {
            log.error("Failed to generate test reports", e);
        }
    }

    /**
     * Generate HTML report
     */
    private void generateHtmlReport() {
        try {
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html>\n<head>\n<title>Test Automation Report</title>\n</head>\n<body>\n");
            html.append("<h1>Test Automation Report</h1>\n");
            html.append("<table border='1'>\n");
            html.append("<tr><th>Test Suite</th><th>Status</th><th>Duration</th></tr>\n");
            
            results.getTestSuites().forEach((name, result) -> {
                String status = result.isPassed() ? "PASSED" : "FAILED";
                String color = result.isPassed() ? "green" : "red";
                html.append(String.format("<tr><td>%s</td><td style='color:%s'>%s</td><td>%dms</td></tr>\n", 
                        name, color, status, result.getDuration().toMillis()));
            });
            
            html.append("</table>\n");
            html.append("</body>\n</html>");
            
            // Write to file
            java.nio.file.Files.write(java.nio.file.Paths.get("test-report.html"), 
                    html.toString().getBytes());
            
            log.info("HTML report generated: test-report.html");
            
        } catch (Exception e) {
            log.error("Failed to generate HTML report", e);
        }
    }

    /**
     * Generate JSON report
     */
    private void generateJsonReport() {
        try {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"testAutomation\": {\n");
            json.append("    \"timestamp\": \"").append(Instant.now().toString()).append("\",\n");
            json.append("    \"testSuites\": [\n");
            
            boolean first = true;
            for (Map.Entry<String, TestSuiteResult> entry : results.getTestSuites().entrySet()) {
                if (!first) json.append(",\n");
                json.append("      {\n");
                json.append("        \"name\": \"").append(entry.getKey()).append("\",\n");
                json.append("        \"passed\": ").append(entry.getValue().isPassed()).append(",\n");
                json.append("        \"duration\": ").append(entry.getValue().getDuration().toMillis()).append("\n");
                json.append("      }");
                first = false;
            }
            
            json.append("\n    ]\n");
            json.append("  }\n");
            json.append("}\n");
            
            // Write to file
            java.nio.file.Files.write(java.nio.file.Paths.get("test-report.json"), 
                    json.toString().getBytes());
            
            log.info("JSON report generated: test-report.json");
            
        } catch (Exception e) {
            log.error("Failed to generate JSON report", e);
        }
    }

    /**
     * Generate XML report (JUnit format)
     */
    private void generateXmlReport() {
        try {
            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xml.append("<testsuite name=\"Test Automation Suite\">\n");
            
            for (Map.Entry<String, TestSuiteResult> entry : results.getTestSuites().entrySet()) {
                xml.append("  <testcase name=\"").append(entry.getKey()).append("\" ");
                xml.append("time=\"").append(entry.getValue().getDuration().toMillis() / 1000.0).append("\"");
                
                if (!entry.getValue().isPassed()) {
                    xml.append(">\n");
                    xml.append("    <failure message=\"Test suite failed\"/>\n");
                    xml.append("  </testcase>\n");
                } else {
                    xml.append("/>\n");
                }
            }
            
            xml.append("</testsuite>\n");
            
            // Write to file
            java.nio.file.Files.write(java.nio.file.Paths.get("test-report.xml"), 
                    xml.toString().getBytes());
            
            log.info("XML report generated: test-report.xml");
            
        } catch (Exception e) {
            log.error("Failed to generate XML report", e);
        }
    }

    /**
     * Generate CSV report
     */
    private void generateCsvReport() {
        try {
            StringBuilder csv = new StringBuilder();
            csv.append("Test Suite,Status,Duration (ms)\n");
            
            for (Map.Entry<String, TestSuiteResult> entry : results.getTestSuites().entrySet()) {
                String status = entry.getValue().isPassed() ? "PASSED" : "FAILED";
                csv.append(entry.getKey()).append(",").append(status).append(",")
                   .append(entry.getValue().getDuration().toMillis()).append("\n");
            }
            
            // Write to file
            java.nio.file.Files.write(java.nio.file.Paths.get("test-report.csv"), 
                    csv.toString().getBytes());
            
            log.info("CSV report generated: test-report.csv");
            
        } catch (Exception e) {
            log.error("Failed to generate CSV report", e);
        }
    }

    /**
     * Print test results
     */
    private void printResults() {
        log.info("\n=== TEST AUTOMATION RESULTS ===");
        
        results.getTestSuites().forEach((name, result) -> {
            String status = result.isPassed() ? "PASSED" : "FAILED";
            log.info("{}: {} ({}ms)", name, status, result.getDuration().toMillis());
        });
        
        log.info("\n=== OVERALL RESULTS ===");
        results.getOverallResults().forEach((name, value) -> {
            if (value instanceof Duration) {
                log.info("{}: {}ms", name, ((Duration) value).toMillis());
            } else {
                log.info("{}: {}", name, value);
            }
        });
        
        log.info("\n=== TEST AUTOMATION SUMMARY ===");
        long totalTests = results.getTestSuites().size();
        long passedTests = results.getTestSuites().values().stream()
                .mapToLong(result -> result.isPassed() ? 1 : 0)
                .sum();
        long failedTests = totalTests - passedTests;
        
        log.info("Total test suites: {}", totalTests);
        log.info("Passed: {}", passedTests);
        log.info("Failed: {}", failedTests);
        log.info("Success rate: {:.1f}%", totalTests > 0 ? (double) passedTests / totalTests * 100 : 0);
    }

    /**
     * Shutdown executor
     */
    private void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Test suite result
     */
    @lombok.Data
    private static class TestSuiteResult {
        private final boolean passed;
        private final Duration duration;
    }

    /**
     * Test results tracking
     */
    private static class TestResults {
        private final Map<String, TestSuiteResult> testSuites = new HashMap<>();
        private final Map<String, Object> overallResults = new HashMap<>();

        public void recordTestSuite(String name, boolean passed, Duration duration) {
            testSuites.put(name, new TestSuiteResult(passed, duration));
        }

        public void recordOverallResult(String name, Object value) {
            overallResults.put(name, value);
        }

        public Map<String, TestSuiteResult> getTestSuites() {
            return testSuites;
        }

        public Map<String, Object> getOverallResults() {
            return overallResults;
        }
    }
}
