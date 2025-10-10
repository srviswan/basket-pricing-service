package com.srviswan.basketpricing.quality;

import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Result of data quality validation for a single price
 */
@Data
public class ValidationResult {
    private final String ric;
    private final Instant validationTime = Instant.now();
    private final List<Issue> issues = new ArrayList<>();
    
    public void addError(ValidationDimension dimension, String message) {
        issues.add(new Issue(IssueLevel.ERROR, dimension, message));
    }
    
    public void addWarning(ValidationDimension dimension, String message) {
        issues.add(new Issue(IssueLevel.WARNING, dimension, message));
    }
    
    public boolean isValid() {
        return issues.stream().noneMatch(i -> i.getLevel() == IssueLevel.ERROR);
    }
    
    public boolean hasWarnings() {
        return issues.stream().anyMatch(i -> i.getLevel() == IssueLevel.WARNING);
    }
    
    public long getErrorCount() {
        return issues.stream().filter(i -> i.getLevel() == IssueLevel.ERROR).count();
    }
    
    public long getWarningCount() {
        return issues.stream().filter(i -> i.getLevel() == IssueLevel.WARNING).count();
    }
    
    public String getSummary() {
        if (issues.isEmpty()) {
            return "All checks passed";
        }
        
        return String.format("%d error(s), %d warning(s): %s", 
            getErrorCount(), 
            getWarningCount(),
            issues.stream()
                .map(i -> i.getDimension() + ":" + i.getMessage())
                .collect(Collectors.joining("; ")));
    }
    
    public List<Issue> getErrors() {
        return issues.stream()
            .filter(i -> i.getLevel() == IssueLevel.ERROR)
            .collect(Collectors.toList());
    }
    
    public List<Issue> getWarnings() {
        return issues.stream()
            .filter(i -> i.getLevel() == IssueLevel.WARNING)
            .collect(Collectors.toList());
    }
    
    @Data
    public static class Issue {
        private final IssueLevel level;
        private final ValidationDimension dimension;
        private final String message;
        private final Instant timestamp = Instant.now();
    }
    
    public enum IssueLevel {
        ERROR,   // Critical issue - data should not be used
        WARNING  // Non-critical issue - data can be used with caution
    }
}

