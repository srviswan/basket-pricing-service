package com.srviswan.basketpricing.quality;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics for data quality monitoring
 */
@Slf4j
@Component
public class DataQualityMetrics {
    
    private final MeterRegistry registry;
    
    // Counters
    private final Counter validPrices;
    private final Counter invalidPrices;
    private final Counter stalePrices;
    private final Counter inconsistentPrices;
    private final Counter missingFieldsTotal;
    private final Counter outOfRangePrices;
    private final Counter validationErrors;
    
    // Gauges
    private final AtomicLong currentQualityScore = new AtomicLong(100);
    private final AtomicLong totalValidations = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    
    // Per-symbol tracking
    private final Map<String, AtomicLong> errorsBySymbol = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> validationsBySymbol = new ConcurrentHashMap<>();
    
    // Timer for validation performance
    private final Timer validationTimer;
    
    public DataQualityMetrics(MeterRegistry registry) {
        this.registry = registry;
        
        // Initialize counters
        this.validPrices = Counter.builder("pricing.data.quality.valid")
            .description("Number of valid price updates")
            .register(registry);
            
        this.invalidPrices = Counter.builder("pricing.data.quality.invalid")
            .description("Number of invalid price updates")
            .register(registry);
            
        this.stalePrices = Counter.builder("pricing.data.quality.stale")
            .description("Number of stale price updates")
            .register(registry);
            
        this.inconsistentPrices = Counter.builder("pricing.data.quality.inconsistent")
            .description("Number of inconsistent price updates")
            .register(registry);
            
        this.missingFieldsTotal = Counter.builder("pricing.data.quality.missing_fields")
            .description("Total number of missing fields")
            .register(registry);
            
        this.outOfRangePrices = Counter.builder("pricing.data.quality.out_of_range")
            .description("Number of out-of-range prices")
            .register(registry);
            
        this.validationErrors = Counter.builder("pricing.data.quality.validation_errors")
            .description("Number of validation system errors")
            .register(registry);
        
        // Initialize gauges
        Gauge.builder("pricing.data.quality.score", currentQualityScore, AtomicLong::get)
            .description("Overall data quality score (0-100)")
            .register(registry);
            
        Gauge.builder("pricing.data.quality.error_rate", this, DataQualityMetrics::getErrorRate)
            .description("Current error rate (errors per second)")
            .register(registry);
        
        // Initialize timer
        this.validationTimer = Timer.builder("pricing.data.quality.validation_duration")
            .description("Time taken to validate price data")
            .register(registry);
    }
    
    /**
     * Record a valid price
     */
    public void recordValidPrice(String ric) {
        validPrices.increment();
        totalValidations.incrementAndGet();
        validationsBySymbol.computeIfAbsent(ric, k -> new AtomicLong(0)).incrementAndGet();
        updateQualityScore();
    }
    
    /**
     * Record an invalid price with issues
     */
    public void recordInvalidPrice(String ric, List<ValidationResult.Issue> issues) {
        invalidPrices.increment();
        totalValidations.incrementAndGet();
        totalErrors.incrementAndGet();
        errorsBySymbol.computeIfAbsent(ric, k -> new AtomicLong(0)).incrementAndGet();
        
        // Count specific issue types
        for (ValidationResult.Issue issue : issues) {
            if (issue.getLevel() == ValidationResult.IssueLevel.ERROR) {
                recordIssueType(ric, issue.getDimension());
            }
        }
        
        updateQualityScore();
    }
    
    /**
     * Record invalid price with simple reason
     */
    public void recordInvalidPrice(String ric, String reason) {
        invalidPrices.increment();
        totalValidations.incrementAndGet();
        totalErrors.incrementAndGet();
        errorsBySymbol.computeIfAbsent(ric, k -> new AtomicLong(0)).incrementAndGet();
        updateQualityScore();
    }
    
    /**
     * Record missing fields
     */
    public void recordMissingFields(String ric, int count) {
        for (int i = 0; i < count; i++) {
            missingFieldsTotal.increment();
        }
    }
    
    /**
     * Record stale price
     */
    public void recordStalePrice(String ric, long ageSeconds) {
        stalePrices.increment();
        registry.counter("pricing.data.quality.stale", "ric", ric).increment();
    }
    
    /**
     * Record inconsistent price
     */
    public void recordInconsistentPrice(String ric, String type) {
        inconsistentPrices.increment();
        registry.counter("pricing.data.quality.inconsistent", "ric", ric, "type", type).increment();
    }
    
    /**
     * Record out-of-range price
     */
    public void recordOutOfRangePrice(String ric, String field) {
        outOfRangePrices.increment();
        registry.counter("pricing.data.quality.out_of_range", "ric", ric, "field", field).increment();
    }
    
    /**
     * Record wide spread
     */
    public void recordWideSpread(String ric, double spreadPercentage) {
        registry.counter("pricing.data.quality.wide_spread", "ric", ric).increment();
        registry.gauge("pricing.data.quality.spread_percentage", spreadPercentage);
    }
    
    /**
     * Record future timestamp
     */
    public void recordFutureTimestamp(String ric) {
        registry.counter("pricing.data.quality.future_timestamp", "ric", ric).increment();
    }
    
    /**
     * Record validation system error
     */
    public void recordValidationError(String ric) {
        validationErrors.increment();
        registry.counter("pricing.data.quality.validation_errors", "ric", ric).increment();
    }
    
    /**
     * Record issue by type
     */
    private void recordIssueType(String ric, ValidationDimension dimension) {
        registry.counter("pricing.data.quality.issues", 
            "ric", ric, 
            "dimension", dimension.name().toLowerCase()).increment();
    }
    
    /**
     * Time a validation operation
     */
    public <T> T timeValidation(java.util.function.Supplier<T> operation) {
        return validationTimer.record(operation);
    }
    
    /**
     * Update overall quality score
     */
    private void updateQualityScore() {
        long total = totalValidations.get();
        long errors = totalErrors.get();
        
        if (total > 0) {
            long valid = total - errors;
            long score = (valid * 100) / total;
            currentQualityScore.set(score);
        }
    }
    
    /**
     * Get current error rate
     */
    private double getErrorRate() {
        // This is a simplified calculation
        // In production, you'd want a time-windowed rate
        return totalErrors.get() / Math.max(1.0, totalValidations.get());
    }
    
    /**
     * Get quality score for a specific symbol
     */
    public double getSymbolQualityScore(String ric) {
        long validations = validationsBySymbol.getOrDefault(ric, new AtomicLong(0)).get();
        long errors = errorsBySymbol.getOrDefault(ric, new AtomicLong(0)).get();
        
        if (validations == 0) return 100.0;
        
        long valid = validations - errors;
        return (valid * 100.0) / validations;
    }
    
    /**
     * Get overall quality score
     */
    public long getQualityScore() {
        return currentQualityScore.get();
    }
    
    /**
     * Reset metrics (for testing)
     */
    public void reset() {
        totalValidations.set(0);
        totalErrors.set(0);
        currentQualityScore.set(100);
        errorsBySymbol.clear();
        validationsBySymbol.clear();
    }
}

