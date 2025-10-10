package com.srviswan.basketpricing.quality.dto;

import com.srviswan.basketpricing.quality.ValidationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for individual issues (Jackson-serializable)
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IssueDTO {
    private String level;
    private String dimension;
    private String message;
    private Instant timestamp;
    
    /**
     * Convert from ValidationResult.Issue
     */
    public static IssueDTO from(ValidationResult.Issue issue) {
        return IssueDTO.builder()
            .level(issue.getLevel().name())
            .dimension(issue.getDimension().name())
            .message(issue.getMessage())
            .timestamp(issue.getTimestamp())
            .build();
    }
}

