package com.srviswan.basketpricing.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class PricingMetrics {

    private final MeterRegistry meterRegistry;
    
    // Connection metrics
    private final AtomicInteger activeSubscriptions = new AtomicInteger(0);
    private final AtomicLong totalPriceUpdates = new AtomicLong(0);
    private final AtomicInteger connectionStatus = new AtomicInteger(0); // 0=disconnected, 1=connected
    
    // Counters
    private final Counter subscriptionRequests = Counter.builder("pricing.subscription.requests")
            .description("Total subscription requests")
            .register(meterRegistry);
            
    private final Counter unsubscriptionRequests = Counter.builder("pricing.unsubscription.requests")
            .description("Total unsubscription requests")
            .register(meterRegistry);
            
    private final Counter priceUpdateCounter = Counter.builder("pricing.updates.total")
            .description("Total price updates received")
            .register(meterRegistry);
            
    private final Counter connectionErrors = Counter.builder("pricing.connection.errors")
            .description("Connection errors to market data provider")
            .register(meterRegistry);
            
    private final Counter apiRequests = Counter.builder("pricing.api.requests")
            .description("Total API requests")
            .tag("endpoint", "unknown")
            .register(meterRegistry);
    
    // Timers
    private final Timer priceUpdateLatency = Timer.builder("pricing.update.latency")
            .description("Time from market data update to API availability")
            .register(meterRegistry);
            
    private final Timer apiResponseTime = Timer.builder("pricing.api.response.time")
            .description("API response time")
            .register(meterRegistry);

    public void initializeGauges() {
        // Active subscriptions gauge
        Gauge.builder("pricing.subscriptions.active")
                .description("Number of active symbol subscriptions")
                .register(meterRegistry, activeSubscriptions, AtomicInteger::get);
                
        // Total price updates gauge
        Gauge.builder("pricing.updates.total.count")
                .description("Total number of price updates received")
                .register(meterRegistry, totalPriceUpdates, AtomicLong::get);
                
        // Connection status gauge
        Gauge.builder("pricing.connection.status")
                .description("Market data provider connection status (0=disconnected, 1=connected)")
                .register(meterRegistry, connectionStatus, AtomicInteger::get);
    }

    // Subscription metrics
    public void recordSubscriptionRequest() {
        subscriptionRequests.increment();
    }
    
    public void recordUnsubscriptionRequest() {
        unsubscriptionRequests.increment();
    }
    
    public void incrementActiveSubscriptions() {
        activeSubscriptions.incrementAndGet();
    }
    
    public void decrementActiveSubscriptions() {
        activeSubscriptions.decrementAndGet();
    }
    
    public void setActiveSubscriptions(int count) {
        activeSubscriptions.set(count);
    }

    // Price update metrics
    public void recordPriceUpdate() {
        totalPriceUpdates.incrementAndGet();
        priceUpdateCounter.increment();
    }
    
    public void recordPriceUpdateLatency(long latencyMs) {
        priceUpdateLatency.record(latencyMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    // Connection metrics
    public void recordConnectionError() {
        connectionErrors.increment();
    }
    
    public void setConnectionStatus(boolean connected) {
        connectionStatus.set(connected ? 1 : 0);
    }

    // API metrics
    public void recordApiRequest(String endpoint) {
        apiRequests.increment(Tags.of("endpoint", endpoint));
    }
    
    public Timer.Sample startApiTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordApiResponseTime(Timer.Sample sample) {
        sample.stop(apiResponseTime);
    }
}
