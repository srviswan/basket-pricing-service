package com.srviswan.basketpricing.examples;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srviswan.basketpricing.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Example comparing REST and gRPC client performance and features.
 * This demonstrates the differences between the two approaches:
 * - Performance comparison
 * - Feature comparison
 * - Use case recommendations
 */
@Slf4j
public class ClientComparisonExample {

    private static final String REST_BASE_URL = "http://localhost:8080/api/prices";
    private static final String GRPC_HOST = "localhost";
    private static final int GRPC_PORT = 9090;
    
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) {
        try {
            log.info("Starting Client Comparison Example");
            
            // Performance comparison
            performanceComparison();
            
            // Feature comparison
            featureComparison();
            
            // Use case recommendations
            useCaseRecommendations();
            
            log.info("Client Comparison Example completed successfully");
            
        } catch (Exception e) {
            log.error("Error in Client Comparison Example", e);
        }
    }

    /**
     * Performance comparison between REST and gRPC
     */
    public static void performanceComparison() {
        log.info("=== Performance Comparison ===");
        
        String[] symbols = {"IBM.N", "MSFT.O", "AAPL.O"};
        int iterations = 10;
        
        // REST API performance test
        long restTotalTime = 0;
        for (int i = 0; i < iterations; i++) {
            long startTime = System.currentTimeMillis();
            try {
                getPricesRest(symbols);
                long endTime = System.currentTimeMillis();
                restTotalTime += (endTime - startTime);
            } catch (Exception e) {
                log.warn("REST call {} failed: {}", i + 1, e.getMessage());
            }
        }
        
        // gRPC performance test
        long grpcTotalTime = 0;
        ManagedChannel channel = null;
        try {
            channel = ManagedChannelBuilder.forAddress(GRPC_HOST, GRPC_PORT)
                    .usePlaintext()
                    .build();
            PricingServiceGrpc.PricingServiceBlockingStub stub = 
                    PricingServiceGrpc.newBlockingStub(channel);
            
            for (int i = 0; i < iterations; i++) {
                long startTime = System.currentTimeMillis();
                try {
                    getPricesGrpc(stub, symbols);
                    long endTime = System.currentTimeMillis();
                    grpcTotalTime += (endTime - startTime);
                } catch (Exception e) {
                    log.warn("gRPC call {} failed: {}", i + 1, e.getMessage());
                }
            }
        } finally {
            if (channel != null) {
                channel.shutdown();
            }
        }
        
        // Results
        double restAvgTime = (double) restTotalTime / iterations;
        double grpcAvgTime = (double) grpcTotalTime / iterations;
        
        log.info("Performance Results ({} iterations):", iterations);
        log.info("  REST API - Total: {}ms, Average: {:.2f}ms", restTotalTime, restAvgTime);
        log.info("  gRPC API - Total: {}ms, Average: {:.2f}ms", grpcTotalTime, grpcAvgTime);
        
        if (grpcAvgTime < restAvgTime) {
            double improvement = ((restAvgTime - grpcAvgTime) / restAvgTime) * 100;
            log.info("  gRPC is {:.1f}% faster than REST", improvement);
        } else {
            double improvement = ((grpcAvgTime - restAvgTime) / grpcAvgTime) * 100;
            log.info("  REST is {:.1f}% faster than gRPC", improvement);
        }
    }

    /**
     * Feature comparison between REST and gRPC
     */
    public static void featureComparison() {
        log.info("=== Feature Comparison ===");
        
        log.info("REST API Features:");
        log.info("  ✓ Simple HTTP requests");
        log.info("  ✓ JSON response format");
        log.info("  ✓ Easy to test with curl/Postman");
        log.info("  ✓ Browser-friendly");
        log.info("  ✓ Stateless requests");
        log.info("  ✗ No real-time streaming");
        log.info("  ✗ Larger payload size");
        log.info("  ✗ Text-based protocol");
        
        log.info("gRPC API Features:");
        log.info("  ✓ Binary protocol (faster)");
        log.info("  ✓ Real-time streaming");
        log.info("  ✓ Strong typing with Protocol Buffers");
        log.info("  ✓ HTTP/2 multiplexing");
        log.info("  ✓ Smaller payload size");
        log.info("  ✓ Bidirectional streaming");
        log.info("  ✗ More complex setup");
        log.info("  ✗ Not browser-friendly");
        log.info("  ✗ Requires code generation");
    }

    /**
     * Use case recommendations
     */
    public static void useCaseRecommendations() {
        log.info("=== Use Case Recommendations ===");
        
        log.info("Use REST API when:");
        log.info("  • Building web applications");
        log.info("  • Simple request/response patterns");
        log.info("  • Need browser compatibility");
        log.info("  • Quick prototyping");
        log.info("  • Third-party integrations");
        log.info("  • Microservices with different languages");
        
        log.info("Use gRPC API when:");
        log.info("  • High-performance requirements");
        log.info("  • Real-time data streaming");
        log.info("  • Internal service communication");
        log.info("  • Mobile applications");
        log.info("  • Microservices with same language");
        log.info("  • Need strong typing");
        log.info("  • Bidirectional communication");
    }

    /**
     * Example: REST API call
     */
    private static void getPricesRest(String[] symbols) throws IOException, InterruptedException {
        String symbolsParam = String.join(",", symbols);
        String url = REST_BASE_URL + "?symbols=" + symbolsParam;
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .build();
        
        HttpResponse<String> response = HTTP_CLIENT.send(request, 
                HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP error: " + response.statusCode());
        }
    }

    /**
     * Example: gRPC API call
     */
    private static void getPricesGrpc(PricingServiceGrpc.PricingServiceBlockingStub stub, String[] symbols) {
        GetPricesRequest.Builder requestBuilder = GetPricesRequest.newBuilder();
        for (String symbol : symbols) {
            requestBuilder.addSymbols(symbol);
        }
        
        GetPricesRequest request = requestBuilder.build();
        GetPricesResponse response = stub.getPrices(request);
        
        // Process response
        response.getPricesList().forEach(snapshot -> {
            // Just accessing the data to simulate processing
            snapshot.getSymbol();
            snapshot.getBid();
            snapshot.getAsk();
            snapshot.getLast();
        });
    }

    /**
     * Example: Real-time streaming comparison
     */
    public static void streamingComparison() {
        log.info("=== Streaming Comparison ===");
        
        // REST API - Polling simulation
        log.info("REST API - Polling approach:");
        log.info("  • Client polls server every X seconds");
        log.info("  • Higher latency");
        log.info("  • More network overhead");
        log.info("  • Simpler implementation");
        
        // gRPC API - Real-time streaming
        log.info("gRPC API - Real-time streaming:");
        log.info("  • Server pushes updates to client");
        log.info("  • Lower latency");
        log.info("  • Less network overhead");
        log.info("  • More complex implementation");
        
        // Demonstrate gRPC streaming
        demonstrateGrpcStreaming();
    }

    /**
     * Demonstrate gRPC streaming capabilities
     */
    private static void demonstrateGrpcStreaming() {
        ManagedChannel channel = null;
        try {
            channel = ManagedChannelBuilder.forAddress(GRPC_HOST, GRPC_PORT)
                    .usePlaintext()
                    .build();
            
            PricingServiceGrpc.PricingServiceStub stub = PricingServiceGrpc.newStub(channel);
            
            StreamPricesRequest request = StreamPricesRequest.newBuilder()
                    .addSymbols("IBM.N")
                    .addSymbols("MSFT.O")
                    .build();
            
            CountDownLatch finishLatch = new CountDownLatch(1);
            
            StreamObserver<PriceUpdate> responseObserver = new StreamObserver<PriceUpdate>() {
                private int updateCount = 0;
                
                @Override
                public void onNext(PriceUpdate update) {
                    updateCount++;
                    log.info("Stream Update #{} - {}: Bid={}, Ask={}, Last={}",
                            updateCount,
                            update.getSymbol(),
                            update.getSnapshot().getBid(),
                            update.getSnapshot().getAsk(),
                            update.getSnapshot().getLast());
                    
                    // Stop after 5 updates for demo
                    if (updateCount >= 5) {
                        finishLatch.countDown();
                    }
                }

                @Override
                public void onError(Throwable t) {
                    log.error("Stream error: {}", t.getMessage());
                    finishLatch.countDown();
                }

                @Override
                public void onCompleted() {
                    log.info("Stream completed after {} updates", updateCount);
                    finishLatch.countDown();
                }
            };
            
            stub.streamPrices(request, responseObserver);
            
            // Wait for stream to complete
            if (!finishLatch.await(10, TimeUnit.SECONDS)) {
                log.warn("Stream timeout after 10 seconds");
            }
            
        } catch (Exception e) {
            log.error("Error demonstrating gRPC streaming", e);
        } finally {
            if (channel != null) {
                channel.shutdown();
            }
        }
    }
}
