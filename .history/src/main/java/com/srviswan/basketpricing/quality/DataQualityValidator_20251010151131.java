package com.srviswan.basketpricing.quality;

import com.srviswan.basketpricing.model.PriceSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Real-time data quality validator for pricing data.
 * Validates every price update as it flows through the system.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataQualityValidator {

    private final DataQualityMetrics metrics;
    private final DataQualityConfig config;
    
    /**
     * Validate a single price snapshot in real-time
     * @param ric The instrument identifier
     * @param price The price snapshot to validate
     * @return Validation result with any issues found
     */
    public ValidationResult validate(String ric, PriceSnapshot price) {
        ValidationResult result = new ValidationResult(ric);
        
        try {
            // Run all validation checks
            validateCompleteness(ric, price, result);
            validateValidity(ric, price, result);
            validateConsistency(ric, price, result);
            validateTimeliness(ric, price, result);
            validateAccuracy(ric, price, result);
            
            // Record metrics
            if (result.isValid()) {
                metrics.recordValidPrice(ric);
            } else {
                metrics.recordInvalidPrice(ric, result.getIssues());
            }
            
            // Log issues if any
            if (!result.isValid()) {
                log.warn("Data quality issues for {}: {}", ric, result.getSummary());
            }
            
        } catch (Exception e) {
            log.error("Error validating price for {}", ric, e);
            result.addError(ValidationDimension.SYSTEM, "Validation error: " + e.getMessage());
            metrics.recordValidationError(ric);
        }
        
        return result;
    }
    
    /**
     * Validate multiple prices (batch validation)
     */
    public Map<String, ValidationResult> validateBatch(Map<String, PriceSnapshot> prices) {
        Map<String, ValidationResult> results = new HashMap<>();
        
        for (Map.Entry<String, PriceSnapshot> entry : prices.entrySet()) {
            results.put(entry.getKey(), validate(entry.getKey(), entry.getValue()));
        }
        
        return results;
    }
    
    /**
     * Check 1: Completeness - All required fields present
     */
    private void validateCompleteness(String ric, PriceSnapshot price, ValidationResult result) {
        if (price == null) {
            result.addError(ValidationDimension.COMPLETENESS, "Price snapshot is null");
            return;
        }
        
        List<String> missingFields = new ArrayList<>();
        
        if (price.getBid() == null) missingFields.add("bid");
        if (price.getAsk() == null) missingFields.add("ask");
        if (price.getLast() == null) missingFields.add("last");
        if (price.getTimestamp() == null) missingFields.add("timestamp");
        
        if (!missingFields.isEmpty()) {
            result.addError(ValidationDimension.COMPLETENESS, 
                "Missing required fields: " + String.join(", ", missingFields));
            metrics.recordMissingFields(ric, missingFields.size());
        }
    }
    
    /**
     * Check 2: Validity - Prices are in valid range
     */
    private void validateValidity(String ric, PriceSnapshot price, ValidationResult result) {
        if (price == null) return;
        
        // Check bid validity
        if (price.getBid() != null) {
            if (price.getBid() <= 0) {
                result.addError(ValidationDimension.VALIDITY, 
                    "Bid price must be positive: " + price.getBid());
                metrics.recordInvalidPrice(ric, "negative_bid");
            } else if (price.getBid() < config.getMinPrice() || price.getBid() > config.getMaxPrice()) {
                result.addWarning(ValidationDimension.VALIDITY, 
                    String.format("Bid price outside expected range [%.2f, %.2f]: %.4f", 
                        config.getMinPrice(), config.getMaxPrice(), price.getBid()));
                metrics.recordOutOfRangePrice(ric, "bid");
            }
        }
        
        // Check ask validity
        if (price.getAsk() != null) {
            if (price.getAsk() <= 0) {
                result.addError(ValidationDimension.VALIDITY, 
                    "Ask price must be positive: " + price.getAsk());
                metrics.recordInvalidPrice(ric, "negative_ask");
            } else if (price.getAsk() < config.getMinPrice() || price.getAsk() > config.getMaxPrice()) {
                result.addWarning(ValidationDimension.VALIDITY, 
                    String.format("Ask price outside expected range [%.2f, %.2f]: %.4f", 
                        config.getMinPrice(), config.getMaxPrice(), price.getAsk()));
                metrics.recordOutOfRangePrice(ric, "ask");
            }
        }
        
        // Check volume validity
        if (price.getVolume() != null && price.getVolume() < 0) {
            result.addError(ValidationDimension.VALIDITY, 
                "Volume cannot be negative: " + price.getVolume());
            metrics.recordInvalidPrice(ric, "negative_volume");
        }
    }
    
    /**
     * Check 3: Consistency - Bid <= Last <= Ask
     */
    private void validateConsistency(String ric, PriceSnapshot price, ValidationResult result) {
        if (price == null) return;
        
        Double bid = price.getBid();
        Double ask = price.getAsk();
        Double last = price.getLast();
        
        // Check bid <= ask
        if (bid != null && ask != null) {
            if (bid > ask) {
                result.addError(ValidationDimension.CONSISTENCY, 
                    String.format("Bid (%.4f) cannot be greater than Ask (%.4f)", bid, ask));
                metrics.recordInconsistentPrice(ric, "inverted_bid_ask");
                return;
            }
            
            // Check spread is reasonable
            double spread = ((ask - bid) / bid) * 100;
            if (spread > config.getMaxSpreadPercentage()) {
                result.addWarning(ValidationDimension.CONSISTENCY, 
                    String.format("Spread too wide: %.2f%% (max: %.2f%%)", 
                        spread, config.getMaxSpreadPercentage()));
                metrics.recordWideSpread(ric, spread);
            }
        }
        
        // Check last is between bid and ask
        if (bid != null && ask != null && last != null) {
            if (last < bid || last > ask) {
                result.addWarning(ValidationDimension.CONSISTENCY, 
                    String.format("Last price (%.4f) outside bid-ask spread [%.4f, %.4f]", 
                        last, bid, ask));
                metrics.recordInconsistentPrice(ric, "last_outside_spread");
            }
        }
    }
    
    /**
     * Check 4: Timeliness - Data is fresh
     */
    private void validateTimeliness(String ric, PriceSnapshot price, ValidationResult result) {
        if (price == null || price.getTimestamp() == null) return;
        
        Instant now = Instant.now();
        Instant priceTime = price.getTimestamp();
        
        // Check for future timestamps
        if (priceTime.isAfter(now)) {
            result.addWarning(ValidationDimension.TIMELINESS, 
                "Price timestamp is in the future");
            metrics.recordFutureTimestamp(ric);
            return;
        }
        
        Duration age = Duration.between(priceTime, now);
        
        if (age.compareTo(config.getMaxAge()) > 0) {
            result.addWarning(ValidationDimension.TIMELINESS, 
                String.format("Stale data: %d seconds old (max: %d)", 
                    age.getSeconds(), config.getMaxAge().getSeconds()));
            metrics.recordStalePrice(ric, age.getSeconds());
        }
    }
    
    /**
     * Check 5: Accuracy - Prices have correct precision
     */
    private void validateAccuracy(String ric, PriceSnapshot price, ValidationResult result) {
        if (price == null) return;
        
        int maxDecimals = config.getMaxDecimalPlaces();
        
        // Check bid precision
        if (price.getBid() != null && !hasValidPrecision(price.getBid(), maxDecimals)) {
            result.addWarning(ValidationDimension.ACCURACY, 
                String.format("Bid price has too many decimal places (max: %d): %.10f", 
                    maxDecimals, price.getBid()));
        }
        
        // Check ask precision
        if (price.getAsk() != null && !hasValidPrecision(price.getAsk(), maxDecimals)) {
            result.addWarning(ValidationDimension.ACCURACY, 
                String.format("Ask price has too many decimal places (max: %d): %.10f", 
                    maxDecimals, price.getAsk()));
        }
    }
    
    /**
     * Helper: Check decimal precision
     */
    private boolean hasValidPrecision(double value, int maxDecimals) {
        String str = String.format("%.10f", value);
        int decimalIndex = str.indexOf('.');
        if (decimalIndex == -1) return true;
        
        // Count non-zero decimals
        String decimals = str.substring(decimalIndex + 1);
        int significantDecimals = decimals.replaceAll("0+$", "").length();
        
        return significantDecimals <= maxDecimals;
    }
}

