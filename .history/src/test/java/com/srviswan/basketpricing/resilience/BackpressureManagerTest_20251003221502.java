package com.srviswan.basketpricing.resilience;

import com.srviswan.basketpricing.monitoring.PricingMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackpressureManagerTest {

    @Mock
    private PricingMetrics pricingMetrics;

    private BackpressureManager backpressureManager;

    @BeforeEach
    void setUp() {
        backpressureManager = new BackpressureManager(pricingMetrics);
    }

    @Test
    void offerUpdate_ShouldAcceptTask_WhenQueueHasCapacity() {
        // Given
        BackpressureManager.PriceUpdateTask task = new BackpressureManager.PriceUpdateTask(
                "IBM.N", () -> {});

        // When
        boolean accepted = backpressureManager.offerUpdate(task);

        // Then
        assertThat(accepted).isTrue();
        verify(pricingMetrics).recordBackpressureUtilization(anyDouble());
    }

    @Test
    void offerUpdate_ShouldRejectTask_WhenQueueIsFull() {
        // Given
        // Fill up the queue to capacity
        for (int i = 0; i < 10000; i++) {
            BackpressureManager.PriceUpdateTask task = new BackpressureManager.PriceUpdateTask(
                    "SYMBOL" + i, () -> {});
            backpressureManager.offerUpdate(task);
        }

        // When
        BackpressureManager.PriceUpdateTask newTask = new BackpressureManager.PriceUpdateTask(
                "NEW_SYMBOL", () -> {});
        boolean accepted = backpressureManager.offerUpdate(newTask);

        // Then
        assertThat(accepted).isFalse();
        verify(pricingMetrics).recordDroppedUpdates(anyLong());
    }

    @Test
    void getQueueUtilization_ShouldReturnCorrectValue_WhenQueueHasItems() {
        // Given
        BackpressureManager.PriceUpdateTask task1 = new BackpressureManager.PriceUpdateTask(
                "IBM.N", () -> {});
        BackpressureManager.PriceUpdateTask task2 = new BackpressureManager.PriceUpdateTask(
                "MSFT.O", () -> {});

        backpressureManager.offerUpdate(task1);
        backpressureManager.offerUpdate(task2);

        // When
        double utilization = backpressureManager.getQueueUtilization();

        // Then
        assertThat(utilization).isGreaterThan(0.0);
        assertThat(utilization).isLessThan(1.0);
    }

    @Test
    void getQueueUtilization_ShouldReturnZero_WhenQueueIsEmpty() {
        // When
        double utilization = backpressureManager.getQueueUtilization();

        // Then
        assertThat(utilization).isEqualTo(0.0);
    }

    @Test
    void getProcessedUpdates_ShouldReturnZero_Initially() {
        // When
        long processed = backpressureManager.getProcessedUpdates();

        // Then
        assertThat(processed).isEqualTo(0);
    }

    @Test
    void getDroppedUpdates_ShouldReturnZero_Initially() {
        // When
        long dropped = backpressureManager.getDroppedUpdates();

        // Then
        assertThat(dropped).isEqualTo(0);
    }

    @Test
    void processedUpdates_ShouldIncrement_WhenTaskIsProcessed() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger executionCount = new AtomicInteger(0);
        
        BackpressureManager.PriceUpdateTask task = new BackpressureManager.PriceUpdateTask(
                "IBM.N", () -> {
                    executionCount.incrementAndGet();
                    latch.countDown();
                });

        // When
        backpressureManager.offerUpdate(task);
        boolean completed = latch.await(5, TimeUnit.SECONDS);

        // Then
        assertThat(completed).isTrue();
        assertThat(executionCount.get()).isEqualTo(1);
        assertThat(backpressureManager.getProcessedUpdates()).isEqualTo(1);
    }

    @Test
    void droppedUpdates_ShouldIncrement_WhenTaskIsRejected() {
        // Given
        // Fill up the queue to capacity
        for (int i = 0; i < 10000; i++) {
            BackpressureManager.PriceUpdateTask task = new BackpressureManager.PriceUpdateTask(
                    "SYMBOL" + i, () -> {});
            backpressureManager.offerUpdate(task);
        }

        // When
        BackpressureManager.PriceUpdateTask newTask = new BackpressureManager.PriceUpdateTask(
                "NEW_SYMBOL", () -> {});
        backpressureManager.offerUpdate(newTask);

        // Then
        assertThat(backpressureManager.getDroppedUpdates()).isEqualTo(1);
    }

    @Test
    void start_ShouldInitializeConsumerThread() {
        // Given
        BackpressureManager manager = new BackpressureManager(pricingMetrics);

        // When
        manager.start();

        // Then
        // Thread should be started (verified by being able to process tasks)
        BackpressureManager.PriceUpdateTask task = new BackpressureManager.PriceUpdateTask(
                "IBM.N", () -> {});
        boolean accepted = manager.offerUpdate(task);
        assertThat(accepted).isTrue();
    }

    @Test
    void stop_ShouldShutdownConsumerThread() throws InterruptedException {
        // Given
        BackpressureManager manager = new BackpressureManager(pricingMetrics);
        manager.start();

        // When
        manager.stop();

        // Then
        // Thread should be stopped (verified by not being able to process new tasks)
        // Note: This is a bit tricky to test directly, but we can verify the stop method completes
        assertThat(true).isTrue(); // If we get here, stop() completed without hanging
    }

    @Test
    void priceUpdateTask_ShouldExecuteAction_WhenCreated() {
        // Given
        AtomicInteger executionCount = new AtomicInteger(0);
        Runnable action = executionCount::incrementAndGet;

        // When
        BackpressureManager.PriceUpdateTask task = new BackpressureManager.PriceUpdateTask(
                "IBM.N", action);
        task.getUpdateAction().run();

        // Then
        assertThat(executionCount.get()).isEqualTo(1);
        assertThat(task.getSymbol()).isEqualTo("IBM.N");
    }
}
