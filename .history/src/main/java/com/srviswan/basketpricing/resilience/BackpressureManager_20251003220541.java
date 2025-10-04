package com.srviswan.basketpricing.resilience;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackpressureManager {

    private final BlockingQueue<PriceUpdateTask> updateQueue = new LinkedBlockingQueue<>(1000);
    private final Semaphore processingSemaphore = new Semaphore(10); // Max 10 concurrent updates
    private final AtomicInteger queueSize = new AtomicInteger(0);
    private final AtomicLong droppedUpdates = new AtomicLong(0);
    private final AtomicLong processedUpdates = new AtomicLong(0);

    public boolean offerUpdate(PriceUpdateTask task) {
        if (updateQueue.offer(task)) {
            queueSize.incrementAndGet();
            return true;
        } else {
            droppedUpdates.incrementAndGet();
            log.warn("Price update queue full, dropping update for symbol: {}", task.getSymbol());
            return false;
        }
    }

    public PriceUpdateTask pollUpdate(long timeout, TimeUnit unit) throws InterruptedException {
        PriceUpdateTask task = updateQueue.poll(timeout, unit);
        if (task != null) {
            queueSize.decrementAndGet();
        }
        return task;
    }

    public boolean acquireProcessingPermit() {
        return processingSemaphore.tryAcquire();
    }

    public void releaseProcessingPermit() {
        processingSemaphore.release();
        processedUpdates.incrementAndGet();
    }

    public int getQueueSize() {
        return queueSize.get();
    }

    public long getDroppedUpdates() {
        return droppedUpdates.get();
    }

    public long getProcessedUpdates() {
        return processedUpdates.get();
    }

    public double getQueueUtilization() {
        return (double) queueSize.get() / 1000.0; // Max queue size is 1000
    }

    public static class PriceUpdateTask {
        private final String symbol;
        private final Runnable updateAction;
        private final long timestamp;

        public PriceUpdateTask(String symbol, Runnable updateAction) {
            this.symbol = symbol;
            this.updateAction = updateAction;
            this.timestamp = System.currentTimeMillis();
        }

        public String getSymbol() {
            return symbol;
        }

        public Runnable getUpdateAction() {
            return updateAction;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public boolean isStale(long maxAgeMs) {
            return System.currentTimeMillis() - timestamp > maxAgeMs;
        }
    }
}
