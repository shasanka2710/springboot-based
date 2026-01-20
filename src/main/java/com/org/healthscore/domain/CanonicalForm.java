package com.org.healthscore.domain;

/**
 * Canonical data forms define allowed data shapes beyond adapters.
 * These are framework contracts - code-defined, config-referenced.
 * 
 * NEVER add new canonical forms without code change.
 */
public enum CanonicalForm {
    
    /**
     * Map of category to count (Map<Enum, Integer>)
     * Example: {CRITICAL: 5, HIGH: 10, MEDIUM: 20}
     */
    COUNTABLE_CATEGORY,
    
    /**
     * Single numeric value (BigDecimal / Double)
     * Example: 85.5 (coverage percentage)
     */
    SCALAR,
    
    /**
     * Boolean flag
     * Example: true (has CI/CD pipeline)
     */
    BOOLEAN,
    
    /**
     * Enumerated value
     * Example: HIGH (risk level)
     */
    ENUM
}
