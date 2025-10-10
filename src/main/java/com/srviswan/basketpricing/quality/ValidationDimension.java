package com.srviswan.basketpricing.quality;

/**
 * Data quality dimensions for pricing data
 */
public enum ValidationDimension {
    COMPLETENESS,  // All required fields present
    VALIDITY,      // Values are in valid range
    CONSISTENCY,   // Values are consistent with each other
    TIMELINESS,    // Data is fresh/not stale
    ACCURACY,      // Precision and format are correct
    SYSTEM         // System/technical errors
}

