package com.srviswan.basketpricing.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive regression test suite for the basket pricing service.
 * Tests all API endpoints, error scenarios, and edge cases to ensure
 * system stability and functionality.
 */
@Slf4j
public class RegressionTestSuite {

    private static final String REST_BASE_URL = "http://localhost:8080/api/prices";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private final TestResults testResults = new TestResults();
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        try {
            log.info("Starting Regression Test Suite");
            
            RegressionTestSuite suite = new RegressionTestSuite();
            boolean allTestsPassed = suite.runAllTests();
            
            suite.printResults();
            suite.shutdown();
            
            if (allTestsPassed) {
                log.info("All regression tests PASSED");
                System.exit(0);
            } else {
                log.error("Some regression tests FAILED");
                System.exit(1);
            }
            
        } catch (Exception e) {
            log.error("Regression test suite failed", e);
            System.exit(1);
        }
    }

    public boolean runAllTests() {
        log.info("=== Running Regression Test Suite ===");
        
        // API Endpoint Tests
        testGetPricesEndpoint();
        testSubscribeEndpoint();
        testUnsubscribeEndpoint();
        testGetSubscriptionsEndpoint();
        
        // Error Handling Tests
        testInvalidSymbols();
        testEmptyRequests();
        testMalformedRequests();
        testServiceUnavailable();
        
        // Edge Case Tests
        testLargeSymbolList();
        testSpecialCharacters();
        testLongSymbolNames();
        testDuplicateSymbols();
        
        // Concurrent Access Tests
        testConcurrentRequests();
        testConcurrentSubscriptions();
        testRaceConditions();
        
        // Data Consistency Tests
        testSubscriptionConsistency();
        testPriceDataConsistency();
        testBackpressureHandling();
        
        // Performance Regression Tests
        testResponseTimeRegression();
        testMemoryUsageRegression();
        
        return testResults.getFailureCount() == 0;
    }

    /**
     * Test GET /api/prices endpoint
     */
    private void testGetPricesEndpoint() {
        log.info("Testing GET /api/prices endpoint");
        
        // Test 1: Valid symbols
        runTest("GET prices - valid symbols", () -> {
            String symbols = "IBM.N,MSFT.O,AAPL.O";
            String url = REST_BASE_URL + "?symbols=" + symbols;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .header("Accept", "application/json")
                    .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            assert response.statusCode() == 200 : "Expected 200, got " + response.statusCode();
            
            Map<String, Object> prices = OBJECT_MAPPER.readValue(response.body(), Map.class);
            assert prices != null : "Response should not be null";
            
            log.info("GET prices test passed - received {} price snapshots", prices.size());
        });
        
        // Test 2: Single symbol
        runTest("GET prices - single symbol", () -> {
            String url = REST_BASE_URL + "?symbols=IBM.N";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            assert response.statusCode() == 200 : "Expected 200, got " + response.statusCode();
        });
        
        // Test 3: No symbols parameter
        runTest("GET prices - no symbols parameter", () -> {
            String url = REST_BASE_URL;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            // Should handle gracefully (400 or empty response)
            assert response.statusCode() == 400 || response.statusCode() == 200 : 
                "Expected 400 or 200, got " + response.statusCode();
        });
    }

    /**
     * Test POST /api/prices/subscribe endpoint
     */
    private void testSubscribeEndpoint() {
        log.info("Testing POST /api/prices/subscribe endpoint");
        
        // Test 1: Valid subscription
        runTest("Subscribe - valid symbols", () -> {
            String symbols = "IBM.N,MSFT.O";
            String url = REST_BASE_URL + "/subscribe?symbols=" + symbols;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .header("Content-Type", "application/json")
                    .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            assert response.statusCode() == 200 : "Expected 200, got " + response.statusCode();
            
            Map<String, Object> result = OBJECT_MAPPER.readValue(response.body(), Map.class);
            assert result.containsKey("subscribed") : "Response should contain 'subscribed'";
            assert result.containsKey("totalSubscriptions") : "Response should contain 'totalSubscriptions'";
        });
        
        // Test 2: Duplicate subscription
        runTest("Subscribe - duplicate symbols", () -> {
            String symbols = "IBM.N"; // Already subscribed above
            String url = REST_BASE_URL + "/subscribe?symbols=" + symbols;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            // Should handle gracefully (200 or 409)
            assert response.statusCode() == 200 || response.statusCode() == 409 : 
                "Expected 200 or 409, got " + response.statusCode();
        });
    }

    /**
     * Test DELETE /api/prices/unsubscribe endpoint
     */
    private void testUnsubscribeEndpoint() {
        log.info("Testing DELETE /api/prices/unsubscribe endpoint");
        
        // Test 1: Valid unsubscription
        runTest("Unsubscribe - valid symbols", () -> {
            String symbols = "MSFT.O";
            String url = REST_BASE_URL + "/unsubscribe?symbols=" + symbols;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .DELETE()
                    .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            assert response.statusCode() == 200 : "Expected 200, got " + response.statusCode();
            
            Map<String, Object> result = OBJECT_MAPPER.readValue(response.body(), Map.class);
            assert result.containsKey("unsubscribed") : "Response should contain 'unsubscribed'";
            assert result.containsKey("remainingSubscriptions") : "Response should contain 'remainingSubscriptions'";
        });
        
        // Test 2: Unsubscribe from non-existent symbol
        runTest("Unsubscribe - non-existent symbol", () -> {
            String symbols = "NONEXISTENT.SYMBOL";
            String url = REST_BASE_URL + "/unsubscribe?symbols=" + symbols;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .DELETE()
                    .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            // Should handle gracefully (200 or 404)
            assert response.statusCode() == 200 || response.statusCode() == 404 : 
                "Expected 200 or 404, got " + response.statusCode();
        });
    }

    /**
     * Test GET /api/prices/subscriptions endpoint
     */
    private void testGetSubscriptionsEndpoint() {
        log.info("Testing GET /api/prices/subscriptions endpoint");
        
        runTest("Get subscriptions", () -> {
            String url = REST_BASE_URL + "/subscriptions";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            assert response.statusCode() == 200 : "Expected 200, got " + response.statusCode();
            
            Map<String, Object> result = OBJECT_MAPPER.readValue(response.body(), Map.class);
            assert result.containsKey("subscribedSymbols") : "Response should contain 'subscribedSymbols'";
            assert result.containsKey("count") : "Response should contain 'count'";
            
            List<String> symbols = (List<String>) result.get("subscribedSymbols");
            Integer count = (Integer) result.get("count");
            assert symbols.size() == count : "Symbol count mismatch";
        });
    }

    /**
     * Test error handling for invalid symbols
     */
    private void testInvalidSymbols() {
        log.info("Testing invalid symbols handling");
        
        runTest("Invalid symbols - special characters", () -> {
            String symbols = "INVALID@SYMBOL,ANOTHER#SYMBOL";
            String url = REST_BASE_URL + "?symbols=" + symbols;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            // Should handle gracefully (400 or 200 with empty results)
            assert response.statusCode() == 400 || response.statusCode() == 200 : 
                "Expected 400 or 200, got " + response.statusCode();
        });
        
        runTest("Invalid symbols - empty strings", () -> {
            String symbols = ",,EMPTY,,";
            String url = REST_BASE_URL + "?symbols=" + symbols;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            assert response.statusCode() == 200 || response.statusCode() == 400 : 
                "Expected 200 or 400, got " + response.statusCode();
        });
    }

    /**
     * Test empty request handling
     */
    private void testEmptyRequests() {
        log.info("Testing empty request handling");
        
        runTest("Empty symbols parameter", () -> {
            String url = REST_BASE_URL + "?symbols=";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            assert response.statusCode() == 200 || response.statusCode() == 400 : 
                "Expected 200 or 400, got " + response.statusCode();
        });
        
        runTest("Whitespace-only symbols", () -> {
            String url = REST_BASE_URL + "?symbols=%20%20%20";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            assert response.statusCode() == 200 || response.statusCode() == 400 : 
                "Expected 200 or 400, got " + response.statusCode();
        });
    }

    /**
     * Test malformed request handling
     */
    private void testMalformedRequests() {
        log.info("Testing malformed request handling");
        
        runTest("Malformed JSON in request body", () -> {
            String url = REST_BASE_URL + "/subscribe";
            String malformedJson = "{invalid json}";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(malformedJson))
                    .header("Content-Type", "application/json")
                    .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            // Should handle gracefully (400 or 415)
            assert response.statusCode() == 400 || response.statusCode() == 415 : 
                "Expected 400 or 415, got " + response.statusCode();
        });
    }

    /**
     * Test service unavailable scenarios
     */
    private void testServiceUnavailable() {
        log.info("Testing service unavailable scenarios");
        
        runTest("Service unavailable - wrong port", () -> {
            String url = "http://localhost:9999/api/prices?symbols=IBM.N";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            
            try {
                HttpResponse<String> response = HTTP_CLIENT.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                // If we get here, the test should fail
                assert false : "Expected connection failure";
            } catch (IOException e) {
                // Expected behavior - connection should fail
                log.info("Service unavailable test passed - connection failed as expected");
            }
        });
    }

    /**
     * Test large symbol list handling
     */
    private void testLargeSymbolList() {
        log.info("Testing large symbol list handling");
        
        runTest("Large symbol list - 100 symbols", () -> {
            List<String> symbols = new ArrayList<>();
            for (int i = 1; i <= 100; i++) {
                symbols.add("SYMBOL" + String.format("%03d", i) + ".N");
            }
            String symbolsParam = String.join(",", symbols);
            String url = REST_BASE_URL + "?symbols=" + symbolsParam;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            assert response.statusCode() == 200 : "Expected 200, got " + response.statusCode();
            
            // Should handle large requests gracefully
            assert response.body().length() > 0 : "Response should not be empty";
        });
    }

    /**
     * Test special characters in symbols
     */
    private void testSpecialCharacters() {
        log.info("Testing special characters in symbols");
        
        runTest("Special characters - URL encoding", () -> {
            String symbols = "SYMBOL%20WITH%20SPACES.N,SYMBOL-WITH-DASH.N";
            String url = REST_BASE_URL + "?symbols=" + symbols;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            assert response.statusCode() == 200 || response.statusCode() == 400 : 
                "Expected 200 or 400, got " + response.statusCode();
        });
    }

    /**
     * Test long symbol names
     */
    private void testLongSymbolNames() {
        log.info("Testing long symbol names");
        
        runTest("Long symbol names", () -> {
            String longSymbol = "A".repeat(100) + ".N";
            String url = REST_BASE_URL + "?symbols=" + longSymbol;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            // Should handle gracefully (200, 400, or 414)
            assert response.statusCode() == 200 || response.statusCode() == 400 || response.statusCode() == 414 : 
                "Expected 200, 400, or 414, got " + response.statusCode();
        });
    }

    /**
     * Test duplicate symbols handling
     */
    private void testDuplicateSymbols() {
        log.info("Testing duplicate symbols handling");
        
        runTest("Duplicate symbols in request", () -> {
            String symbols = "IBM.N,MSFT.O,IBM.N,MSFT.O";
            String url = REST_BASE_URL + "?symbols=" + symbols;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            assert response.statusCode() == 200 : "Expected 200, got " + response.statusCode();
            
            Map<String, Object> prices = OBJECT_MAPPER.readValue(response.body(), Map.class);
            // Should deduplicate or handle gracefully
            assert prices != null : "Response should not be null";
        });
    }

    /**
     * Test concurrent request handling
     */
    private void testConcurrentRequests() {
        log.info("Testing concurrent request handling");
        
        runTest("Concurrent requests - 10 simultaneous", () -> {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            for (int i = 0; i < 10; i++) {
                final int requestId = i;
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        String symbols = "IBM.N,MSFT.O";
                        String url = REST_BASE_URL + "?symbols=" + symbols;
                        
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .GET()
                                .build();
                        
                        HttpResponse<String> response = HTTP_CLIENT.send(request, 
                                HttpResponse.BodyHandlers.ofString());
                        
                        assert response.statusCode() == 200 : 
                            "Request " + requestId + " failed with status " + response.statusCode();
                        
                    } catch (Exception e) {
                        throw new RuntimeException("Request " + requestId + " failed", e);
                    }
                }, executor);
                
                futures.add(future);
            }
            
            // Wait for all requests to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            log.info("Concurrent requests test passed - all 10 requests completed");
        });
    }

    /**
     * Test concurrent subscription handling
     */
    private void testConcurrentSubscriptions() {
        log.info("Testing concurrent subscription handling");
        
        runTest("Concurrent subscriptions", () -> {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            for (int i = 0; i < 5; i++) {
                final int requestId = i;
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        String symbols = "TEST" + requestId + ".N";
                        String url = REST_BASE_URL + "/subscribe?symbols=" + symbols;
                        
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .POST(HttpRequest.BodyPublishers.noBody())
                                .build();
                        
                        HttpResponse<String> response = HTTP_CLIENT.send(request, 
                                HttpResponse.BodyHandlers.ofString());
                        
                        assert response.statusCode() == 200 : 
                            "Subscription " + requestId + " failed with status " + response.statusCode();
                        
                    } catch (Exception e) {
                        throw new RuntimeException("Subscription " + requestId + " failed", e);
                    }
                }, executor);
                
                futures.add(future);
            }
            
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            log.info("Concurrent subscriptions test passed - all 5 subscriptions completed");
        });
    }

    /**
     * Test race condition scenarios
     */
    private void testRaceConditions() {
        log.info("Testing race condition scenarios");
        
        runTest("Race condition - subscribe and unsubscribe", () -> {
            String symbol = "RACE.TEST";
            
            // Start subscribe and unsubscribe simultaneously
            CompletableFuture<Void> subscribeFuture = CompletableFuture.runAsync(() -> {
                try {
                    String url = REST_BASE_URL + "/subscribe?symbols=" + symbol;
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .build();
                    HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                } catch (Exception e) {
                    throw new RuntimeException("Subscribe failed", e);
                }
            }, executor);
            
            CompletableFuture<Void> unsubscribeFuture = CompletableFuture.runAsync(() -> {
                try {
                    String url = REST_BASE_URL + "/unsubscribe?symbols=" + symbol;
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .DELETE()
                            .build();
                    HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                } catch (Exception e) {
                    throw new RuntimeException("Unsubscribe failed", e);
                }
            }, executor);
            
            // Both should complete without errors
            CompletableFuture.allOf(subscribeFuture, unsubscribeFuture).join();
            log.info("Race condition test passed - subscribe/unsubscribe completed");
        });
    }

    /**
     * Test subscription consistency
     */
    private void testSubscriptionConsistency() {
        log.info("Testing subscription consistency");
        
        runTest("Subscription consistency", () -> {
            String symbol = "CONSISTENCY.TEST";
            
            // Subscribe
            String subscribeUrl = REST_BASE_URL + "/subscribe?symbols=" + symbol;
            HttpRequest subscribeRequest = HttpRequest.newBuilder()
                    .uri(URI.create(subscribeUrl))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            
            HttpResponse<String> subscribeResponse = HTTP_CLIENT.send(subscribeRequest, 
                    HttpResponse.BodyHandlers.ofString());
            assert subscribeResponse.statusCode() == 200 : "Subscribe failed";
            
            // Check subscriptions
            String subscriptionsUrl = REST_BASE_URL + "/subscriptions";
            HttpRequest subscriptionsRequest = HttpRequest.newBuilder()
                    .uri(URI.create(subscriptionsUrl))
                    .GET()
                    .build();
            
            HttpResponse<String> subscriptionsResponse = HTTP_CLIENT.send(subscriptionsRequest, 
                    HttpResponse.BodyHandlers.ofString());
            assert subscriptionsResponse.statusCode() == 200 : "Get subscriptions failed";
            
            Map<String, Object> result = OBJECT_MAPPER.readValue(subscriptionsResponse.body(), Map.class);
            List<String> symbols = (List<String>) result.get("subscribedSymbols");
            assert symbols.contains(symbol) : "Symbol should be in subscriptions list";
            
            // Unsubscribe
            String unsubscribeUrl = REST_BASE_URL + "/unsubscribe?symbols=" + symbol;
            HttpRequest unsubscribeRequest = HttpRequest.newBuilder()
                    .uri(URI.create(unsubscribeUrl))
                    .DELETE()
                    .build();
            
            HttpResponse<String> unsubscribeResponse = HTTP_CLIENT.send(unsubscribeRequest, 
                    HttpResponse.BodyHandlers.ofString());
            assert unsubscribeResponse.statusCode() == 200 : "Unsubscribe failed";
            
            log.info("Subscription consistency test passed");
        });
    }

    /**
     * Test price data consistency
     */
    private void testPriceDataConsistency() {
        log.info("Testing price data consistency");
        
        runTest("Price data consistency", () -> {
            String symbol = "IBM.N";
            String url = REST_BASE_URL + "?symbols=" + symbol;
            
            // Make multiple requests and check consistency
            List<Map<String, Object>> responses = new ArrayList<>();
            
            for (int i = 0; i < 5; i++) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();
                
                HttpResponse<String> response = HTTP_CLIENT.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                
                assert response.statusCode() == 200 : "Request failed";
                
                Map<String, Object> prices = OBJECT_MAPPER.readValue(response.body(), Map.class);
                responses.add(prices);
                
                Thread.sleep(100); // Small delay between requests
            }
            
            // All responses should have the same structure
            for (Map<String, Object> response : responses) {
                assert response != null : "Response should not be null";
                // Additional consistency checks can be added here
            }
            
            log.info("Price data consistency test passed");
        });
    }

    /**
     * Test backpressure handling
     */
    private void testBackpressureHandling() {
        log.info("Testing backpressure handling");
        
        runTest("Backpressure handling", () -> {
            // Subscribe to multiple symbols to generate updates
            String symbols = "IBM.N,MSFT.O,AAPL.O,GOOGL.O,AMZN.O";
            String subscribeUrl = REST_BASE_URL + "/subscribe?symbols=" + symbols;
            
            HttpRequest subscribeRequest = HttpRequest.newBuilder()
                    .uri(URI.create(subscribeUrl))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            
            HttpResponse<String> subscribeResponse = HTTP_CLIENT.send(subscribeRequest, 
                    HttpResponse.BodyHandlers.ofString());
            assert subscribeResponse.statusCode() == 200 : "Subscribe failed";
            
            // Check backpressure status in response
            Map<String, Object> result = OBJECT_MAPPER.readValue(subscribeResponse.body(), Map.class);
            assert result.containsKey("backpressureStatus") : "Response should contain backpressure status";
            
            Map<String, Object> backpressureStatus = (Map<String, Object>) result.get("backpressureStatus");
            assert backpressureStatus.containsKey("queueUtilization") : "Should contain queue utilization";
            assert backpressureStatus.containsKey("processedUpdates") : "Should contain processed updates";
            assert backpressureStatus.containsKey("droppedUpdates") : "Should contain dropped updates";
            
            log.info("Backpressure handling test passed");
        });
    }

    /**
     * Test response time regression
     */
    private void testResponseTimeRegression() {
        log.info("Testing response time regression");
        
        runTest("Response time regression", () -> {
            String symbols = "IBM.N,MSFT.O";
            String url = REST_BASE_URL + "?symbols=" + symbols;
            
            List<Long> responseTimes = new ArrayList<>();
            
            for (int i = 0; i < 10; i++) {
                long startTime = System.currentTimeMillis();
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();
                
                HttpResponse<String> response = HTTP_CLIENT.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                
                long endTime = System.currentTimeMillis();
                long responseTime = endTime - startTime;
                responseTimes.add(responseTime);
                
                assert response.statusCode() == 200 : "Request failed";
                assert responseTime < 5000 : "Response time too high: " + responseTime + "ms";
            }
            
            // Calculate average response time
            double avgResponseTime = responseTimes.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);
            
            assert avgResponseTime < 1000 : "Average response time too high: " + avgResponseTime + "ms";
            
            log.info("Response time regression test passed - average: {}ms", avgResponseTime);
        });
    }

    /**
     * Test memory usage regression
     */
    private void testMemoryUsageRegression() {
        log.info("Testing memory usage regression");
        
        runTest("Memory usage regression", () -> {
            Runtime runtime = Runtime.getRuntime();
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();
            
            // Perform multiple operations
            for (int i = 0; i < 100; i++) {
                String symbols = "TEST" + i + ".N";
                String url = REST_BASE_URL + "?symbols=" + symbols;
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();
                
                HttpResponse<String> response = HTTP_CLIENT.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                
                assert response.statusCode() == 200 : "Request failed";
            }
            
            // Force garbage collection
            System.gc();
            Thread.sleep(1000);
            
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryIncrease = finalMemory - initialMemory;
            
            // Memory increase should be reasonable (less than 50MB)
            assert memoryIncrease < 50 * 1024 * 1024 : 
                "Memory usage increased too much: " + (memoryIncrease / 1024 / 1024) + "MB";
            
            log.info("Memory usage regression test passed - increase: {}MB", 
                    memoryIncrease / 1024 / 1024);
        });
    }

    /**
     * Run a test and record results
     */
    private void runTest(String testName, Runnable test) {
        try {
            Instant startTime = Instant.now();
            test.run();
            Instant endTime = Instant.now();
            
            Duration duration = Duration.between(startTime, endTime);
            testResults.recordSuccess(testName, duration);
            
            log.info("✓ {} - PASSED ({}ms)", testName, duration.toMillis());
            
        } catch (Exception e) {
            testResults.recordFailure(testName, e);
            log.error("✗ {} - FAILED: {}", testName, e.getMessage());
        }
    }

    /**
     * Print test results
     */
    private void printResults() {
        log.info("\n=== REGRESSION TEST RESULTS ===");
        log.info("Total Tests: {}", testResults.getTotalTests());
        log.info("Passed: {}", testResults.getSuccessCount());
        log.info("Failed: {}", testResults.getFailureCount());
        log.info("Success Rate: {:.1f}%", testResults.getSuccessRate());
        
        if (testResults.getFailureCount() > 0) {
            log.error("\nFAILED TESTS:");
            testResults.getFailures().forEach((testName, error) -> {
                log.error("  - {}: {}", testName, error.getMessage());
            });
        }
        
        log.info("\nPERFORMANCE SUMMARY:");
        testResults.getSuccesses().forEach((testName, duration) -> {
            log.info("  - {}: {}ms", testName, duration.toMillis());
        });
    }

    /**
     * Shutdown executor
     */
    private void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Test results tracking
     */
    private static class TestResults {
        private final Map<String, Duration> successes = new HashMap<>();
        private final Map<String, Exception> failures = new HashMap<>();

        public void recordSuccess(String testName, Duration duration) {
            successes.put(testName, duration);
        }

        public void recordFailure(String testName, Exception error) {
            failures.put(testName, error);
        }

        public int getTotalTests() {
            return successes.size() + failures.size();
        }

        public int getSuccessCount() {
            return successes.size();
        }

        public int getFailureCount() {
            return failures.size();
        }

        public double getSuccessRate() {
            int total = getTotalTests();
            return total > 0 ? (double) getSuccessCount() / total * 100 : 0;
        }

        public Map<String, Duration> getSuccesses() {
            return successes;
        }

        public Map<String, Exception> getFailures() {
            return failures;
        }
    }
}
