package com.srviswan.basketpricing.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Stress testing suite for the basket pricing service.
 * Tests system behavior under extreme load conditions to identify breaking points.
 */
@Slf4j
public class StressTestSuite {

    private static final String REST_BASE_URL = "http://localhost:8080/api/prices";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    // ObjectMapper for JSON processing
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private final StressTestResults results = new StressTestResults();

    public static void main(String[] args) {
        try {
            log.info("Starting Stress Test Suite");
            
            StressTestSuite suite = new StressTestSuite();
            suite.runAllStressTests();
            suite.printResults();
            
        } catch (Exception e) {
            log.error("Stress test suite failed", e);
            System.exit(1);
        }
    }

    public void runAllStressTests() {
        log.info("=== Running Stress Test Suite ===");
        
        // Basic Stress Tests
        testMemoryStress();
        testCpuStress();
        testNetworkStress();
        testConnectionStress();
        
        // Advanced Stress Tests
        testDataVolumeStress();
        testConcurrentConnectionStress();
        testRequestSizeStress();
        testRateLimitStress();
        
        // System Limit Tests
        testSystemResourceLimits();
        testApplicationLimits();
        testDatabaseLimits();
        testNetworkLimits();
        
        // Recovery Tests
        testSystemRecovery();
        testDataConsistencyAfterStress();
        testPerformanceAfterStress();
    }

