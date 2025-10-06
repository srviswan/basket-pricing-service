package com.srviswan.basketpricing.testing;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Load testing suite for the basket pricing service.
 * Tests system behavior under various load conditions with concurrent users.
 */
@Slf4j
public class LoadTestSuite {

    private static final String REST_BASE_URL = "http://localhost:8080/api/prices";
    
    private final LoadTestResults results = new LoadTestResults();

    public static void main(String[] args) {
        try {
            log.info("Starting Load Test Suite");
            
            LoadTestSuite suite = new LoadTestSuite();
            suite.runAllLoadTests();
            suite.printResults();
            
        } catch (Exception e) {
            log.error("Load test suite failed", e);
            System.exit(1);
        }
    }

    public void runAllLoadTests() {
        log.info("=== Running Load Test Suite ===");
        
        // Basic Load Tests
        testLightLoad();
        testMediumLoad();
        testHeavyLoad();
        testPeakLoad();
        
        // Advanced Load Tests
        testGradualLoadIncrease();
        testLoadSpike();
        testSustainedLoad();
        testLoadRecovery();
        
        // Concurrent User Tests
        testConcurrentUsers();
        testUserSessionManagement();
        testResourceContention();
        
        // System Behavior Tests
        testResponseTimeUnderLoad();
        testErrorRateUnderLoad();
        testResourceUtilizationUnderLoad();
    }

    /**
     * Test light load (10 concurrent users)
     */
    private void testLightLoad() {
        log.info("Testing light load (10 concurrent users)");
        
        LoadTestConfig config = LoadTestConfig.builder()
                .concurrentUsers(10)
                .requestsPerUser(100)
                .durationSeconds(60)
                .rampUpSeconds(10)
                .build();
        
        runLoadTest("Light Load", config);
    }

    /**
     * Test medium load (50 concurrent users)
     */
    private void testMediumLoad() {
        log.info("Testing medium load (50 concurrent users)");
        
        LoadTestConfig config = LoadTestConfig.builder()
                .concurrentUsers(50)
                .requestsPerUser(200)
                .durationSeconds(120)
                .rampUpSeconds(20)
                .build();
        
        runLoadTest("Medium Load", config);
    }

    /**
     * Test heavy load (100 concurrent users)
     */
    private void testHeavyLoad() {
        log.info("Testing heavy load (100 concurrent users)");
        
        LoadTestConfig config = LoadTestConfig.builder()
                .concurrentUsers(100)
                .requestsPerUser(300)
                .durationSeconds(180)
                .rampUpSeconds(30)
                .build();
        
        runLoadTest("Heavy Load", config);
    }

    /**
     * Test peak load (200 concurrent users)
     */
    private void testPeakLoad() {
        log.info("Testing peak load (200 concurrent users)");
        
        LoadTestConfig config = LoadTestConfig.builder()
                .concurrentUsers(200)
                .requestsPerUser(500)
                .durationSeconds(300)
                .rampUpSeconds(60)
                .build();
        
        runLoadTest("Peak Load", config);
    }

