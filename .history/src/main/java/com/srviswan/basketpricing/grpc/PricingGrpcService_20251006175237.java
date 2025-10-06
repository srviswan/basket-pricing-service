package com.srviswan.basketpricing.grpc;

import com.srviswan.basketpricing.marketdata.MarketDataProvider;
import com.srviswan.basketpricing.marketdata.PriceSnapshot;
import com.srviswan.basketpricing.monitoring.PricingMetrics;
import com.srviswan.basketpricing.events.PriceUpdateEvent;
import org.springframework.context.event.EventListener;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.cache.annotation.Cacheable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class PricingGrpcService extends PricingServiceGrpc.PricingServiceImplBase {

    private final MarketDataProvider marketDataProvider;
    private final PricingMetrics pricingMetrics;
    
    // Track active streams for cleanup
    private final Map<String, Set<StreamObserver<PriceUpdate>>> activeStreams = new ConcurrentHashMap<>();
    private final ScheduledExecutorService streamExecutor = Executors.newScheduledThreadPool(10);

    @Override
    public void getPrices(GetPricesRequest request, StreamObserver<GetPricesResponse> responseObserver) {
        try {
            pricingMetrics.recordApiRequest("grpc.getPrices");
            
            List<String> symbols = request.getSymbolsList();
            Map<String, PriceSnapshot> prices = marketDataProvider.getLatestPrices(symbols);
            
            GetPricesResponse.Builder responseBuilder = GetPricesResponse.newBuilder();
            for (Map.Entry<String, PriceSnapshot> entry : prices.entrySet()) {
                PriceSnapshot snapshot = entry.getValue();
                com.srviswan.basketpricing.grpc.PriceSnapshot grpcSnapshot = 
                    com.srviswan.basketpricing.grpc.PriceSnapshot.newBuilder()
                        .setSymbol(snapshot.getSymbol())
                        .setBid(snapshot.getBid() != null ? snapshot.getBid() : 0.0)
                        .setAsk(snapshot.getAsk() != null ? snapshot.getAsk() : 0.0)
                        .setLast(snapshot.getLast() != null ? snapshot.getLast() : 0.0)
                        .setTimestamp(snapshot.getTimestamp().toEpochMilli())
                        .build();
                responseBuilder.addPrices(grpcSnapshot);
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in getPrices gRPC call", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void subscribe(SubscribeRequest request, StreamObserver<SubscribeResponse> responseObserver) {
        try {
            pricingMetrics.recordApiRequest("grpc.subscribe");
            
            List<String> symbols = request.getSymbolsList();
            marketDataProvider.subscribe(symbols);
            pricingMetrics.recordSubscriptionRequest();
            
            Set<String> subscribedSymbols = marketDataProvider.getSubscribedSymbols();
            
            SubscribeResponse response = SubscribeResponse.newBuilder()
                .addAllSubscribedSymbols(symbols)
                .setTotalSubscriptions(subscribedSymbols.size())
                .setSuccess(true)
                .setMessage("Successfully subscribed to " + symbols.size() + " symbols")
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in subscribe gRPC call", e);
            SubscribeResponse response = SubscribeResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Failed to subscribe: " + e.getMessage())
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void unsubscribe(UnsubscribeRequest request, StreamObserver<UnsubscribeResponse> responseObserver) {
        try {
            pricingMetrics.recordApiRequest("grpc.unsubscribe");
            
            List<String> symbols = request.getSymbolsList();
            marketDataProvider.unsubscribe(symbols);
            pricingMetrics.recordUnsubscriptionRequest();
            
            Set<String> remainingSymbols = marketDataProvider.getSubscribedSymbols();
            
            UnsubscribeResponse response = UnsubscribeResponse.newBuilder()
                .addAllUnsubscribedSymbols(symbols)
                .setRemainingSubscriptions(remainingSymbols.size())
                .setSuccess(true)
                .setMessage("Successfully unsubscribed from " + symbols.size() + " symbols")
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in unsubscribe gRPC call", e);
            UnsubscribeResponse response = UnsubscribeResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Failed to unsubscribe: " + e.getMessage())
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getSubscriptions(GetSubscriptionsRequest request, StreamObserver<GetSubscriptionsResponse> responseObserver) {
        try {
            pricingMetrics.recordApiRequest("grpc.getSubscriptions");
            
            Set<String> subscribedSymbols = marketDataProvider.getSubscribedSymbols();
            
            GetSubscriptionsResponse response = GetSubscriptionsResponse.newBuilder()
                .addAllSubscribedSymbols(subscribedSymbols)
                .setCount(subscribedSymbols.size())
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in getSubscriptions gRPC call", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void streamPrices(StreamPricesRequest request, StreamObserver<PriceUpdate> responseObserver) {
        try {
            pricingMetrics.recordApiRequest("grpc.streamPrices");
            
            List<String> symbols = request.getSymbolsList();
            
            // Subscribe to symbols if not already subscribed
            marketDataProvider.subscribe(symbols);
            
            // Add this stream to active streams for each symbol
            for (String symbol : symbols) {
                activeStreams.computeIfAbsent(symbol, k -> new CopyOnWriteArraySet<>()).add(responseObserver);
            }
            
            // Note: StreamObserver doesn't have setOnCancelHandler in this version
            // Cleanup will be handled in the stream processing loop
            
            // Start streaming price updates
            startPriceStreaming(symbols, responseObserver);
            
        } catch (Exception e) {
            log.error("Error in streamPrices gRPC call", e);
            responseObserver.onError(e);
        }
    }

    private void startPriceStreaming(List<String> symbols, StreamObserver<PriceUpdate> responseObserver) {
        streamExecutor.scheduleWithFixedDelay(() -> {
            try {
                Map<String, PriceSnapshot> prices = marketDataProvider.getLatestPrices(symbols);
                
                for (Map.Entry<String, PriceSnapshot> entry : prices.entrySet()) {
                    PriceSnapshot snapshot = entry.getValue();
                    
                    PriceUpdate update = PriceUpdate.newBuilder()
                        .setSymbol(snapshot.getSymbol())
                        .setSnapshot(com.srviswan.basketpricing.grpc.PriceSnapshot.newBuilder()
                            .setSymbol(snapshot.getSymbol())
                            .setBid(snapshot.getBid() != null ? snapshot.getBid() : 0.0)
                            .setAsk(snapshot.getAsk() != null ? snapshot.getAsk() : 0.0)
                            .setLast(snapshot.getLast() != null ? snapshot.getLast() : 0.0)
                            .setTimestamp(snapshot.getTimestamp().toEpochMilli())
                            .build())
                        .setUpdateTimestamp(Instant.now().toEpochMilli())
                        .build();
                    
                    responseObserver.onNext(update);
                }
                
            } catch (Exception e) {
                log.error("Error streaming prices", e);
                responseObserver.onError(e);
            }
        }, 0, 100, TimeUnit.MILLISECONDS); // Stream every 100ms
    }

    // Event listener for price updates
    @EventListener
    public void handlePriceUpdate(PriceUpdateEvent event) {
        String symbol = event.getSymbol();
        PriceSnapshot snapshot = event.getSnapshot();
        
        // Broadcast to all active streams for this symbol
        Set<StreamObserver<PriceUpdate>> streams = activeStreams.get(symbol);
        if (streams != null && !streams.isEmpty()) {
            PriceUpdate update = PriceUpdate.newBuilder()
                    .setSymbol(symbol)
                    .setSnapshot(com.srviswan.basketpricing.grpc.PriceSnapshot.newBuilder()
                            .setSymbol(snapshot.getSymbol())
                            .setBid(snapshot.getBid() != null ? snapshot.getBid() : 0.0)
                            .setAsk(snapshot.getAsk() != null ? snapshot.getAsk() : 0.0)
                            .setLast(snapshot.getLast() != null ? snapshot.getLast() : 0.0)
                            .setTimestamp(snapshot.getTimestamp().toEpochMilli())
                            .build())
                    .setUpdateTimestamp(System.currentTimeMillis())
                    .build();
            
            streams.forEach(stream -> {
                try {
                    stream.onNext(update);
                } catch (Exception e) {
                    log.warn("Failed to send update to stream for symbol {}", symbol, e);
                    streams.remove(stream);
                }
            });
        }
    }
    
    // Method to broadcast price updates to all active streams (kept for backward compatibility)
    public void broadcastPriceUpdate(String symbol, PriceSnapshot snapshot) {
        Set<StreamObserver<PriceUpdate>> streams = activeStreams.get(symbol);
        if (streams != null && !streams.isEmpty()) {
            PriceUpdate update = PriceUpdate.newBuilder()
                .setSymbol(symbol)
                .setSnapshot(com.srviswan.basketpricing.grpc.PriceSnapshot.newBuilder()
                    .setSymbol(snapshot.getSymbol())
                    .setBid(snapshot.getBid() != null ? snapshot.getBid() : 0.0)
                    .setAsk(snapshot.getAsk() != null ? snapshot.getAsk() : 0.0)
                    .setLast(snapshot.getLast() != null ? snapshot.getLast() : 0.0)
                    .setTimestamp(snapshot.getTimestamp().toEpochMilli())
                    .build())
                .setUpdateTimestamp(Instant.now().toEpochMilli())
                .build();
            
            streams.removeIf(stream -> {
                try {
                    stream.onNext(update);
                    return false;
                } catch (Exception e) {
                    log.debug("Failed to send update to stream, removing it", e);
                    return true;
                }
            });
        }
    }
}
