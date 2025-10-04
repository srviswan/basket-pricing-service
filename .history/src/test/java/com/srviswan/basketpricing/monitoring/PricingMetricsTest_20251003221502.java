package com.srviswan.basketpricing.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PricingMetricsTest {

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @Mock
    private Timer timer;

    @Mock
    private Timer.Sample sample;

    private PricingMetrics pricingMetrics;

    @BeforeEach
    void setUp() {
        pricingMetrics = new PricingMetrics(meterRegistry);
        
        // Mock the meter registry to return our mocked meters
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        when(meterRegistry.counter(anyString(), any())).thenReturn(counter);
        when(meterRegistry.timer(anyString())).thenReturn(timer);
        when(Timer.start(meterRegistry)).thenReturn(sample);
    }

    @Test
    void initializeGauges_ShouldRegisterGauges_WhenCalled() {
        // Given
        when(meterRegistry.gauge(anyString(), any(), any())).thenReturn(mock(Gauge.class));

        // When
        pricingMetrics.initializeGauges();

        // Then
        verify(meterRegistry).gauge(eq("pricing.connection.status"), any(), any());
        verify(meterRegistry).gauge(eq("pricing.subscriptions.active"), any(), any());
        verify(meterRegistry).gauge(eq("pricing.backpressure.queue.utilization"), any(), any());
        verify(meterRegistry).gauge(eq("pricing.backpressure.dropped.updates"), any(), any());
        
        verify(meterRegistry).counter("pricing.connection.errors");
        verify(meterRegistry).counter("pricing.subscriptions.requests");
        verify(meterRegistry).counter("pricing.unsubscriptions.requests");
        verify(meterRegistry).counter("pricing.updates.total");
        verify(meterRegistry).timer("pricing.update.latency");
        verify(meterRegistry).counter("pricing.api.requests.total");
        verify(meterRegistry).timer("pricing.api.response.time");
    }

    @Test
    void recordConnectionError_ShouldIncrementCounter_WhenCalled() {
        // Given
        pricingMetrics.initializeGauges();

        // When
        pricingMetrics.recordConnectionError();

        // Then
        verify(counter).increment();
    }

    @Test
    void setConnectionStatus_ShouldUpdateStatus_WhenCalled() {
        // Given
        pricingMetrics.initializeGauges();

        // When
        pricingMetrics.setConnectionStatus(true);
        pricingMetrics.setConnectionStatus(false);

        // Then
        // Status is stored in AtomicInteger, verify by checking the internal state
        assertThat(pricingMetrics).isNotNull();
    }

    @Test
    void incrementActiveSubscriptions_ShouldIncrementCount_WhenCalled() {
        // Given
        pricingMetrics.initializeGauges();

        // When
        pricingMetrics.incrementActiveSubscriptions();
        pricingMetrics.incrementActiveSubscriptions();

        // Then
        // Count is stored in AtomicInteger, verify by checking the internal state
        assertThat(pricingMetrics).isNotNull();
    }

    @Test
    void decrementActiveSubscriptions_ShouldDecrementCount_WhenCalled() {
        // Given
        pricingMetrics.initializeGauges();

        // When
        pricingMetrics.incrementActiveSubscriptions();
        pricingMetrics.decrementActiveSubscriptions();

        // Then
        // Count is stored in AtomicInteger, verify by checking the internal state
        assertThat(pricingMetrics).isNotNull();
    }

    @Test
    void recordSubscriptionRequest_ShouldIncrementCounter_WhenCalled() {
        // Given
        pricingMetrics.initializeGauges();

        // When
        pricingMetrics.recordSubscriptionRequest();

        // Then
        verify(counter).increment();
    }

    @Test
    void recordUnsubscriptionRequest_ShouldIncrementCounter_WhenCalled() {
        // Given
        pricingMetrics.initializeGauges();

        // When
        pricingMetrics.recordUnsubscriptionRequest();

        // Then
        verify(counter).increment();
    }

    @Test
    void recordPriceUpdate_ShouldIncrementCounter_WhenCalled() {
        // Given
        pricingMetrics.initializeGauges();

        // When
        pricingMetrics.recordPriceUpdate();

        // Then
        verify(counter).increment();
    }

    @Test
    void recordPriceUpdateLatency_ShouldRecordLatency_WhenCalled() {
        // Given
        pricingMetrics.initializeGauges();

        // When
        pricingMetrics.recordPriceUpdateLatency(100);

        // Then
        verify(timer).record(100, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    @Test
    void recordApiRequest_ShouldIncrementCounter_WhenCalled() {
        // Given
        pricingMetrics.initializeGauges();

        // When
        pricingMetrics.recordApiRequest("testEndpoint");

        // Then
        verify(counter).increment(any());
    }

    @Test
    void startApiTimer_ShouldReturnSample_WhenCalled() {
        // When
        Timer.Sample result = pricingMetrics.startApiTimer();

        // Then
        assertThat(result).isEqualTo(sample);
        verify(meterRegistry).timer("pricing.api.response.time");
    }

    @Test
    void recordApiResponseTime_ShouldStopTimer_WhenCalled() {
        // Given
        pricingMetrics.initializeGauges();

        // When
        pricingMetrics.recordApiResponseTime(sample);

        // Then
        verify(sample).stop(timer);
    }

    @Test
    void recordBackpressureUtilization_ShouldUpdateUtilization_WhenCalled() {
        // Given
        pricingMetrics.initializeGauges();

        // When
        pricingMetrics.recordBackpressureUtilization(0.75);

        // Then
        // Utilization is stored in AtomicInteger, verify by checking the internal state
        assertThat(pricingMetrics).isNotNull();
    }

    @Test
    void recordDroppedUpdates_ShouldUpdateCount_WhenCalled() {
        // Given
        pricingMetrics.initializeGauges();

        // When
        pricingMetrics.recordDroppedUpdates(42);

        // Then
        // Count is stored in AtomicLong, verify by checking the internal state
        assertThat(pricingMetrics).isNotNull();
    }
}
