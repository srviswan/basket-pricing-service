package com.srviswan.basketpricing.resilience;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService processingExecutor;

    @PostConstruct
    public void start() {
        log.info("🚀 Starting BackpressureManager processing threads...");
        running.set(true);
        
        // Create thread pool for processing price updates
        processingExecutor = Executors.newFixedThreadPool(5, r -> {
            Thread t = new Thread(r, "backpressure-processor");
            t.setDaemon(true);
            return t;
        });
        
        // Start consumer threads
        for (int i = 0; i < 5; i++) {
            final int threadNum = i;
            processingExecutor.submit(() -> {
                log.info("Backpressure processing thread {} started", threadNum);
                processQueue(threadNum);
            });
        }
        
        log.info("✅ BackpressureManager started with 5 processing threads");
    }
    
    @PreDestroy
    public void stop() {
        log.info("Stopping BackpressureManager...");
        running.set(false);
        
        if (processingExecutor != null) {
            processingExecutor.shutdown();
            try {
                if (!processingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    processingExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                processingExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        log.info("✅ BackpressureManager stopped");
    }
    
    private void processQueue(int threadNum) {
        while (running.get()) {
            try {
                // Poll for tasks with timeout
                PriceUpdateTask task = updateQueue.poll(500, TimeUnit.MILLISECONDS);
                
                if (task != null) {
                    queueSize.decrementAndGet();
                    
                    // Check if task is stale (older than 5 seconds)
                    if (task.isStale(5000)) {
                        log.debug("Thread {}: Skipping stale update for {}", threadNum, task.getSymbol());
                        continue;
                    }
                    
                    // Acquire processing permit
                    if (processingSemaphore.tryAcquire(100, TimeUnit.MILLISECONDS)) {
                        try {
                            log.debug("Thread {}: Processing update for {}", threadNum, task.getSymbol());
                            
                            // Execute the update action
                            task.getUpdateAction().run();
                            
                            processedUpdates.incrementAndGet();
                            log.debug("Thread {}: ✅ Successfully processed update for {}", threadNum, task.getSymbol());
                        } catch (Exception e) {
                            log.error("Thread {}: Error processing update for {}", threadNum, task.getSymbol(), e);
                        } finally {
                            processingSemaphore.release();
                        }
                    } else {
                        // Put back in queue if we couldn't acquire permit
                        updateQueue.offer(task);
                        queueSize.incrementAndGet();
                    }
                }
            } catch (InterruptedException e) {
                log.warn("Thread {}: Processing interrupted", threadNum);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Thread {}: Unexpected error in processing loop", threadNum, e);
            }
        }
        
        log.info("Backpressure processing thread {} stopped", threadNum);
    }

    public boolean offerUpdate(PriceUpdateTask task) {
        if (updateQueue.offer(task)) {
            queueSize.incrementAndGet();
            log.debug("✅ Task queued for {} (queue size: {})", task.getSymbol(), queueSize.get());
            return true;
        } else {
            droppedUpdates.incrementAndGet();
            log.warn("❌ Price update queue full, dropping update for symbol: {}", task.getSymbol());
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
