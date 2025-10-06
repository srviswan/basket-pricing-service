package com.srviswan.basketpricing.examples;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Example REST API client demonstrating how to interact with the pricing service REST endpoints.
 * This client shows how to:
 * - Get current prices for symbols
 * - Subscribe to price updates
 * - Unsubscribe from symbols
 * - Get current subscriptions
 */
@Slf4j
public class RestApiClientExample {

    private static final String BASE_URL = "http://localhost:8080/api/prices";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        try {
            log.info("Starting REST API Client Example");
            
            // Example 1: Get current prices
            getCurrentPrices();
            
            // Example 2: Subscribe to symbols
            subscribeToSymbols();
            
            // Example 3: Get current subscriptions
            getCurrentSubscriptions();
            
            // Example 4: Unsubscribe from symbols
            unsubscribeFromSymbols();
            
            log.info("REST API Client Example completed successfully");
            
        } catch (Exception e) {
            log.error("Error in REST API Client Example", e);
        }
    }

    /**
     * Example: Get current prices for multiple symbols
     */
    public static void getCurrentPrices() {
        try {
            log.info("=== Getting Current Prices ===");
            
            String symbols = "IBM.N,MSFT.O,AAPL.O";
            String url = BASE_URL + "?symbols=" + symbols;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .header("Accept", "application/json")
                    .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                log.info("Prices Response: {}", responseBody);
                
                // Parse and display prices
                @SuppressWarnings("unchecked")
                Map<String, Object> prices = OBJECT_MAPPER.readValue(responseBody, Map.class);
                prices.forEach((symbol, priceData) -> {
                    log.info("Symbol: {} - Price: {}", symbol, priceData);
                });
            } else {
                log.error("Failed to get prices. Status: {}, Body: {}", 
                        response.statusCode(), response.body());
            }
            
        } catch (IOException | InterruptedException e) {
            log.error("Error getting current prices", e);
        }
    }

    /**
     * Example: Subscribe to price updates for symbols
     */
    public static void subscribeToSymbols() {
        try {
            log.info("=== Subscribing to Symbols ===");
            
            String symbols = "IBM.N,MSFT.O";
            String url = BASE_URL + "/subscribe?symbols=" + symbols;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .header("Content-Type", "application/json")
                    .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                log.info("Subscription Response: {}", responseBody);
                
                // Parse subscription response
                @SuppressWarnings("unchecked")
                Map<String, Object> subscriptionResponse = OBJECT_MAPPER.readValue(responseBody, Map.class);
                log.info("Subscribed to {} symbols", subscriptionResponse.get("subscribed"));
                log.info("Total subscriptions: {}", subscriptionResponse.get("totalSubscriptions"));
                
                // Display backpressure status
                @SuppressWarnings("unchecked")
                Map<String, Object> backpressureStatus = (Map<String, Object>) subscriptionResponse.get("backpressureStatus");
                if (backpressureStatus != null) {
                    log.info("Backpressure - Queue utilization: {}%, Processed updates: {}, Dropped updates: {}",
                            backpressureStatus.get("queueUtilization"),
                            backpressureStatus.get("processedUpdates"),
                            backpressureStatus.get("droppedUpdates"));
                }
            } else {
                log.error("Failed to subscribe. Status: {}, Body: {}", 
                        response.statusCode(), response.body());
            }
            
        } catch (IOException | InterruptedException e) {
            log.error("Error subscribing to symbols", e);
        }
    }

    /**
     * Example: Get current subscriptions
     */
    public static void getCurrentSubscriptions() {
        try {
            log.info("=== Getting Current Subscriptions ===");
            
            String url = BASE_URL + "/subscriptions";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .header("Accept", "application/json")
                    .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                log.info("Subscriptions Response: {}", responseBody);
                
                // Parse subscriptions response
                @SuppressWarnings("unchecked")
                Map<String, Object> subscriptionsResponse = OBJECT_MAPPER.readValue(responseBody, Map.class);
                @SuppressWarnings("unchecked")
                List<String> subscribedSymbols = (List<String>) subscriptionsResponse.get("subscribedSymbols");
                Integer count = (Integer) subscriptionsResponse.get("count");
                
                log.info("Currently subscribed to {} symbols:", count);
                subscribedSymbols.forEach(symbol -> log.info("  - {}", symbol));
            } else {
                log.error("Failed to get subscriptions. Status: {}, Body: {}", 
                        response.statusCode(), response.body());
            }
            
        } catch (IOException | InterruptedException e) {
            log.error("Error getting current subscriptions", e);
        }
    }

    /**
     * Example: Unsubscribe from symbols
     */
    public static void unsubscribeFromSymbols() {
        try {
            log.info("=== Unsubscribing from Symbols ===");
            
            String symbols = "IBM.N";
            String url = BASE_URL + "/unsubscribe?symbols=" + symbols;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .DELETE()
                    .header("Content-Type", "application/json")
                    .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                log.info("Unsubscription Response: {}", responseBody);
                
                // Parse unsubscription response
                @SuppressWarnings("unchecked")
                Map<String, Object> unsubscriptionResponse = OBJECT_MAPPER.readValue(responseBody, Map.class);
                @SuppressWarnings("unchecked")
                List<String> unsubscribedSymbols = (List<String>) unsubscriptionResponse.get("unsubscribed");
                Integer remainingSubscriptions = (Integer) unsubscriptionResponse.get("remainingSubscriptions");
                
                log.info("Unsubscribed from {} symbols", unsubscribedSymbols);
                log.info("Remaining subscriptions: {}", remainingSubscriptions);
            } else {
                log.error("Failed to unsubscribe. Status: {}, Body: {}", 
                        response.statusCode(), response.body());
            }
            
        } catch (IOException | InterruptedException e) {
            log.error("Error unsubscribing from symbols", e);
        }
    }

    /**
     * Example: Continuous price polling
     */
    public static void continuousPricePolling() {
        try {
            log.info("=== Starting Continuous Price Polling ===");
            
            String symbols = "IBM.N,MSFT.O";
            String url = BASE_URL + "?symbols=" + symbols;
            
            for (int i = 0; i < 10; i++) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .header("Accept", "application/json")
                        .build();
                
                HttpResponse<String> response = HTTP_CLIENT.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    String responseBody = response.body();
                    log.info("Poll #{} - Prices: {}", i + 1, responseBody);
                } else {
                    log.error("Poll #{} failed. Status: {}", i + 1, response.statusCode());
                }
                
                // Wait 2 seconds before next poll
                Thread.sleep(2000);
            }
            
        } catch (IOException | InterruptedException e) {
            log.error("Error in continuous price polling", e);
        }
    }

    /**
     * Example: Error handling and retry logic
     */
    public static void getPricesWithRetry() {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                log.info("=== Getting Prices with Retry (Attempt {}) ===", retryCount + 1);
                
                String symbols = "IBM.N";
                String url = BASE_URL + "?symbols=" + symbols;
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(5))
                        .build();
                
                HttpResponse<String> response = HTTP_CLIENT.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    log.info("Success! Prices: {}", response.body());
                    return; // Success, exit retry loop
                } else {
                    log.warn("HTTP error: {} - {}", response.statusCode(), response.body());
                }
                
            } catch (IOException | InterruptedException e) {
                log.warn("Attempt {} failed: {}", retryCount + 1, e.getMessage());
            }
            
            retryCount++;
            if (retryCount < maxRetries) {
                try {
                    Thread.sleep(1000 * retryCount); // Exponential backoff
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        log.error("Failed to get prices after {} attempts", maxRetries);
    }
}
