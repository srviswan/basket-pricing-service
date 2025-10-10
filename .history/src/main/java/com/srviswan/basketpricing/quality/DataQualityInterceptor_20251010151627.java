package com.srviswan.basketpricing.quality;

import com.srviswan.basketpricing.events.PriceUpdateEvent;
import com.srviswan.basketpricing.marketdata.PriceSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Intercepts price updates in real-time and validates data quality.
 * Runs validation on every price update as it flows through the system.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataQualityInterceptor {
    
    private final DataQualityValidator validator;
    private final DataQualityConfig config;
    private final DataQualityAlertService alertService;
    
    /**
     * Intercept price updates and validate in real-time
     * Order = 0 ensures this runs before other listeners
     */
    @EventListener
    @Order(0)
    public void onPriceUpdate(PriceUpdateEvent event) {
        if (!config.isEnabled()) {
            return;
        }
        
        String ric = event.getRic();
        PriceSnapshot price = event.getPriceSnapshot();
        
        try {
            // Validate the price update
            ValidationResult result = validator.validate(ric, price);
            
            // Handle validation results
            if (!result.isValid()) {
                handleInvalidPrice(ric, result);
            } else if (result.hasWarnings()) {
                handleWarnings(ric, result);
            }
            
        } catch (Exception e) {
            log.error("Error in data quality interceptor for {}", ric, e);
        }
    }
    
    /**
     * Handle invalid price (has errors)
     */
    private void handleInvalidPrice(String ric, ValidationResult result) {
        log.error("Invalid price data for {}: {}", ric, result.getSummary());
        
        // Send alert if alerting is enabled
        if (config.isAlertingEnabled()) {
            alertService.sendQualityAlert(ric, result);
        }
        
        // Log detailed issues
        for (ValidationResult.Issue issue : result.getErrors()) {
            log.error("  [ERROR] {}: {}", issue.getDimension(), issue.getMessage());
        }
    }
    
    /**
     * Handle warnings (non-critical issues)
     */
    private void handleWarnings(String ric, ValidationResult result) {
        log.warn("Data quality warnings for {}: {} warning(s)", ric, result.getWarningCount());
        
        // Log warnings at debug level
        for (ValidationResult.Issue issue : result.getWarnings()) {
            log.debug("  [WARNING] {}: {}", issue.getDimension(), issue.getMessage());
        }
    }
}

