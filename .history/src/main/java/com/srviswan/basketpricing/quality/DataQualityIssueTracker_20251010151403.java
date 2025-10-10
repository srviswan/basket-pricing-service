package com.srviswan.basketpricing.quality;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * Tracks data quality issues over time for analysis and reporting.
 * Maintains a rolling window of recent issues.
 */
@Slf4j
@Component
public class DataQualityIssueTracker {
    
    // Store last 1000 issues per symbol
    private static final int MAX_ISSUES_PER_SYMBOL = 1000;
    
    // Store issues for last 24 hours
    private static final long RETENTION_HOURS = 24;
    
    private final Map<String, ConcurrentLinkedQueue<IssueRecord>> issuesBySymbol = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<IssueRecord> allIssues = new ConcurrentLinkedQueue<>();
    
    /**
     * Record a data quality issue
     */
    public void recordIssue(String ric, ValidationResult result) {
        IssueRecord record = new IssueRecord(ric, result);
        
        // Add to symbol-specific queue
        ConcurrentLinkedQueue<IssueRecord> symbolQueue = 
            issuesBySymbol.computeIfAbsent(ric, k -> new ConcurrentLinkedQueue<>());
        symbolQueue.add(record);
        
        // Trim if too large
        while (symbolQueue.size() > MAX_ISSUES_PER_SYMBOL) {
            symbolQueue.poll();
        }
        
        // Add to global queue
        allIssues.add(record);
        
        // Clean up old issues periodically
        cleanupOldIssues();
    }
    
    /**
     * Get issues for a specific symbol
     */
    public List<IssueRecord> getIssues(String ric) {
        Queue<IssueRecord> queue = issuesBySymbol.get(ric);
        if (queue == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(queue);
    }
    
    /**
     * Get issues for a specific symbol within time range
     */
    public List<IssueRecord> getIssues(String ric, Instant since) {
        return getIssues(ric).stream()
            .filter(issue -> issue.getTimestamp().isAfter(since))
            .collect(Collectors.toList());
    }
    
    /**
     * Get all recent issues
     */
    public List<IssueRecord> getAllIssues() {
        return new ArrayList<>(allIssues);
    }
    
    /**
     * Get all issues within time range
     */
    public List<IssueRecord> getAllIssues(Instant since) {
        return allIssues.stream()
            .filter(issue -> issue.getTimestamp().isAfter(since))
            .collect(Collectors.toList());
    }
    
    /**
     * Get issue count for a symbol
     */
    public long getIssueCount(String ric) {
        Queue<IssueRecord> queue = issuesBySymbol.get(ric);
        return queue != null ? queue.size() : 0;
    }
    
    /**
     * Get symbols with most issues
     */
    public List<Map.Entry<String, Long>> getTopOffenders(int limit) {
        return issuesBySymbol.entrySet().stream()
            .map(e -> Map.entry(e.getKey(), (long) e.getValue().size()))
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Get issue breakdown by dimension
     */
    public Map<ValidationDimension, Long> getIssuesByDimension() {
        Map<ValidationDimension, Long> breakdown = new EnumMap<>(ValidationDimension.class);
        
        for (IssueRecord record : allIssues) {
            for (ValidationResult.Issue issue : record.getResult().getIssues()) {
                breakdown.merge(issue.getDimension(), 1L, Long::sum);
            }
        }
        
        return breakdown;
    }
    
    /**
     * Generate summary report
     */
    public QualitySummary getSummary() {
        Instant oneHourAgo = Instant.now().minusSeconds(3600);
        List<IssueRecord> recentIssues = getAllIssues(oneHourAgo);
        
        QualitySummary summary = new QualitySummary();
        summary.setTotalIssues(recentIssues.size());
        summary.setAffectedSymbols(
            recentIssues.stream()
                .map(IssueRecord::getRic)
                .distinct()
                .count()
        );
        
        // Count by severity
        long errors = recentIssues.stream()
            .mapToLong(r -> r.getResult().getErrorCount())
            .sum();
        long warnings = recentIssues.stream()
            .mapToLong(r -> r.getResult().getWarningCount())
            .sum();
        
        summary.setTotalErrors(errors);
        summary.setTotalWarnings(warnings);
        summary.setTopOffenders(getTopOffenders(10));
        summary.setIssuesByDimension(getIssuesByDimension());
        
        return summary;
    }
    
    /**
     * Clean up old issues (older than retention period)
     */
    private void cleanupOldIssues() {
        // Only clean up occasionally (every 100 issues)
        if (allIssues.size() % 100 != 0) {
            return;
        }
        
        Instant cutoff = Instant.now().minusSeconds(RETENTION_HOURS * 3600);
        
        // Clean global queue
        allIssues.removeIf(issue -> issue.getTimestamp().isBefore(cutoff));
        
        // Clean symbol-specific queues
        issuesBySymbol.values().forEach(queue -> 
            queue.removeIf(issue -> issue.getTimestamp().isBefore(cutoff))
        );
        
        log.debug("Cleaned up old data quality issues. Current size: {}", allIssues.size());
    }
    
    /**
     * Clear all tracked issues (for testing)
     */
    public void clear() {
        issuesBySymbol.clear();
        allIssues.clear();
    }
    
    /**
     * Record of a data quality issue
     */
    @Data
    public static class IssueRecord {
        private final String ric;
        private final ValidationResult result;
        private final Instant timestamp = Instant.now();
    }
    
    /**
     * Summary of data quality issues
     */
    @Data
    public static class QualitySummary {
        private long totalIssues;
        private long affectedSymbols;
        private long totalErrors;
        private long totalWarnings;
        private List<Map.Entry<String, Long>> topOffenders;
        private Map<ValidationDimension, Long> issuesByDimension;
        
        public String toFormattedString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Data Quality Summary (Last Hour)\n");
            sb.append("═══════════════════════════════════\n");
            sb.append(String.format("Total Issues: %d\n", totalIssues));
            sb.append(String.format("Affected Symbols: %d\n", affectedSymbols));
            sb.append(String.format("Errors: %d | Warnings: %d\n", totalErrors, totalWarnings));
            sb.append("\nTop Offenders:\n");
            
            if (topOffenders != null) {
                for (Map.Entry<String, Long> entry : topOffenders) {
                    sb.append(String.format("  %s: %d issues\n", entry.getKey(), entry.getValue()));
                }
            }
            
            sb.append("\nIssues by Dimension:\n");
            if (issuesByDimension != null) {
                issuesByDimension.forEach((dim, count) -> 
                    sb.append(String.format("  %s: %d\n", dim, count))
                );
            }
            
            return sb.toString();
        }
    }
}

