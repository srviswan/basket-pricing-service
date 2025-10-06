package com.srviswan.basketpricing.examples;

import com.srviswan.basketpricing.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Example gRPC client demonstrating how to interact with the pricing service gRPC endpoints.
 * This client shows how to:
 * - Get current prices for symbols
 * - Subscribe to price updates
 * - Unsubscribe from symbols
 * - Get current subscriptions
 * - Stream real-time price updates
 */
@Slf4j
public class GrpcClientExample {

    private static final String HOST = "localhost";
    private static final int PORT = 9090;
    
    private final ManagedChannel channel;
    private final PricingServiceGrpc.PricingServiceBlockingStub blockingStub;
    private final PricingServiceGrpc.PricingServiceStub asyncStub;

    public GrpcClientExample(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext() // For local development only
                .build();
        this.blockingStub = PricingServiceGrpc.newBlockingStub(channel);
        this.asyncStub = PricingServiceGrpc.newStub(channel);
    }

    public static void main(String[] args) {
        try {
            log.info("Starting gRPC Client Example");
            
            GrpcClientExample client = new GrpcClientExample(HOST, PORT);
            
            // Example 1: Get current prices (blocking call)
            client.getCurrentPrices();
            
            // Example 2: Subscribe to symbols (blocking call)
            client.subscribeToSymbols();
            
            // Example 3: Get current subscriptions (blocking call)
            client.getCurrentSubscriptions();
            
            // Example 4: Stream real-time price updates (async call)
            client.streamPriceUpdates();
            
            // Example 5: Unsubscribe from symbols (blocking call)
            client.unsubscribeFromSymbols();
            
            // Example 6: Async subscription with callback
            client.subscribeAsync();
            
            client.shutdown();
            
            log.info("gRPC Client Example completed successfully");
            
        } catch (Exception e) {
            log.error("Error in gRPC Client Example", e);
        }
    }

    /**
     * Example: Get current prices for multiple symbols (blocking call)
     */
    public void getCurrentPrices() {
        try {
            log.info("=== Getting Current Prices (Blocking) ===");
            
            GetPricesRequest request = GetPricesRequest.newBuilder()
                    .addSymbols("IBM.N")
                    .addSymbols("MSFT.O")
                    .addSymbols("AAPL.O")
                    .build();
            
            GetPricesResponse response = blockingStub.getPrices(request);
            
            log.info("Received {} price snapshots:", response.getPricesCount());
            for (PriceSnapshot snapshot : response.getPricesList()) {
                log.info("Symbol: {}, Bid: {}, Ask: {}, Last: {}, Timestamp: {}",
                        snapshot.getSymbol(),
                        snapshot.getBid(),
                        snapshot.getAsk(),
                        snapshot.getLast(),
                        snapshot.getTimestamp());
            }
            
        } catch (Exception e) {
            log.error("Error getting current prices", e);
        }
    }

    /**
     * Example: Subscribe to price updates for symbols (blocking call)
     */
    public void subscribeToSymbols() {
        try {
            log.info("=== Subscribing to Symbols (Blocking) ===");
            
            SubscribeRequest request = SubscribeRequest.newBuilder()
                    .addSymbols("IBM.N")
                    .addSymbols("MSFT.O")
                    .build();
            
            SubscribeResponse response = blockingStub.subscribe(request);
            
            if (response.getSuccess()) {
                log.info("Successfully subscribed to {} symbols", response.getSubscribedSymbolsCount());
                log.info("Subscribed symbols: {}", response.getSubscribedSymbolsList());
                log.info("Total subscriptions: {}", response.getTotalSubscriptions());
                log.info("Message: {}", response.getMessage());
            } else {
                log.error("Subscription failed: {}", response.getMessage());
            }
            
        } catch (Exception e) {
            log.error("Error subscribing to symbols", e);
        }
    }

    /**
     * Example: Get current subscriptions (blocking call)
     */
    public void getCurrentSubscriptions() {
        try {
            log.info("=== Getting Current Subscriptions (Blocking) ===");
            
            GetSubscriptionsRequest request = GetSubscriptionsRequest.newBuilder().build();
            
            GetSubscriptionsResponse response = blockingStub.getSubscriptions(request);
            
            log.info("Currently subscribed to {} symbols:", response.getCount());
            for (String symbol : response.getSubscribedSymbolsList()) {
                log.info("  - {}", symbol);
            }
            
        } catch (Exception e) {
            log.error("Error getting current subscriptions", e);
        }
    }

    /**
     * Example: Stream real-time price updates (async call)
     */
    public void streamPriceUpdates() {
        try {
            log.info("=== Streaming Real-time Price Updates ===");
            
            StreamPricesRequest request = StreamPricesRequest.newBuilder()
                    .addSymbols("IBM.N")
                    .addSymbols("MSFT.O")
                    .build();
            
            CountDownLatch finishLatch = new CountDownLatch(1);
            
            StreamObserver<PriceUpdate> responseObserver = new StreamObserver<PriceUpdate>() {
                @Override
                public void onNext(PriceUpdate update) {
                    log.info("Price Update - Symbol: {}, Bid: {}, Ask: {}, Last: {}, Update Time: {}",
                            update.getSymbol(),
                            update.getSnapshot().getBid(),
                            update.getSnapshot().getAsk(),
                            update.getSnapshot().getLast(),
                            update.getUpdateTimestamp());
                }

                @Override
                public void onError(Throwable t) {
                    log.error("Stream error: {}", t.getMessage());
                    finishLatch.countDown();
                }

                @Override
                public void onCompleted() {
                    log.info("Stream completed");
                    finishLatch.countDown();
                }
            };
            
            asyncStub.streamPrices(request, responseObserver);
            
            // Wait for stream to complete (with timeout)
            if (!finishLatch.await(30, TimeUnit.SECONDS)) {
                log.warn("Stream timeout after 30 seconds");
            }
            
        } catch (Exception e) {
            log.error("Error streaming price updates", e);
        }
    }

