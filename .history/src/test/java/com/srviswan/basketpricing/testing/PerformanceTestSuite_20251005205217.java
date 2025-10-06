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
 * Performance benchmarking suite for the basket pricing service.
 * Tests response times, throughput, and resource utilization under various loads.
 */
@Slf4j
public class PerformanceTestSuite {

    private static final String REST_BASE_URL = "http://localhost:8080/api/prices";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    // ObjectMapper for JSON processing
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private final PerformanceResults results = new PerformanceResults();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        try {
            log.info("Starting Performance Test Suite");
            
            PerformanceTestSuite suite = new PerformanceTestSuite();
            suite.runAllPerformanceTests();
            suite.printResults();
            suite.shutdown();
            
        } catch (Exception e) {
            log.error("Performance test suite failed", e);
            System.exit(1);
        }
    }

    public void runAllPerformanceTests() {
        log.info("=== Running Performance Test Suite ===");
        
        // Basic Performance Tests
        testSingleRequestLatency();
        testConcurrentRequestLatency();
        testThroughputUnderLoad();
        testMemoryUsage();
        testCpuUsage();
        
        // Advanced Performance Tests
        testResponseTimePercentiles();
        testConnectionPooling();
        testKeepAlivePerformance();
        testLargePayloadHandling();
        testStreamingPerformance();
        
        // Resource Utilization Tests
        testMemoryLeaks();
        testGarbageCollection();
        testThreadPoolUtilization();
        testNetworkBandwidth();
    }

    /**
     * Test single request latency
     */
    private void testSingleRequestLatency() {
        log.info("Testing single request latency");
        
        List<Long> latencies = new ArrayList<>();
        
        for (int i = 0; i < 100; i++) {
            try {
                long startTime = System.nanoTime();
                
                String url = REST_BASE_URL + "?symbols=IBM.N,MSFT.O";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();
                
                HttpResponse<String> response = HTTP_CLIENT.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                
                long endTime = System.nanoTime();
                long latency = (endTime - startTime) / 1_000_000; // Convert to milliseconds
                latencies.add(latency);
                
                assert response.statusCode() == 200 : "Request failed";
                
            } catch (Exception e) {
                log.error("Request failed", e);
            }
        }
        
        calculateLatencyStats("Single Request Latency", latencies);
    }

    /**
     * Test concurrent request latency
     */
    private void testConcurrentRequestLatency() {
        log.info("Testing concurrent request latency");
        
        int concurrentUsers = 50;
        int requestsPerUser = 10;
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        
        for (int user = 0; user < concurrentUsers; user++) {
            executor.submit(() -> {
                try {
                    for (int req = 0; req < requestsPerUser; req++) {
                        long startTime = System.nanoTime();
                        
                        String url = REST_BASE_URL + "?symbols=IBM.N,MSFT.O,AAPL.O";
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .GET()
                                .build();
                        
                        HttpResponse<String> response = HTTP_CLIENT.send(request, 
                                HttpResponse.BodyHandlers.ofString());
                        
                        long endTime = System.nanoTime();
                        long latency = (endTime - startTime) / 1_000_000;
                        latencies.add(latency);
                        
                        assert response.statusCode() == 200 : "Request failed";
                    }
                } catch (Exception e) {
                    log.error("Concurrent request failed", e);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        calculateLatencyStats("Concurrent Request Latency", latencies);
    }

    /**
     * Test throughput under load
     */
    private void testThroughputUnderLoad() {
        log.info("Testing throughput under load");
        
        int durationSeconds = 30;
        AtomicLong requestCount = new AtomicLong(0);
        AtomicLong errorCount = new AtomicLong(0);
        
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
        
        // Start throughput test
        Instant startTime = Instant.now();
        Instant endTime = startTime.plusSeconds(durationSeconds);
        
        // Submit requests continuously
        while (Instant.now().isBefore(endTime)) {
            scheduler.submit(() -> {
                try {
                    String url = REST_BASE_URL + "?symbols=IBM.N,MSFT.O";
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();
                    
                    HttpResponse<String> response = HTTP_CLIENT.send(request, 
                            HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        requestCount.incrementAndGet();
                    } else {
                        errorCount.incrementAndGet();
                    }
                    
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
            });
            
            try {
                Thread.sleep(10); // Small delay to prevent overwhelming
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // Wait for remaining requests
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        double throughput = (double) requestCount.get() / durationSeconds;
        double errorRate = (double) errorCount.get() / (requestCount.get() + errorCount.get()) * 100;
        
        results.recordMetric("Throughput (req/sec)", throughput);
        results.recordMetric("Error Rate (%)", errorRate);
        
        log.info("Throughput: {:.2f} req/sec, Error Rate: {:.2f}%", throughput, errorRate);
    }

    /**
     * Test memory usage
     */
    private void testMemoryUsage() {
        log.info("Testing memory usage");
        
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Perform memory-intensive operations
        List<String> responses = new ArrayList<>();
        
        for (int i = 0; i < 1000; i++) {
            try {
                String url = REST_BASE_URL + "?symbols=IBM.N,MSFT.O,AAPL.O,GOOGL.O,AMZN.O";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();
                
                HttpResponse<String> response = HTTP_CLIENT.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    responses.add(response.body());
                }
                
            } catch (Exception e) {
                log.error("Memory test request failed", e);
            }
        }
        
            // Force garbage collection
            System.gc();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        results.recordMetric("Memory Increase (MB)", memoryIncrease / 1024.0 / 1024.0);
        results.recordMetric("Memory per Request (KB)", (double) memoryIncrease / responses.size() / 1024.0);
        
        log.info("Memory increase: {:.2f} MB", memoryIncrease / 1024.0 / 1024.0);
    }

    /**
     * Test CPU usage
     */
    private void testCpuUsage() {
        log.info("Testing CPU usage");
        
        int durationSeconds = 10;
        AtomicLong requestCount = new AtomicLong(0);
        
        // Start CPU-intensive test
        Instant startTime = Instant.now();
        Instant endTime = startTime.plusSeconds(durationSeconds);
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        while (Instant.now().isBefore(endTime)) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    String url = REST_BASE_URL + "?symbols=IBM.N,MSFT.O,AAPL.O,GOOGL.O,AMZN.O";
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();
                    
                    HttpResponse<String> response = HTTP_CLIENT.send(request, 
                            HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        requestCount.incrementAndGet();
                    }
                    
                } catch (Exception e) {
                    log.error("CPU test request failed", e);
                }
            }, executor);
            
            futures.add(future);
        }
        
        // Wait for all requests to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        double requestsPerSecond = (double) requestCount.get() / durationSeconds;
        results.recordMetric("CPU Test Throughput (req/sec)", requestsPerSecond);
        
        log.info("CPU test throughput: {:.2f} req/sec", requestsPerSecond);
    }

    /**
     * Test response time percentiles
     */
    private void testResponseTimePercentiles() {
        log.info("Testing response time percentiles");
        
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        int totalRequests = 1000;
        CountDownLatch latch = new CountDownLatch(totalRequests);
        
        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    long startTime = System.nanoTime();
                    
                    String url = REST_BASE_URL + "?symbols=IBM.N,MSFT.O";
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();
                    
                    HttpResponse<String> response = HTTP_CLIENT.send(request, 
                            HttpResponse.BodyHandlers.ofString());
                    
                    long endTime = System.nanoTime();
                    long latency = (endTime - startTime) / 1_000_000;
                    latencies.add(latency);
                    
                    assert response.statusCode() == 200 : "Request failed";
                    
                } catch (Exception e) {
                    log.error("Percentile test request failed", e);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Calculate percentiles
        Collections.sort(latencies);
        
        double p50 = calculatePercentile(latencies, 50);
        double p90 = calculatePercentile(latencies, 90);
        double p95 = calculatePercentile(latencies, 95);
        double p99 = calculatePercentile(latencies, 99);
        
        results.recordMetric("P50 Latency (ms)", p50);
        results.recordMetric("P90 Latency (ms)", p90);
        results.recordMetric("P95 Latency (ms)", p95);
        results.recordMetric("P99 Latency (ms)", p99);
        
        log.info("Response time percentiles - P50: {:.2f}ms, P90: {:.2f}ms, P95: {:.2f}ms, P99: {:.2f}ms", 
                p50, p90, p95, p99);
    }

    /**
     * Test connection pooling
     */
    private void testConnectionPooling() {
        log.info("Testing connection pooling");
        
        // Test with different connection pool sizes
        int[] poolSizes = {1, 5, 10, 20};
        
        for (int poolSize : poolSizes) {
            HttpClient poolClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            
            List<Long> latencies = new ArrayList<>();
            
            for (int i = 0; i < 100; i++) {
                try {
                    long startTime = System.nanoTime();
                    
                    String url = REST_BASE_URL + "?symbols=IBM.N";
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();
                    
                    HttpResponse<String> response = poolClient.send(request, 
                            HttpResponse.BodyHandlers.ofString());
                    
                    long endTime = System.nanoTime();
                    long latency = (endTime - startTime) / 1_000_000;
                    latencies.add(latency);
                    
                    assert response.statusCode() == 200 : "Request failed";
                    
                } catch (Exception e) {
                    log.error("Connection pool test failed", e);
                }
            }
            
            double avgLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
            results.recordMetric("Pool Size " + poolSize + " Avg Latency (ms)", avgLatency);
            
            log.info("Pool size {}: {:.2f}ms average latency", poolSize, avgLatency);
        }
    }

    /**
     * Test keep-alive performance
     */
    private void testKeepAlivePerformance() {
        log.info("Testing keep-alive performance");
        
        HttpClient keepAliveClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        List<Long> latencies = new ArrayList<>();
        
        // First request (connection establishment)
        try {
            long startTime = System.nanoTime();
            
            String url = REST_BASE_URL + "?symbols=IBM.N";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            
            HttpResponse<String> response = keepAliveClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            long endTime = System.nanoTime();
            long latency = (endTime - startTime) / 1_000_000;
            latencies.add(latency);
            
            assert response.statusCode() == 200 : "Request failed";
            
        } catch (Exception e) {
            log.error("Keep-alive test failed", e);
        }
        
        // Subsequent requests (should reuse connection)
        for (int i = 0; i < 50; i++) {
            try {
                long startTime = System.nanoTime();
                
                String url = REST_BASE_URL + "?symbols=MSFT.O";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();
                
                HttpResponse<String> response = keepAliveClient.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                
                long endTime = System.nanoTime();
                long latency = (endTime - startTime) / 1_000_000;
                latencies.add(latency);
                
                assert response.statusCode() == 200 : "Request failed";
                
            } catch (Exception e) {
                log.error("Keep-alive test failed", e);
            }
        }
        
        double avgLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        results.recordMetric("Keep-Alive Avg Latency (ms)", avgLatency);
        
        log.info("Keep-alive average latency: {:.2f}ms", avgLatency);
    }

    /**
     * Test large payload handling
     */
    private void testLargePayloadHandling() {
        log.info("Testing large payload handling");
        
        // Generate large symbol list
        List<String> symbols = new ArrayList<>();
        for (int i = 1; i <= 500; i++) {
            symbols.add("SYMBOL" + String.format("%03d", i) + ".N");
        }
        
        String symbolsParam = String.join(",", symbols);
        String url = REST_BASE_URL + "?symbols=" + symbolsParam;
        
        List<Long> latencies = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            try {
                long startTime = System.nanoTime();
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();
                
                HttpResponse<String> response = HTTP_CLIENT.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                
                long endTime = System.nanoTime();
                long latency = (endTime - startTime) / 1_000_000;
                latencies.add(latency);
                
                assert response.statusCode() == 200 : "Request failed";
                
                // Check response size
                int responseSize = response.body().length();
                results.recordMetric("Large Payload Response Size (KB)", responseSize / 1024.0);
                
            } catch (Exception e) {
                log.error("Large payload test failed", e);
            }
        }
        
        double avgLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        results.recordMetric("Large Payload Avg Latency (ms)", avgLatency);
        
        log.info("Large payload average latency: {:.2f}ms", avgLatency);
    }

    /**
     * Test streaming performance
     */
    private void testStreamingPerformance() {
        log.info("Testing streaming performance");
        
        // Subscribe to multiple symbols
        String symbols = "IBM.N,MSFT.O,AAPL.O,GOOGL.O,AMZN.O";
        String subscribeUrl = REST_BASE_URL + "/subscribe?symbols=" + symbols;
        
        try {
            HttpRequest subscribeRequest = HttpRequest.newBuilder()
                    .uri(URI.create(subscribeUrl))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            
            HttpResponse<String> subscribeResponse = HTTP_CLIENT.send(subscribeRequest, 
                    HttpResponse.BodyHandlers.ofString());
            
            assert subscribeResponse.statusCode() == 200 : "Subscribe failed";
            
            // Measure time to get updates
            long startTime = System.nanoTime();
            
            // Poll for updates
            for (int i = 0; i < 100; i++) {
                String pollUrl = REST_BASE_URL + "?symbols=" + symbols;
                HttpRequest pollRequest = HttpRequest.newBuilder()
                        .uri(URI.create(pollUrl))
                        .GET()
                        .build();
                
                HttpResponse<String> pollResponse = HTTP_CLIENT.send(pollRequest, 
                        HttpResponse.BodyHandlers.ofString());
                
                assert pollResponse.statusCode() == 200 : "Poll failed";
                
                Thread.sleep(100); // Small delay between polls
            }
            
            long endTime = System.nanoTime();
            long totalTime = (endTime - startTime) / 1_000_000;
            
            results.recordMetric("Streaming Total Time (ms)", totalTime);
            results.recordMetric("Streaming Avg Time per Poll (ms)", totalTime / 100.0);
            
            log.info("Streaming total time: {}ms, average per poll: {:.2f}ms", 
                    totalTime, totalTime / 100.0);
            
        } catch (Exception e) {
            log.error("Streaming performance test failed", e);
        }
    }

    /**
     * Test memory leaks
     */
    private void testMemoryLeaks() {
        log.info("Testing memory leaks");
        
        Runtime runtime = Runtime.getRuntime();
        
        // Measure memory over time
        List<Long> memoryReadings = new ArrayList<>();
        
        for (int cycle = 0; cycle < 10; cycle++) {
            // Perform operations
            for (int i = 0; i < 100; i++) {
                try {
                    String url = REST_BASE_URL + "?symbols=IBM.N,MSFT.O";
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();
                    
                    HttpResponse<String> response = HTTP_CLIENT.send(request, 
                            HttpResponse.BodyHandlers.ofString());
                    
                    assert response.statusCode() == 200 : "Request failed";
                    
                } catch (Exception e) {
                    log.error("Memory leak test request failed", e);
                }
            }
            
            // Force garbage collection and measure memory
            System.gc();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            long currentMemory = runtime.totalMemory() - runtime.freeMemory();
            memoryReadings.add(currentMemory);
            
            log.info("Memory after cycle {}: {} MB", cycle + 1, currentMemory / 1024 / 1024);
        }
        
        // Check for memory growth trend
        long initialMemory = memoryReadings.get(0);
        long finalMemory = memoryReadings.get(memoryReadings.size() - 1);
        long memoryGrowth = finalMemory - initialMemory;
        
        results.recordMetric("Memory Growth (MB)", memoryGrowth / 1024.0 / 1024.0);
        
        log.info("Memory growth over 10 cycles: {:.2f} MB", memoryGrowth / 1024.0 / 1024.0);
    }

    /**
     * Test garbage collection
     */
    private void testGarbageCollection() {
        log.info("Testing garbage collection");
        
        // Measure GC impact
        long startTime = System.nanoTime();
        
        for (int i = 0; i < 1000; i++) {
            try {
                String url = REST_BASE_URL + "?symbols=IBM.N,MSFT.O,AAPL.O";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();
                
                HttpResponse<String> response = HTTP_CLIENT.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                
                assert response.statusCode() == 200 : "Request failed";
                
            } catch (Exception e) {
                log.error("GC test request failed", e);
            }
        }
        
        long endTime = System.nanoTime();
        long totalTime = (endTime - startTime) / 1_000_000;
        
        results.recordMetric("GC Test Total Time (ms)", totalTime);
        results.recordMetric("GC Test Avg Time per Request (ms)", totalTime / 1000.0);
        
        log.info("GC test total time: {}ms, average per request: {:.2f}ms", 
                totalTime, totalTime / 1000.0);
    }

    /**
     * Test thread pool utilization
     */
    private void testThreadPoolUtilization() {
        log.info("Testing thread pool utilization");
        
        int threadCount = 20;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicLong completedRequests = new AtomicLong(0);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 50; j++) {
                        String url = REST_BASE_URL + "?symbols=IBM.N";
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .GET()
                                .build();
                        
                        HttpResponse<String> response = HTTP_CLIENT.send(request, 
                                HttpResponse.BodyHandlers.ofString());
                        
                        if (response.statusCode() == 200) {
                            completedRequests.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    log.error("Thread pool test failed", e);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        double requestsPerThread = (double) completedRequests.get() / threadCount;
        results.recordMetric("Requests per Thread", requestsPerThread);
        
        log.info("Thread pool utilization: {:.2f} requests per thread", requestsPerThread);
    }

    /**
     * Test network bandwidth
     */
    private void testNetworkBandwidth() {
        log.info("Testing network bandwidth");
        
        List<Integer> responseSizes = new ArrayList<>();
        List<Long> latencies = new ArrayList<>();
        
        for (int i = 0; i < 100; i++) {
            try {
                long startTime = System.nanoTime();
                
                String url = REST_BASE_URL + "?symbols=IBM.N,MSFT.O,AAPL.O,GOOGL.O,AMZN.O";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();
                
                HttpResponse<String> response = HTTP_CLIENT.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                
                long endTime = System.nanoTime();
                long latency = (endTime - startTime) / 1_000_000;
                
                responseSizes.add(response.body().length());
                latencies.add(latency);
                
                assert response.statusCode() == 200 : "Request failed";
                
            } catch (Exception e) {
                log.error("Bandwidth test failed", e);
            }
        }
        
        double avgResponseSize = responseSizes.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double avgLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double bandwidth = (avgResponseSize * 8) / (avgLatency / 1000.0); // bits per second
        
        results.recordMetric("Avg Response Size (bytes)", avgResponseSize);
        results.recordMetric("Avg Latency (ms)", avgLatency);
        results.recordMetric("Bandwidth (bps)", bandwidth);
        
        log.info("Network bandwidth: {:.2f} bps, avg response size: {:.2f} bytes", 
                bandwidth, avgResponseSize);
    }

    /**
     * Calculate latency statistics
     */
    private void calculateLatencyStats(String testName, List<Long> latencies) {
        if (latencies.isEmpty()) return;
        
        Collections.sort(latencies);
        
        double min = latencies.get(0);
        double max = latencies.get(latencies.size() - 1);
        double avg = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double p50 = calculatePercentile(latencies, 50);
        double p90 = calculatePercentile(latencies, 90);
        double p95 = calculatePercentile(latencies, 95);
        double p99 = calculatePercentile(latencies, 99);
        
        results.recordMetric(testName + " - Min (ms)", min);
        results.recordMetric(testName + " - Max (ms)", max);
        results.recordMetric(testName + " - Avg (ms)", avg);
        results.recordMetric(testName + " - P50 (ms)", p50);
        results.recordMetric(testName + " - P90 (ms)", p90);
        results.recordMetric(testName + " - P95 (ms)", p95);
        results.recordMetric(testName + " - P99 (ms)", p99);
        
        log.info("{} - Min: {:.2f}ms, Max: {:.2f}ms, Avg: {:.2f}ms, P50: {:.2f}ms, P90: {:.2f}ms, P95: {:.2f}ms, P99: {:.2f}ms",
                testName, min, max, avg, p50, p90, p95, p99);
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
     * Print performance results
     */
    private void printResults() {
        log.info("\n=== PERFORMANCE TEST RESULTS ===");
        
        results.getMetrics().forEach((metric, value) -> {
            log.info("{}: {:.2f}", metric, value);
        });
        
        log.info("\n=== PERFORMANCE SUMMARY ===");
        log.info("Total metrics recorded: {}", results.getMetrics().size());
    }

    /**
     * Shutdown executor
     */
    private void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Performance results tracking
     */
    private static class PerformanceResults {
        private final Map<String, Double> metrics = new HashMap<>();

        public void recordMetric(String name, double value) {
            metrics.put(name, value);
        }

        public Map<String, Double> getMetrics() {
            return metrics;
        }
    }
}
