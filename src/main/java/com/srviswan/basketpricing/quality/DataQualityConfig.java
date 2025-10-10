package com.srviswan.basketpricing.quality;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for data quality validation thresholds
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "data-quality")
public class DataQualityConfig {
    
    /**
     * Minimum acceptable price value
     */
    private double minPrice = 0.01;
    
    /**
     * Maximum acceptable price value
     */
    private double maxPrice = 1_000_000.0;
    
    /**
     * Maximum acceptable bid-ask spread percentage
     */
    private double maxSpreadPercentage = 10.0;
    
    /**
     * Maximum age of price data before considered stale
     */
    private Duration maxAge = Duration.ofSeconds(60);
    
    /**
     * Maximum number of decimal places for prices
     */
    private int maxDecimalPlaces = 6;
    
    /**
     * Minimum quality score threshold (0-100)
     */
    private double minQualityScore = 95.0;
    
    /**
     * Enable/disable real-time validation
     */
    private boolean enabled = true;
    
    /**
     * Enable/disable automatic alerting on quality issues
     */
    private boolean alertingEnabled = true;
    
    /**
     * Number of consecutive errors before triggering circuit breaker
     */
    private int circuitBreakerThreshold = 10;
}