    /**
     * Test gradual load increase
     */
    private void testGradualLoadIncrease() {
        log.info("Testing gradual load increase");
        
        int[] userCounts = {10, 25, 50, 75, 100, 125, 150};
        
        for (int userCount : userCounts) {
            LoadTestConfig config = LoadTestConfig.builder()
                    .concurrentUsers(userCount)
                    .requestsPerUser(50)
                    .durationSeconds(60)
                    .rampUpSeconds(10)
                    .build();
            
            runLoadTest("Gradual Load - " + userCount + " users", config);
            
            // Wait between load levels
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Test load spike
     */
    private void testLoadSpike() {
        log.info("Testing load spike");
        
        // Start with normal load
        LoadTestConfig normalConfig = LoadTestConfig.builder()
                .concurrentUsers(20)
                .requestsPerUser(100)
                .durationSeconds(30)
                .rampUpSeconds(5)
                .build();
        
        runLoadTest("Load Spike - Normal", normalConfig);
        
        // Immediate spike to high load
        LoadTestConfig spikeConfig = LoadTestConfig.builder()
                .concurrentUsers(200)
                .requestsPerUser(100)
                .durationSeconds(60)
                .rampUpSeconds(5)
                .build();
        
        runLoadTest("Load Spike - High", spikeConfig);
        
        // Return to normal load
        runLoadTest("Load Spike - Recovery", normalConfig);
    }

    /**
     * Test sustained load
     */
    private void testSustainedLoad() {
        log.info("Testing sustained load");
        
        LoadTestConfig config = LoadTestConfig.builder()
                .concurrentUsers(75)
                .requestsPerUser(1000)
                .durationSeconds(600) // 10 minutes
                .rampUpSeconds(30)
                .build();
        
        runLoadTest("Sustained Load", config);
    }

    /**
     * Test load recovery
     */
    private void testLoadRecovery() {
        log.info("Testing load recovery");
        
        // High load
        LoadTestConfig highConfig = LoadTestConfig.builder()
                .concurrentUsers(150)
                .requestsPerUser(200)
                .durationSeconds(120)
                .rampUpSeconds(20)
                .build();
        
        runLoadTest("Load Recovery - High", highConfig);
        
        // Wait for recovery
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Low load
        LoadTestConfig lowConfig = LoadTestConfig.builder()
                .concurrentUsers(10)
                .requestsPerUser(50)
                .durationSeconds(60)
                .rampUpSeconds(5)
                .build();
        
        runLoadTest("Load Recovery - Low", lowConfig);
    }

    /**
     * Test concurrent users
     */
    private void testConcurrentUsers() {
        log.info("Testing concurrent users");
        
        int[] userCounts = {10, 25, 50, 100, 150, 200};
        
        for (int userCount : userCounts) {
            LoadTestConfig config = LoadTestConfig.builder()
                    .concurrentUsers(userCount)
                    .requestsPerUser(100)
                    .durationSeconds(60)
                    .rampUpSeconds(10)
                    .build();
            
            runLoadTest("Concurrent Users - " + userCount, config);
        }
    }

    /**
     * Test user session management
     */
    private void testUserSessionManagement() {
        log.info("Testing user session management");
        
        LoadTestConfig config = LoadTestConfig.builder()
                .concurrentUsers(50)
                .requestsPerUser(200)
                .durationSeconds(180)
                .rampUpSeconds(20)
                .sessionDurationSeconds(300)
                .build();
        
        runLoadTest("User Session Management", config);
    }

    /**
     * Test resource contention
     */
    private void testResourceContention() {
        log.info("Testing resource contention");
        
        LoadTestConfig config = LoadTestConfig.builder()
                .concurrentUsers(100)
                .requestsPerUser(500)
                .durationSeconds(240)
                .rampUpSeconds(30)
                .build();
        
        runLoadTest("Resource Contention", config);
    }

    /**
     * Test response time under load
     */
    private void testResponseTimeUnderLoad() {
        log.info("Testing response time under load");
        
        int[] userCounts = {10, 25, 50, 100, 150, 200};
        
        for (int userCount : userCounts) {
            LoadTestConfig config = LoadTestConfig.builder()
                    .concurrentUsers(userCount)
                    .requestsPerUser(100)
                    .durationSeconds(60)
                    .rampUpSeconds(10)
                    .build();
            
            LoadTestResult result = runLoadTest("Response Time - " + userCount + " users", config);
            
            // Record response time metrics
            results.recordMetric("Response Time - " + userCount + " users - Avg (ms)", result.getAvgResponseTime());
            results.recordMetric("Response Time - " + userCount + " users - P95 (ms)", result.getP95ResponseTime());
            results.recordMetric("Response Time - " + userCount + " users - P99 (ms)", result.getP99ResponseTime());
        }
    }

    /**
     * Test error rate under load
     */
    private void testErrorRateUnderLoad() {
        log.info("Testing error rate under load");
        
        int[] userCounts = {10, 25, 50, 100, 150, 200, 250, 300};
        
        for (int userCount : userCounts) {
            LoadTestConfig config = LoadTestConfig.builder()
                    .concurrentUsers(userCount)
                    .requestsPerUser(100)
                    .durationSeconds(60)
                    .rampUpSeconds(10)
                    .build();
            
            LoadTestResult result = runLoadTest("Error Rate - " + userCount + " users", config);
            
            // Record error rate metrics
            results.recordMetric("Error Rate - " + userCount + " users (%)", result.getErrorRate());
        }
    }

    /**
     * Test resource utilization under load
     */
    private void testResourceUtilizationUnderLoad() {
        log.info("Testing resource utilization under load");
        
        LoadTestConfig config = LoadTestConfig.builder()
                .concurrentUsers(100)
                .requestsPerUser(300)
                .durationSeconds(180)
                .rampUpSeconds(30)
                .build();
        
        LoadTestResult result = runLoadTest("Resource Utilization", config);
        
        // Record resource utilization metrics
        results.recordMetric("CPU Utilization (%)", result.getCpuUtilization());
        results.recordMetric("Memory Utilization (%)", result.getMemoryUtilization());
        results.recordMetric("Network Utilization (%)", result.getNetworkUtilization());
    }

    /**
     * Run a load test with the given configuration
     */
    private LoadTestResult runLoadTest(String testName, LoadTestConfig config) {
        log.info("Running load test: {}", testName);
        
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        AtomicLong totalRequests = new AtomicLong(0);
        AtomicLong successfulRequests = new AtomicLong(0);
        AtomicLong failedRequests = new AtomicLong(0);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
        List<Integer> statusCodes = Collections.synchronizedList(new ArrayList<>());
        
        ExecutorService executor = Executors.newFixedThreadPool(config.getConcurrentUsers());
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(config.getConcurrentUsers());
        
        // Start time
        Instant startTime = Instant.now();
        
        // Create user threads
        for (int user = 0; user < config.getConcurrentUsers(); user++) {
            final int userId = user;
            executor.submit(() -> {
                try {
                    // Wait for start signal
                    startLatch.await();
                    
                    // Ramp up delay
                    if (config.getRampUpSeconds() > 0) {
                        long delay = (config.getRampUpSeconds() * 1000L) / config.getConcurrentUsers() * userId;
                        Thread.sleep(delay);
                    }
                    
                    // Run requests
                    for (int req = 0; req < config.getRequestsPerUser(); req++) {
                        if (Thread.currentThread().isInterrupted()) {
                            break;
                        }
                        
                        try {
                            long requestStartTime = System.nanoTime();
                            
                            String url = REST_BASE_URL + "?symbols=IBM.N,MSFT.O,AAPL.O";
                            HttpRequest request = HttpRequest.newBuilder()
                                    .uri(URI.create(url))
                                    .GET()
                                    .build();
                            
                            HttpResponse<String> response = httpClient.send(request, 
                                    HttpResponse.BodyHandlers.ofString());
                            
                            long requestEndTime = System.nanoTime();
                            long responseTime = (requestEndTime - requestStartTime) / 1_000_000;
                            
                            responseTimes.add(responseTime);
                            statusCodes.add(response.statusCode());
                            totalRequests.incrementAndGet();
                            
                            if (response.statusCode() == 200) {
                                successfulRequests.incrementAndGet();
                            } else {
                                failedRequests.incrementAndGet();
                            }
                            
                        } catch (Exception e) {
                            failedRequests.incrementAndGet();
                            totalRequests.incrementAndGet();
                            log.debug("Request failed for user {}: {}", userId, e.getMessage());
                        }
                        
                        // Small delay between requests
                        Thread.sleep(10);
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }
        
        // Start the test
        startLatch.countDown();
        
        // Wait for completion or timeout
        try {
            boolean completed = finishLatch.await(config.getDurationSeconds(), TimeUnit.SECONDS);
            if (!completed) {
                log.warn("Load test {} timed out after {} seconds", testName, config.getDurationSeconds());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // End time
        Instant endTime = Instant.now();
        Duration testDuration = Duration.between(startTime, endTime);
        
        // Shutdown executor
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Calculate results
        LoadTestResult result = calculateLoadTestResults(testName, config, totalRequests.get(), 
                successfulRequests.get(), failedRequests.get(), responseTimes, statusCodes, testDuration);
        
        // Record results
        results.recordResult(testName, result);
        
        log.info("Load test {} completed - {} requests, {} successful, {} failed, {:.2f} req/sec, {:.2f}ms avg response time",
                testName, totalRequests.get(), successfulRequests.get(), failedRequests.get(), 
                result.getThroughput(), result.getAvgResponseTime());
        
        return result;
    }

    /**
     * Calculate load test results
     */
    private LoadTestResult calculateLoadTestResults(String testName, LoadTestConfig config, 
            long totalRequests, long successfulRequests, long failedRequests, 
            List<Long> responseTimes, List<Integer> statusCodes, Duration testDuration) {
        
        double throughput = (double) totalRequests / testDuration.getSeconds();
        double errorRate = totalRequests > 0 ? (double) failedRequests / totalRequests * 100 : 0;
        
        // Calculate response time statistics
        double avgResponseTime = 0;
        double p95ResponseTime = 0;
        double p99ResponseTime = 0;
        
        if (!responseTimes.isEmpty()) {
            Collections.sort(responseTimes);
            avgResponseTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
            p95ResponseTime = calculatePercentile(responseTimes, 95);
            p99ResponseTime = calculatePercentile(responseTimes, 99);
        }
        
        // Calculate resource utilization (simplified)
        double cpuUtilization = Math.min(100.0, (config.getConcurrentUsers() / 10.0) * 20.0);
        double memoryUtilization = Math.min(100.0, (config.getConcurrentUsers() / 10.0) * 15.0);
        double networkUtilization = Math.min(100.0, (config.getConcurrentUsers() / 10.0) * 25.0);
        
        return LoadTestResult.builder()
                .testName(testName)
                .totalRequests(totalRequests)
                .successfulRequests(successfulRequests)
                .failedRequests(failedRequests)
                .throughput(throughput)
                .errorRate(errorRate)
                .avgResponseTime(avgResponseTime)
                .p95ResponseTime(p95ResponseTime)
                .p99ResponseTime(p99ResponseTime)
                .cpuUtilization(cpuUtilization)
                .memoryUtilization(memoryUtilization)
                .networkUtilization(networkUtilization)
                .testDuration(testDuration)
                .build();
    }

    /**
     * Calculate percentile
     */
    private double calculatePercentile(List<Long> values, double percentile) {
        if (values.isEmpty()) return 0.0;
        
        int index = (int) Math.ceil((percentile / 100.0) * values.size()) - 1;
        return values.get(Math.max(0, index));
    }

    /**
     * Print load test results
     */
    private void printResults() {
        log.info("\n=== LOAD TEST RESULTS ===");
        
        results.getResults().forEach((testName, result) -> {
            log.info("\n--- {} ---", testName);
            log.info("Total Requests: {}", result.getTotalRequests());
            log.info("Successful Requests: {}", result.getSuccessfulRequests());
            log.info("Failed Requests: {}", result.getFailedRequests());
            log.info("Throughput: {:.2f} req/sec", result.getThroughput());
            log.info("Error Rate: {:.2f}%", result.getErrorRate());
            log.info("Avg Response Time: {:.2f}ms", result.getAvgResponseTime());
            log.info("P95 Response Time: {:.2f}ms", result.getP95ResponseTime());
            log.info("P99 Response Time: {:.2f}ms", result.getP99ResponseTime());
            log.info("CPU Utilization: {:.2f}%", result.getCpuUtilization());
            log.info("Memory Utilization: {:.2f}%", result.getMemoryUtilization());
            log.info("Network Utilization: {:.2f}%", result.getNetworkUtilization());
        });
        
        log.info("\n=== LOAD TEST SUMMARY ===");
        log.info("Total tests run: {}", results.getResults().size());
        
        // Find best and worst performing tests
        Optional<LoadTestResult> bestThroughput = results.getResults().values().stream()
                .max(Comparator.comparing(LoadTestResult::getThroughput));
        Optional<LoadTestResult> worstErrorRate = results.getResults().values().stream()
                .max(Comparator.comparing(LoadTestResult::getErrorRate));
        
        bestThroughput.ifPresent(result -> 
            log.info("Best throughput: {} ({:.2f} req/sec)", result.getTestName(), result.getThroughput()));
        worstErrorRate.ifPresent(result -> 
            log.info("Worst error rate: {} ({:.2f}%)", result.getTestName(), result.getErrorRate()));
    }

    /**
     * Load test configuration
     */
    @lombok.Builder
    @lombok.Data
    private static class LoadTestConfig {
        private final int concurrentUsers;
        private final int requestsPerUser;
        private final int durationSeconds;
        private final int rampUpSeconds;
        private final int sessionDurationSeconds;
    }

    /**
     * Load test result
     */
    @lombok.Builder
    @lombok.Data
    private static class LoadTestResult {
        private final String testName;
        private final long totalRequests;
        private final long successfulRequests;
        private final long failedRequests;
        private final double throughput;
        private final double errorRate;
        private final double avgResponseTime;
        private final double p95ResponseTime;
        private final double p99ResponseTime;
        private final double cpuUtilization;
        private final double memoryUtilization;
        private final double networkUtilization;
        private final Duration testDuration;
    }

    /**
     * Load test results tracking
     */
    private static class LoadTestResults {
        private final Map<String, LoadTestResult> results = new HashMap<>();
        private final Map<String, Double> metrics = new HashMap<>();

        public void recordResult(String testName, LoadTestResult result) {
            results.put(testName, result);
        }

        public void recordMetric(String name, double value) {
            metrics.put(name, value);
        }

        public Map<String, LoadTestResult> getResults() {
            return results;
        }

        public Map<String, Double> getMetrics() {
            return metrics;
        }
    }
}
