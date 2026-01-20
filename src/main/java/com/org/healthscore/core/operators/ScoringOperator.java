package com.org.healthscore.core.operators;

import com.org.healthscore.domain.CanonicalForm;
import com.org.healthscore.domain.Signal;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Contract for scoring operators.
 * 
 * Operators are FIXED in code - they define HOW to compute scores.
 * Parameters from MongoDB define WHAT values to use.
 * 
 * NEVER add new operators without code change.
 */
public interface ScoringOperator {
    
    /**
     * Unique operator identifier.
     */
    String getOperatorId();
    
    /**
     * Canonical forms this operator can process.
     */
    CanonicalForm[] getSupportedForms();
    
    /**
     * Compute a score from a signal using provided parameters.
     * 
     * @param signal The normalized signal to score
     * @param parameters Operator parameters from MongoDB config
     * @return Score between 0.0 and 100.0
     */
    BigDecimal compute(Signal signal, Map<String, Object> parameters);
    
    /**
     * Validate that parameters are valid for this operator.
     */
    boolean validateParameters(Map<String, Object> parameters);
}