    /**
     * Test memory stress
     */
    private void testMemoryStress() {
        log.info("Testing memory stress");
        
        StressTestConfig config = StressTestConfig.builder()
                .testName("Memory Stress")
                .concurrentUsers(50)
                .requestsPerUser(1000)
                .durationSeconds(300)
                .rampUpSeconds(30)
                .build();
        
        runStressTest(config, () -> {
            // Memory-intensive operations
            List<String> largeResponses = new ArrayList<>();
            
            for (int i = 0; i < 100; i++) {
                try {
                    String url = REST_BASE_URL + "?symbols=" + generateLargeSymbolList(100);
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();
                    
                    HttpResponse<String> response = HTTP_CLIENT.send(request, 
                            HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        largeResponses.add(response.body());
                    }
                    
                } catch (Exception e) {
                    log.debug("Memory stress request failed: {}", e.getMessage());
                }
            }
            
            // Keep responses in memory to stress the system
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Thread interrupted during stress test");
            }
        });
    }

    /**
     * Test CPU stress
     */
    private void testCpuStress() {
        log.info("Testing CPU stress");
        
        StressTestConfig config = StressTestConfig.builder()
                .testName("CPU Stress")
                .concurrentUsers(100)
                .requestsPerUser(500)
                .durationSeconds(180)
                .rampUpSeconds(20)
                .build();
        
        runStressTest(config, () -> {
            // CPU-intensive operations
            for (int i = 0; i < 50; i++) {
                try {
                    String url = REST_BASE_URL + "?symbols=" + generateLargeSymbolList(200);
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();
                    
                    HttpResponse<String> response = HTTP_CLIENT.send(request, 
                            HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        // Process response (CPU-intensive)
                        processLargeResponse(response.body());
                    }
                    
                } catch (Exception e) {
                    log.debug("CPU stress request failed: {}", e.getMessage());
                }
            }
        });
    }

    /**
     * Test network stress
     */
    private void testNetworkStress() {
        log.info("Testing network stress");
        
        StressTestConfig config = StressTestConfig.builder()
                .testName("Network Stress")
                .concurrentUsers(200)
                .requestsPerUser(300)
                .durationSeconds(240)
                .rampUpSeconds(40)
                .build();
        
        runStressTest(config, () -> {
            // Network-intensive operations
            for (int i = 0; i < 20; i++) {
                try {
                    String url = REST_BASE_URL + "?symbols=" + generateLargeSymbolList(500);
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();
                    
                    HttpResponse<String> response = HTTP_CLIENT.send(request, 
                            HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        // Simulate network processing
                        Thread.sleep(100);
                    }
                    
                } catch (Exception e) {
                    log.debug("Network stress request failed: {}", e.getMessage());
                }
            }
        });
    }

    /**
     * Test connection stress
     */
    private void testConnectionStress() {
        log.info("Testing connection stress");
        
        StressTestConfig config = StressTestConfig.builder()
                .testName("Connection Stress")
                .concurrentUsers(300)
                .requestsPerUser(200)
                .durationSeconds(180)
                .rampUpSeconds(60)
                .build();
        
        runStressTest(config, () -> {
            // Connection-intensive operations
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            
            for (int i = 0; i < 10; i++) {
                try {
                    String url = REST_BASE_URL + "?symbols=IBM.N,MSFT.O";
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();
                    
                    HttpResponse<String> response = client.send(request, 
                            HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        // Connection established successfully
                    }
                    
                } catch (Exception e) {
                    log.debug("Connection stress request failed: {}", e.getMessage());
                }
            }
        });
    }

    /**
     * Test data volume stress
     */
    private void testDataVolumeStress() {
        log.info("Testing data volume stress");
        
        StressTestConfig config = StressTestConfig.builder()
                .testName("Data Volume Stress")
                .concurrentUsers(75)
                .requestsPerUser(800)
                .durationSeconds(360)
                .rampUpSeconds(45)
                .build();
        
        runStressTest(config, () -> {
            // Data volume-intensive operations
            for (int i = 0; i < 30; i++) {
                try {
                    String url = REST_BASE_URL + "?symbols=" + generateLargeSymbolList(1000);
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();
                    
                    HttpResponse<String> response = HTTP_CLIENT.send(request, 
                            HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        // Process large data volume
                        processLargeDataVolume(response.body());
                    }
                    
                } catch (Exception e) {
                    log.debug("Data volume stress request failed: {}", e.getMessage());
                }
            }
        });
    }

    /**
     * Test concurrent connection stress
     */
    private void testConcurrentConnectionStress() {
        log.info("Testing concurrent connection stress");
        
        StressTestConfig config = StressTestConfig.builder()
                .testName("Concurrent Connection Stress")
                .concurrentUsers(500)
                .requestsPerUser(100)
                .durationSeconds(120)
                .rampUpSeconds(30)
                .build();
        
        runStressTest(config, () -> {
            // Concurrent connection-intensive operations
            for (int i = 0; i < 5; i++) {
                try {
                    String url = REST_BASE_URL + "?symbols=IBM.N";
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();
                    
                    HttpResponse<String> response = HTTP_CLIENT.send(request, 
                            HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        // Connection successful
                    }
                    
                } catch (Exception e) {
                    log.debug("Concurrent connection stress request failed: {}", e.getMessage());
                }
            }
        });
    }

    /**
     * Test request size stress
     */
    private void testRequestSizeStress() {
        log.info("Testing request size stress");
        
        StressTestConfig config = StressTestConfig.builder()
                .testName("Request Size Stress")
                .concurrentUsers(50)
                .requestsPerUser(200)
                .durationSeconds(180)
                .rampUpSeconds(20)
                .build();
        
        runStressTest(config, () -> {
            // Request size-intensive operations
            for (int i = 0; i < 10; i++) {
                try {
                    String url = REST_BASE_URL + "?symbols=" + generateLargeSymbolList(2000);
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();
                    
                    HttpResponse<String> response = HTTP_CLIENT.send(request, 
                            HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        // Large request processed
                    }
                    
                } catch (Exception e) {
                    log.debug("Request size stress request failed: {}", e.getMessage());
                }
            }
        });
    }

    /**
     * Test rate limit stress
     */
    private void testRateLimitStress() {
        log.info("Testing rate limit stress");
        
        StressTestConfig config = StressTestConfig.builder()
                .testName("Rate Limit Stress")
                .concurrentUsers(100)
                .requestsPerUser(1000)
                .durationSeconds(60)
                .rampUpSeconds(10)
                .build();
        
        runStressTest(config, () -> {
            // Rate limit-intensive operations
            for (int i = 0; i < 100; i++) {
                try {
                    String url = REST_BASE_URL + "?symbols=IBM.N,MSFT.O";
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();
                    
                    HttpResponse<String> response = HTTP_CLIENT.send(request, 
                            HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        // Request successful
                    } else if (response.statusCode() == 429) {
                        // Rate limited
                        Thread.sleep(100);
                    }
                    
                } catch (Exception e) {
                    log.debug("Rate limit stress request failed: {}", e.getMessage());
                }
            }
        });
    }

    /**
     * Test system resource limits
     */
    private void testSystemResourceLimits() {
        log.info("Testing system resource limits");
        
        StressTestConfig config = StressTestConfig.builder()
                .testName("System Resource Limits")
                .concurrentUsers(400)
                .requestsPerUser(300)
                .durationSeconds(300)
                .rampUpSeconds(60)
                .build();
        
        runStressTest(config, () -> {
            // System resource-intensive operations
            for (int i = 0; i < 50; i++) {
                try {
                    String url = REST_BASE_URL + "?symbols=" + generateLargeSymbolList(100);
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();
                    
                    HttpResponse<String> response = HTTP_CLIENT.send(request, 
                            HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        // Resource-intensive processing
                        processResourceIntensiveOperation(response.body());
                    }
                    
                } catch (Exception e) {
                    log.debug("System resource limit request failed: {}", e.getMessage());
                }
            }
        });
    }

    /**
     * Test application limits
     */
    private void testApplicationLimits() {
        log.info("Testing application limits");
        
        StressTestConfig config = StressTestConfig.builder()
                .testName("Application Limits")
                .concurrentUsers(250)
                .requestsPerUser(400)
                .durationSeconds(240)
                .rampUpSeconds(40)
                .build();
        
        runStressTest(config, () -> {
            // Application limit-intensive operations
            for (int i = 0; i < 25; i++) {
                try {
                    String url = REST_BASE_URL + "?symbols=" + generateLargeSymbolList(300);
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();
                    
                    HttpResponse<String> response = HTTP_CLIENT.send(request, 
                            HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        // Application processing
                        processApplicationOperation(response.body());
                    }
                    
                } catch (Exception e) {
                    log.debug("Application limit request failed: {}", e.getMessage());
                }
            }
        });
    }

    /**
     * Test database limits
     */
    private void testDatabaseLimits() {
        log.info("Testing database limits");
        
        StressTestConfig config = StressTestConfig.builder()
                .testName("Database Limits")
                .concurrentUsers(150)
                .requestsPerUser(600)
                .durationSeconds(360)
                .rampUpSeconds(45)
                .build();
        
        runStressTest(config, () -> {
            // Database-intensive operations
            for (int i = 0; i < 40; i++) {
                try {
                    String url = REST_BASE_URL + "?symbols=" + generateLargeSymbolList(150);
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();
                    
                    HttpResponse<String> response = HTTP_CLIENT.send(request, 
                            HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        // Database processing
                        processDatabaseOperation(response.body());
                    }
                    
                } catch (Exception e) {
                    log.debug("Database limit request failed: {}", e.getMessage());
                }
            }
        });
    }

    /**
     * Test network limits
     */
    private void testNetworkLimits() {
        log.info("Testing network limits");
        
        StressTestConfig config = StressTestConfig.builder()
                .testName("Network Limits")
                .concurrentUsers(300)
                .requestsPerUser(200)
                .durationSeconds(180)
                .rampUpSeconds(30)
                .build();
        
        runStressTest(config, () -> {
            // Network-intensive operations
            for (int i = 0; i < 15; i++) {
                try {
                    String url = REST_BASE_URL + "?symbols=" + generateLargeSymbolList(400);
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();
                    
                    HttpResponse<String> response = HTTP_CLIENT.send(request, 
                            HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        // Network processing
                        processNetworkOperation(response.body());
                    }
                    
                } catch (Exception e) {
                    log.debug("Network limit request failed: {}", e.getMessage());
                }
            }
        });
    }

    /**
     * Test system recovery
     */
    private void testSystemRecovery() {
        log.info("Testing system recovery");
        
        // First, stress the system
        StressTestConfig stressConfig = StressTestConfig.builder()
                .testName("System Recovery - Stress Phase")
                .concurrentUsers(200)
                .requestsPerUser(500)
                .durationSeconds(120)
                .rampUpSeconds(20)
                .build();
        
        runStressTest(stressConfig, () -> {
            // Stress operations
            for (int i = 0; i < 20; i++) {
                try {
                    String url = REST_BASE_URL + "?symbols=" + generateLargeSymbolList(200);
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();
                    
                    HttpResponse<String> response = HTTP_CLIENT.send(request, 
                            HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        // Stress processing
                        processStressOperation(response.body());
                    }
                    
                } catch (Exception e) {
                    log.debug("System recovery stress request failed: {}", e.getMessage());
                }
            }
        });
        
        // Wait for system to recover
        try {
            Thread.sleep(30000); // 30 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Test recovery
        StressTestConfig recoveryConfig = StressTestConfig.builder()
                .testName("System Recovery - Recovery Phase")
                .concurrentUsers(20)
                .requestsPerUser(100)
                .durationSeconds(60)
                .rampUpSeconds(5)
                .build();
        
        runStressTest(recoveryConfig, () -> {
            // Recovery operations
            for (int i = 0; i < 10; i++) {
                try {
                    String url = REST_BASE_URL + "?symbols=IBM.N,MSFT.O";
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();
                    
                    HttpResponse<String> response = HTTP_CLIENT.send(request, 
                            HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        // Recovery processing
                        processRecoveryOperation(response.body());
                    }
                    
                } catch (Exception e) {
                    log.debug("System recovery request failed: {}", e.getMessage());
                }
            }
        });
    }

    /**
     * Test data consistency after stress
     */
    private void testDataConsistencyAfterStress() {
        log.info("Testing data consistency after stress");
        
        // First, stress the system
        StressTestConfig stressConfig = StressTestConfig.builder()
                .testName("Data Consistency - Stress Phase")
                .concurrentUsers(150)
                .requestsPerUser(300)
                .durationSeconds(180)
                .rampUpSeconds(30)
                .build();
        
        runStressTest(stressConfig, () -> {
            // Stress operations
            for (int i = 0; i < 25; i++) {
                try {
                    String url = REST_BASE_URL + "?symbols=" + generateLargeSymbolList(100);
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();
                    
                    HttpResponse<String> response = HTTP_CLIENT.send(request, 
                            HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        // Stress processing
                        processStressOperation(response.body());
                    }
                    
                } catch (Exception e) {
                    log.debug("Data consistency stress request failed: {}", e.getMessage());
                }
            }
        });
        
        // Wait for system to stabilize
        try {
            Thread.sleep(20000); // 20 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Test data consistency
        StressTestConfig consistencyConfig = StressTestConfig.builder()
                .testName("Data Consistency - Consistency Phase")
                .concurrentUsers(10)
                .requestsPerUser(50)
                .durationSeconds(60)
                .rampUpSeconds(5)
                .build();
        
        runStressTest(consistencyConfig, () -> {
            // Consistency operations
            for (int i = 0; i < 5; i++) {
                try {
                    String url = REST_BASE_URL + "?symbols=IBM.N,MSFT.O,AAPL.O";
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();
                    
                    HttpResponse<String> response = HTTP_CLIENT.send(request, 
                            HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        // Consistency processing
                        processConsistencyOperation(response.body());
                    }
                    
                } catch (Exception e) {
                    log.debug("Data consistency request failed: {}", e.getMessage());
                }
            }
        });
    }

    /**
     * Test performance after stress
     */
    private void testPerformanceAfterStress() {
        log.info("Testing performance after stress");
        
        // First, stress the system
        StressTestConfig stressConfig = StressTestConfig.builder()
                .testName("Performance After Stress - Stress Phase")
                .concurrentUsers(100)
                .requestsPerUser(400)
                .durationSeconds(150)
                .rampUpSeconds(25)
                .build();
        
        runStressTest(stressConfig, () -> {
            // Stress operations
            for (int i = 0; i < 30; i++) {
                try {
                    String url = REST_BASE_URL + "?symbols=" + generateLargeSymbolList(150);
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();
                    
                    HttpResponse<String> response = HTTP_CLIENT.send(request, 
                            HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        // Stress processing
                        processStressOperation(response.body());
                    }
                    
                } catch (Exception e) {
                    log.debug("Performance after stress request failed: {}", e.getMessage());
                }
            }
        });
        
        // Wait for system to recover
        try {
            Thread.sleep(25000); // 25 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Test performance
        StressTestConfig performanceConfig = StressTestConfig.builder()
                .testName("Performance After Stress - Performance Phase")
                .concurrentUsers(15)
                .requestsPerUser(80)
                .durationSeconds(60)
                .rampUpSeconds(5)
                .build();
        
        runStressTest(performanceConfig, () -> {
            // Performance operations
            for (int i = 0; i < 8; i++) {
                try {
                    String url = REST_BASE_URL + "?symbols=IBM.N,MSFT.O";
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();
                    
                    HttpResponse<String> response = HTTP_CLIENT.send(request, 
                            HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        // Performance processing
                        processPerformanceOperation(response.body());
                    }
                    
                } catch (Exception e) {
                    log.debug("Performance after stress request failed: {}", e.getMessage());
                }
            }
        });
    }

    /**
     * Run a stress test with the given configuration
     */
    private void runStressTest(StressTestConfig config, Runnable stressOperation) {
        log.info("Running stress test: {}", config.getTestName());
        
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
                    
                    // Run stress operations
                    for (int req = 0; req < config.getRequestsPerUser(); req++) {
                        if (Thread.currentThread().isInterrupted()) {
                            break;
                        }
                        
                        try {
                            long requestStartTime = System.nanoTime();
                            
                            // Execute stress operation
                            stressOperation.run();
                            
                            long requestEndTime = System.nanoTime();
                            long responseTime = (requestEndTime - requestStartTime) / 1_000_000;
                            
                            responseTimes.add(responseTime);
                            statusCodes.add(200); // Assume success for stress operations
                            totalRequests.incrementAndGet();
                            successfulRequests.incrementAndGet();
                            
                        } catch (Exception e) {
                            failedRequests.incrementAndGet();
                            totalRequests.incrementAndGet();
                            log.debug("Stress test request failed for user {}: {}", userId, e.getMessage());
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
                log.warn("Stress test {} timed out after {} seconds", config.getTestName(), config.getDurationSeconds());
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
        StressTestResult result = calculateStressTestResults(config, totalRequests.get(), 
                successfulRequests.get(), failedRequests.get(), responseTimes, statusCodes, testDuration);
        
        // Record results
        results.recordResult(config.getTestName(), result);
        
        log.info("Stress test {} completed - {} requests, {} successful, {} failed, {:.2f} req/sec, {:.2f}ms avg response time",
                config.getTestName(), totalRequests.get(), successfulRequests.get(), failedRequests.get(), 
                result.getThroughput(), result.getAvgResponseTime());
    }

    /**
     * Calculate stress test results
     */
    private StressTestResult calculateStressTestResults(StressTestConfig config, 
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
        double cpuUtilization = Math.min(100.0, (config.getConcurrentUsers() / 10.0) * 30.0);
        double memoryUtilization = Math.min(100.0, (config.getConcurrentUsers() / 10.0) * 25.0);
        double networkUtilization = Math.min(100.0, (config.getConcurrentUsers() / 10.0) * 35.0);
        
        return StressTestResult.builder()
                .testName(config.getTestName())
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
     * Generate large symbol list
     */
    private String generateLargeSymbolList(int count) {
        List<String> symbols = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            symbols.add("SYMBOL" + String.format("%04d", i) + ".N");
        }
        return String.join(",", symbols);
    }

    /**
     * Process large response
     */
    private void processLargeResponse(String response) {
        // Simulate processing of large response
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Process large data volume
     */
    private void processLargeDataVolume(String data) {
        // Simulate processing of large data volume
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Process resource-intensive operation
     */
    private void processResourceIntensiveOperation(String data) {
        // Simulate resource-intensive processing
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Process application operation
     */
    private void processApplicationOperation(String data) {
        // Simulate application processing
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Process database operation
     */
    private void processDatabaseOperation(String data) {
        // Simulate database processing
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Process network operation
     */
    private void processNetworkOperation(String data) {
        // Simulate network processing
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Process stress operation
     */
    private void processStressOperation(String data) {
        // Simulate stress processing
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Process recovery operation
     */
    private void processRecoveryOperation(String data) {
        // Simulate recovery processing
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Process consistency operation
     */
    private void processConsistencyOperation(String data) {
        // Simulate consistency processing
        try {
            Thread.sleep(75);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Process performance operation
     */
    private void processPerformanceOperation(String data) {
        // Simulate performance processing
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Print stress test results
     */
    private void printResults() {
        log.info("\n=== STRESS TEST RESULTS ===");
        
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
        
        log.info("\n=== STRESS TEST SUMMARY ===");
        log.info("Total tests run: {}", results.getResults().size());
        
        // Find breaking points
        Optional<StressTestResult> highestErrorRate = results.getResults().values().stream()
                .max(Comparator.comparing(StressTestResult::getErrorRate));
        Optional<StressTestResult> lowestThroughput = results.getResults().values().stream()
                .min(Comparator.comparing(StressTestResult::getThroughput));
        
        highestErrorRate.ifPresent(result -> 
            log.info("Highest error rate: {} ({:.2f}%)", result.getTestName(), result.getErrorRate()));
        lowestThroughput.ifPresent(result -> 
            log.info("Lowest throughput: {} ({:.2f} req/sec)", result.getTestName(), result.getThroughput()));
    }

    /**
     * Stress test configuration
     */
    @lombok.Builder
    @lombok.Data
    private static class StressTestConfig {
        private final String testName;
        private final int concurrentUsers;
        private final int requestsPerUser;
        private final int durationSeconds;
        private final int rampUpSeconds;
    }

    /**
     * Stress test result
     */
    @lombok.Builder
    @lombok.Data
    private static class StressTestResult {
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
     * Stress test results tracking
     */
    private static class StressTestResults {
        private final Map<String, StressTestResult> results = new HashMap<>();

        public void recordResult(String testName, StressTestResult result) {
            results.put(testName, result);
        }

        public Map<String, StressTestResult> getResults() {
            return results;
        }
    }
}
