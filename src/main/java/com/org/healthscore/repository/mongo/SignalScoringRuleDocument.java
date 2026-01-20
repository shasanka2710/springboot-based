package com.org.healthscore.repository.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Map;

/**
 * MongoDB document defining scoring rules for signals.
 * 
 * Scoring rules determine how signals are converted to scores.
 * Operators are fixed in code; parameters come from this config.
 */
@Data
@Document(collection = "signal_scoring_rules")
public class SignalScoringRuleDocument {
    
    @Id
    private String id;
    
    /**
     * The metric key this rule applies to
     */
    private String metricKey;
    
    /**
     * Required canonical form for this rule
     */
    private String requiredCanonicalForm;
    
    /**
     * Fixed operator to use. Must be one of the code-defined operators:
     * - THRESHOLD_SCORE
     * - WEIGHTED_CATEGORY_SUM
     * - BOOLEAN_PENALTY
     * - ENUM_MAPPING
     */
    private String operator;
    
    /**
     * Operator parameters - structure depends on operator type
     */
    private Map<String, Object> parameters;
    
    /**
     * Weight of this signal in the dimension score (0.0 - 1.0)
     */
    private Double weight;
    
    /**
     * Dimension this signal contributes to (e.g., "code_quality", "reliability")
     */
    private String dimension;
    
    /**
     * Whether this rule is active
     */
    private boolean enabled;
}
