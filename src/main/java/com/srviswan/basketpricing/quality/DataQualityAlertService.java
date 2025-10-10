package com.srviswan.basketpricing.quality;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for alerting on data quality issues.
 * Implements rate limiting to prevent alert storms.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataQualityAlertService {
    
    private final DataQualityIssueTracker issueTracker;
    
    // Rate limiting: track last alert time per symbol
    private final Map<String, Instant> lastAlertTime = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> alertsSuppressed = new ConcurrentHashMap<>();
    
    // Alert throttling: minimum time between alerts for same symbol
    private static final long ALERT_THROTTLE_SECONDS = 60;
    
    /**
     * Send alert for data quality issue
     */
    public void sendQualityAlert(String ric, ValidationResult result) {
        // Check if we should throttle this alert
        if (shouldThrottle(ric)) {
            alertsSuppressed.computeIfAbsent(ric, k -> new AtomicInteger(0)).incrementAndGet();
            return;
        }
        
        // Record the issue
        issueTracker.recordIssue(ric, result);
        
        // Log the alert
        logAlert(ric, result);
        
        // Update last alert time
        lastAlertTime.put(ric, Instant.now());
        
        // Reset suppression counter
        int suppressed = alertsSuppressed.getOrDefault(ric, new AtomicInteger(0)).getAndSet(0);
        if (suppressed > 0) {
            log.info("Suppressed {} alerts for {} in the last {} seconds", 
                suppressed, ric, ALERT_THROTTLE_SECONDS);
        }
        
        // In production, you would:
        // - Send to monitoring system (PagerDuty, OpsGenie, etc.)
        // - Send email/Slack notification
        // - Create incident ticket
        // - Trigger circuit breaker if threshold exceeded
    }
    
    /**
     * Check if alert should be throttled
     */
    private boolean shouldThrottle(String ric) {
        Instant lastAlert = lastAlertTime.get(ric);
        if (lastAlert == null) {
            return false;
        }
        
        long secondsSinceLastAlert = Instant.now().getEpochSecond() - lastAlert.getEpochSecond();
        return secondsSinceLastAlert < ALERT_THROTTLE_SECONDS;
    }
    
    /**
     * Log alert details
     */
    private void logAlert(String ric, ValidationResult result) {
        log.error("╔══════════════════════════════════════════════════════════════");
        log.error("║ DATA QUALITY ALERT");
        log.error("╠══════════════════════════════════════════════════════════════");
        log.error("║ Symbol: {}", ric);
        log.error("║ Errors: {}", result.getErrorCount());
        log.error("║ Warnings: {}", result.getWarningCount());
        log.error("║ Time: {}", result.getValidationTime());
        log.error("╠══════════════════════════════════════════════════════════════");
        
        // Log each error
        for (ValidationResult.Issue issue : result.getErrors()) {
            log.error("║ [ERROR] {}: {}", issue.getDimension(), issue.getMessage());
        }
        
        // Log each warning
        for (ValidationResult.Issue issue : result.getWarnings()) {
            log.error("║ [WARN]  {}: {}", issue.getDimension(), issue.getMessage());
        }
        
        log.error("╚══════════════════════════════════════════════════════════════");
    }
    
    /**
     * Send summary alert for multiple issues
     */
    public void sendSummaryAlert(Map<String, List<ValidationResult>> issues) {
        log.error("╔══════════════════════════════════════════════════════════════");
        log.error("║ DATA QUALITY SUMMARY ALERT");
        log.error("╠══════════════════════════════════════════════════════════════");
        log.error("║ Total symbols with issues: {}", issues.size());
        
        int totalErrors = issues.values().stream()
            .mapToInt(list -> list.stream().mapToInt(r -> (int) r.getErrorCount()).sum())
            .sum();
        int totalWarnings = issues.values().stream()
            .mapToInt(list -> list.stream().mapToInt(r -> (int) r.getWarningCount()).sum())
            .sum();
        
        log.error("║ Total errors: {}", totalErrors);
        log.error("║ Total warnings: {}", totalWarnings);
        log.error("╠══════════════════════════════════════════════════════════════");
        
        // Log top offenders
        issues.entrySet().stream()
            .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
            .limit(10)
            .forEach(entry -> {
                log.error("║ {}: {} issues", entry.getKey(), entry.getValue().size());
            });
        
        log.error("╚══════════════════════════════════════════════════════════════");
    }
}