    /**
     * Example: Unsubscribe from symbols (blocking call)
     */
    public void unsubscribeFromSymbols() {
        try {
            log.info("=== Unsubscribing from Symbols (Blocking) ===");
            
            UnsubscribeRequest request = UnsubscribeRequest.newBuilder()
                    .addSymbols("IBM.N")
                    .build();
            
            UnsubscribeResponse response = blockingStub.unsubscribe(request);
            
            if (response.getSuccess()) {
                log.info("Successfully unsubscribed from {} symbols", response.getUnsubscribedSymbolsCount());
                log.info("Unsubscribed symbols: {}", response.getUnsubscribedSymbolsList());
                log.info("Remaining subscriptions: {}", response.getRemainingSubscriptions());
                log.info("Message: {}", response.getMessage());
            } else {
                log.error("Unsubscription failed: {}", response.getMessage());
            }
            
        } catch (Exception e) {
            log.error("Error unsubscribing from symbols", e);
        }
    }

    /**
     * Example: Async subscription with callback
     */
    public void subscribeAsync() {
        try {
            log.info("=== Async Subscription with Callback ===");
            
            SubscribeRequest request = SubscribeRequest.newBuilder()
                    .addSymbols("AAPL.O")
                    .addSymbols("GOOGL.O")
                    .build();
            
            CountDownLatch finishLatch = new CountDownLatch(1);
            
            StreamObserver<SubscribeResponse> responseObserver = new StreamObserver<SubscribeResponse>() {
                @Override
                public void onNext(SubscribeResponse response) {
                    if (response.getSuccess()) {
                        log.info("Async subscription successful:");
                        log.info("  Subscribed symbols: {}", response.getSubscribedSymbolsList());
                        log.info("  Total subscriptions: {}", response.getTotalSubscriptions());
                        log.info("  Message: {}", response.getMessage());
                    } else {
                        log.error("Async subscription failed: {}", response.getMessage());
                    }
                }

                @Override
                public void onError(Throwable t) {
                    log.error("Async subscription error: {}", t.getMessage());
                    finishLatch.countDown();
                }

                @Override
                public void onCompleted() {
                    log.info("Async subscription completed");
                    finishLatch.countDown();
                }
            };
            
            asyncStub.subscribe(request, responseObserver);
            
            // Wait for async call to complete
            if (!finishLatch.await(10, TimeUnit.SECONDS)) {
                log.warn("Async subscription timeout after 10 seconds");
            }
            
        } catch (Exception e) {
            log.error("Error in async subscription", e);
        }
    }

    /**
     * Example: Continuous price polling with gRPC
     */
    public void continuousPricePolling() {
        try {
            log.info("=== Continuous Price Polling with gRPC ===");
            
            GetPricesRequest request = GetPricesRequest.newBuilder()
                    .addSymbols("IBM.N")
                    .addSymbols("MSFT.O")
                    .build();
            
            for (int i = 0; i < 10; i++) {
                try {
                    GetPricesResponse response = blockingStub.getPrices(request);
                    
                    log.info("Poll #{} - Received {} price snapshots:", i + 1, response.getPricesCount());
                    for (PriceSnapshot snapshot : response.getPricesList()) {
                        log.info("  {}: Bid={}, Ask={}, Last={}",
                                snapshot.getSymbol(),
                                snapshot.getBid(),
                                snapshot.getAsk(),
                                snapshot.getLast());
                    }
                    
                } catch (Exception e) {
                    log.error("Poll #{} failed: {}", i + 1, e.getMessage());
                }
                
                // Wait 2 seconds before next poll
                Thread.sleep(2000);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Continuous polling interrupted", e);
        } catch (Exception e) {
            log.error("Error in continuous price polling", e);
        }
    }

    /**
     * Example: Error handling and retry logic
     */
    public void getPricesWithRetry() {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                log.info("=== Getting Prices with Retry (Attempt {}) ===", retryCount + 1);
                
                GetPricesRequest request = GetPricesRequest.newBuilder()
                        .addSymbols("IBM.N")
                        .build();
                
                GetPricesResponse response = blockingStub
                        .withDeadlineAfter(5, TimeUnit.SECONDS)
                        .getPrices(request);
                
                log.info("Success! Received {} price snapshots", response.getPricesCount());
                return; // Success, exit retry loop
                
            } catch (Exception e) {
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

    /**
     * Shutdown the gRPC channel
     */
    public void shutdown() {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            log.info("gRPC channel shutdown successfully");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Error shutting down gRPC channel", e);
        }
    }
}
