package com.srviswan.basketpricing.quality.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for symbol issue counts (Jackson-serializable)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SymbolIssueCount {
    private String symbol;
    private long issueCount;
}

