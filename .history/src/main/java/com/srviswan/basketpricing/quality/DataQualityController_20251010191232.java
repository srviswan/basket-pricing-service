package com.srviswan.basketpricing.quality;

import com.srviswan.basketpricing.quality.dto.IssueRecordDTO;
import com.srviswan.basketpricing.quality.dto.QualitySummaryDTO;
import com.srviswan.basketpricing.quality.dto.SymbolIssueCount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API for data quality monitoring and reporting
 */
@Slf4j
@RestController
@RequestMapping("/api/data-quality")
@RequiredArgsConstructor
public class DataQualityController {
    
    private final DataQualityMetrics metrics;
    private final DataQualityIssueTracker issueTracker;
    private final DataQualityConfig config;
    
    /**
     * Get overall data quality score
     */
    @GetMapping("/score")
    public ResponseEntity<Map<String, Object>> getQualityScore() {
        Map<String, Object> response = new HashMap<>();
        response.put("score", metrics.getQualityScore());
        response.put("threshold", config.getMinQualityScore());
        response.put("healthy", metrics.getQualityScore() >= config.getMinQualityScore());
        response.put("timestamp", Instant.now());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get quality score for a specific symbol
     */
    @GetMapping("/score/{ric}")
    public ResponseEntity<Map<String, Object>> getSymbolQualityScore(@PathVariable String ric) {
        Map<String, Object> response = new HashMap<>();
        response.put("ric", ric);
        response.put("score", metrics.getSymbolQualityScore(ric));
        response.put("issueCount", issueTracker.getIssueCount(ric));
        response.put("timestamp", Instant.now());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get summary of data quality issues
     */
    @GetMapping("/summary")
    public ResponseEntity<QualitySummaryDTO> getSummary() {
        DataQualityIssueTracker.QualitySummary summary = issueTracker.getSummary();
        
        // Convert to DTO for proper serialization
        QualitySummaryDTO dto = QualitySummaryDTO.builder()
            .totalIssues(summary.getTotalIssues())
            .affectedSymbols(summary.getAffectedSymbols())
            .totalErrors(summary.getTotalErrors())
            .totalWarnings(summary.getTotalWarnings())
            .topOffenders(summary.getTopOffenders().stream()
                .map(entry -> new SymbolIssueCount(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()))
            .issuesByDimension(summary.getIssuesByDimension().entrySet().stream()
                .collect(Collectors.toMap(
                    e -> e.getKey().name(),  // Convert enum to String
                    Map.Entry::getValue
                )))
            .build();
        
        return ResponseEntity.ok(dto);
    }
    
    /**
     * Get issues for a specific symbol
     */
    @GetMapping("/issues/{ric}")
    public ResponseEntity<List<IssueRecordDTO>> getIssues(
            @PathVariable String ric,
            @RequestParam(required = false, defaultValue = "1") int hours) {
        
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<DataQualityIssueTracker.IssueRecord> issues = issueTracker.getIssues(ric, since);
        
        // Convert to DTOs
        List<IssueRecordDTO> dtos = issues.stream()
            .map(issue -> IssueRecordDTO.from(issue.getRic(), issue.getResult(), issue.getTimestamp()))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * Get all recent issues
     */
    @GetMapping("/issues")
    public ResponseEntity<List<IssueRecordDTO>> getAllIssues(
            @RequestParam(required = false, defaultValue = "1") int hours) {
        
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<DataQualityIssueTracker.IssueRecord> issues = issueTracker.getAllIssues(since);
        
        // Convert to DTOs
        List<IssueRecordDTO> dtos = issues.stream()
            .map(issue -> IssueRecordDTO.from(issue.getRic(), issue.getResult(), issue.getTimestamp()))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * Get top offenders (symbols with most issues)
     */
    @GetMapping("/top-offenders")
    public ResponseEntity<List<Map.Entry<String, Long>>> getTopOffenders(
            @RequestParam(required = false, defaultValue = "10") int limit) {
        
        return ResponseEntity.ok(issueTracker.getTopOffenders(limit));
    }
    
    /**
     * Get issue breakdown by dimension
     */
    @GetMapping("/breakdown")
    public ResponseEntity<Map<ValidationDimension, Long>> getIssueBreakdown() {
        return ResponseEntity.ok(issueTracker.getIssuesByDimension());
    }
    
    /**
     * Get current configuration
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("enabled", config.isEnabled());
        configMap.put("alertingEnabled", config.isAlertingEnabled());
        configMap.put("minPrice", config.getMinPrice());
        configMap.put("maxPrice", config.getMaxPrice());
        configMap.put("maxSpreadPercentage", config.getMaxSpreadPercentage());
        configMap.put("maxAge", config.getMaxAge().toString());
        configMap.put("maxDecimalPlaces", config.getMaxDecimalPlaces());
        configMap.put("minQualityScore", config.getMinQualityScore());
        configMap.put("circuitBreakerThreshold", config.getCircuitBreakerThreshold());
        
        return ResponseEntity.ok(configMap);
    }
    
    /**
     * Health check - returns 200 if quality is good, 503 if poor
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        long score = metrics.getQualityScore();
        boolean healthy = score >= config.getMinQualityScore();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", healthy ? "UP" : "DOWN");
        response.put("score", score);
        response.put("threshold", config.getMinQualityScore());
        response.put("timestamp", Instant.now());
        
        if (healthy) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(503).body(response);
        }
    }
    
    /**
     * Get detailed report
     */
    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> getDetailedReport() {
        Map<String, Object> report = new HashMap<>();
        
        // Overall metrics
        report.put("overallScore", metrics.getQualityScore());
        report.put("timestamp", Instant.now());
        
        // Summary
        DataQualityIssueTracker.QualitySummary summary = issueTracker.getSummary();
        report.put("summary", summary);
        
        // Top offenders
        report.put("topOffenders", issueTracker.getTopOffenders(10));
        
        // Issue breakdown
        report.put("issuesByDimension", issueTracker.getIssuesByDimension());
        
        // Configuration (as map to avoid serialization issues)
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("enabled", config.isEnabled());
        configMap.put("alertingEnabled", config.isAlertingEnabled());
        configMap.put("minPrice", config.getMinPrice());
        configMap.put("maxPrice", config.getMaxPrice());
        configMap.put("maxSpreadPercentage", config.getMaxSpreadPercentage());
        configMap.put("maxAge", config.getMaxAge().toString());
        configMap.put("maxDecimalPlaces", config.getMaxDecimalPlaces());
        configMap.put("minQualityScore", config.getMinQualityScore());
        report.put("config", configMap);
        
        return ResponseEntity.ok(report);
    }
    
    /**
     * Clear all tracked issues (admin endpoint)
     */
    @DeleteMapping("/issues")
    public ResponseEntity<Map<String, String>> clearIssues() {
        issueTracker.clear();
        metrics.reset();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "cleared");
        response.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }
    
}

