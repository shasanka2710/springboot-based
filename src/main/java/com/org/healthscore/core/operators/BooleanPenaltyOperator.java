package com.org.healthscore.core.operators;

import com.org.healthscore.domain.CanonicalForm;
import com.org.healthscore.domain.Signal;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Scores BOOLEAN signals with configurable scores for true/false.
 * 
 * Parameters:
 * - trueScore: Score when signal is true (default 100)
 * - falseScore: Score when signal is false (default 0)
 * 
 * Example config for "has CI/CD pipeline":
 * {
 *   "trueScore": 100,
 *   "falseScore": 0
 * }
 */
@Component
public class BooleanPenaltyOperator implements ScoringOperator {
    
    public static final String OPERATOR_ID = "BOOLEAN_PENALTY";
    
    @Override
    public String getOperatorId() {
        return OPERATOR_ID;
    }
    
    @Override
    public CanonicalForm[] getSupportedForms() {
        return new CanonicalForm[]{CanonicalForm.BOOLEAN};
    }
    
    @Override
    public BigDecimal compute(Signal signal, Map<String, Object> parameters) {
        if (signal.getCanonicalForm() != CanonicalForm.BOOLEAN || signal.getBooleanValue() == null) {
            throw new IllegalArgumentException("BOOLEAN_PENALTY requires BOOLEAN signal with value");
        }
        
        BigDecimal trueScore = getParameterOrDefault(parameters, "trueScore", BigDecimal.valueOf(100));
        BigDecimal falseScore = getParameterOrDefault(parameters, "falseScore", BigDecimal.ZERO);
        
        return signal.getBooleanValue() 
                ? trueScore.setScale(2, RoundingMode.HALF_UP)
                : falseScore.setScale(2, RoundingMode.HALF_UP);
    }
    
    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        // Parameters are optional with sensible defaults
        return true;
    }
    
    private BigDecimal getParameterOrDefault(Map<String, Object> parameters, String key, BigDecimal defaultValue) {
        if (parameters == null) {
            return defaultValue;
        }
        Object value = parameters.get(key);
        return value != null ? toBigDecimal(value) : defaultValue;
    }
    
    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        } else if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        } else if (value instanceof String) {
            return new BigDecimal((String) value);
        }
        throw new IllegalArgumentException("Cannot convert to BigDecimal: " + value);
    }
}
