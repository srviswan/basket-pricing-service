package com.srviswan.basketpricing.quality.dto;

import com.srviswan.basketpricing.quality.ValidationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO for issue records (Jackson-serializable)
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IssueRecordDTO {
    private String ric;
    private Instant timestamp;
    private Instant validationTime;
    private long errorCount;
    private long warningCount;
    private List<IssueDTO> issues;
    
    /**
     * Convert from ValidationResult
     */
    public static IssueRecordDTO from(String ric, ValidationResult result, Instant timestamp) {
        return IssueRecordDTO.builder()
            .ric(ric)
            .timestamp(timestamp)
            .validationTime(result.getValidationTime())
            .errorCount(result.getErrorCount())
            .warningCount(result.getWarningCount())
            .issues(result.getIssues().stream()
                .map(IssueDTO::from)
                .collect(Collectors.toList()))
            .build();
    }
}

