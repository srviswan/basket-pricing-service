package com.srviswan.basketpricing.quality.dto;

import com.srviswan.basketpricing.quality.ValidationDimension;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for quality summary (Jackson-serializable)
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QualitySummaryDTO {
    private long totalIssues;
    private long affectedSymbols;
    private long totalErrors;
    private long totalWarnings;
    private List<SymbolIssueCount> topOffenders;
    private Map<String, Long> issuesByDimension;  // Changed from enum key to String
}

