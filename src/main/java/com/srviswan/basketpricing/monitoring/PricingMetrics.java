package com.srviswan.basketpricing.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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
    private final AtomicInteger connectionStatus = new AtomicInteger(0); // 0 = disconnected, 1 = connected
    private Counter connectionErrors;

    // Subscription metrics
    private final AtomicInteger activeSubscriptions = new AtomicInteger(0);
    private Counter subscriptionRequests;
    private Counter unsubscriptionRequests;

    // Price update metrics
    private Counter priceUpdates;
    private Timer priceUpdateLatency;

    // API metrics
    private Counter apiRequests;
    private Timer apiResponseTime;

    // Backpressure metrics
    private AtomicInteger backpressureQueueUtilization = new AtomicInteger(0);
    private AtomicLong droppedUpdates = new AtomicLong(0);

    public void initializeGauges() {
        // Register gauges
        meterRegistry.gauge("pricing.connection.status", connectionStatus, AtomicInteger::get);
        meterRegistry.gauge("pricing.subscriptions.active", activeSubscriptions, AtomicInteger::get);
        meterRegistry.gauge("pricing.backpressure.queue.utilization", backpressureQueueUtilization, AtomicInteger::get);
        meterRegistry.gauge("pricing.backpressure.dropped.updates", droppedUpdates, AtomicLong::get);

        // Initialize counters
        connectionErrors = meterRegistry.counter("pricing.connection.errors");
        subscriptionRequests = meterRegistry.counter("pricing.subscriptions.requests");
        unsubscriptionRequests = meterRegistry.counter("pricing.unsubscriptions.requests");
        priceUpdates = meterRegistry.counter("pricing.updates.total");

        // Initialize timers
        priceUpdateLatency = meterRegistry.timer("pricing.update.latency");
        apiResponseTime = meterRegistry.timer("pricing.api.response.time");

        // Initialize API requests counter with default tag
        apiRequests = meterRegistry.counter("pricing.api.requests.total", "endpoint", "unknown");
    }

    // Connection metrics
    public void recordConnectionError() {
        if (connectionErrors != null) {
            connectionErrors.increment();
        }
    }

    public void setConnectionStatus(boolean connected) {
        connectionStatus.set(connected ? 1 : 0);
    }

    // Subscription metrics
    public void incrementActiveSubscriptions() {
        activeSubscriptions.incrementAndGet();
    }

    public void decrementActiveSubscriptions() {
        activeSubscriptions.decrementAndGet();
    }

    public void recordSubscriptionRequest() {
        if (subscriptionRequests != null) {
            subscriptionRequests.increment();
        }
    }

    public void recordUnsubscriptionRequest() {
        if (unsubscriptionRequests != null) {
            unsubscriptionRequests.increment();
        }
    }

    // Price update metrics
    public void recordPriceUpdate() {
        if (priceUpdates != null) {
            priceUpdates.increment();
        }
    }

    public void recordPriceUpdateLatency(long milliseconds) {
        if (priceUpdateLatency != null) {
            priceUpdateLatency.record(milliseconds, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    // API metrics
    public void recordApiRequest(String endpoint) {
        if (apiRequests != null) {
            meterRegistry.counter("pricing.api.requests.total", "endpoint", endpoint).increment();
        }
    }

    public Timer.Sample startApiTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordApiResponseTime(Timer.Sample sample) {
        if (apiResponseTime != null && sample != null) {
            sample.stop(apiResponseTime);
        }
    }

    // Backpressure metrics
    public void recordBackpressureUtilization(double utilization) {
        backpressureQueueUtilization.set((int) (utilization * 100));
    }

    public void recordDroppedUpdates(long count) {
        droppedUpdates.set(count);
    }
}
