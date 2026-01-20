package com.org.healthscore.core.operators;

import com.org.healthscore.domain.CanonicalForm;
import com.org.healthscore.domain.Signal;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Scores COUNTABLE_CATEGORY signals using weighted sum.
 * 
 * Parameters:
 * - weights: Map of category to weight (negative weights for penalties)
 * - baseScore: Starting score (default 100)
 * - minScore: Minimum allowed score (default 0)
 * - maxScore: Maximum allowed score (default 100)
 * 
 * Example config for bug severity scoring:
 * {
 *   "weights": {"CRITICAL": -20, "HIGH": -10, "MEDIUM": -5, "LOW": -1},
 *   "baseScore": 100,
 *   "minScore": 0,
 *   "maxScore": 100
 * }
 */
@Component
public class WeightedCategorySumOperator implements ScoringOperator {
    
    public static final String OPERATOR_ID = "WEIGHTED_CATEGORY_SUM";
    
    @Override
    public String getOperatorId() {
        return OPERATOR_ID;
    }
    
    @Override
    public CanonicalForm[] getSupportedForms() {
        return new CanonicalForm[]{CanonicalForm.COUNTABLE_CATEGORY};
    }
    
    @Override
    public BigDecimal compute(Signal signal, Map<String, Object> parameters) {
        if (signal.getCanonicalForm() != CanonicalForm.COUNTABLE_CATEGORY || signal.getCountableValue() == null) {
            throw new IllegalArgumentException("WEIGHTED_CATEGORY_SUM requires COUNTABLE_CATEGORY signal");
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> weights = (Map<String, Object>) parameters.get("weights");
        
        if (weights == null || weights.isEmpty()) {
            throw new IllegalArgumentException("WEIGHTED_CATEGORY_SUM requires non-empty weights parameter");
        }
        
        BigDecimal baseScore = getParameterOrDefault(parameters, "baseScore", BigDecimal.valueOf(100));
        BigDecimal minScore = getParameterOrDefault(parameters, "minScore", BigDecimal.ZERO);
        BigDecimal maxScore = getParameterOrDefault(parameters, "maxScore", BigDecimal.valueOf(100));
        
        BigDecimal score = baseScore;
        
        for (Map.Entry<String, Integer> entry : signal.getCountableValue().entrySet()) {
            String category = entry.getKey();
            Integer count = entry.getValue();
            
            if (weights.containsKey(category) && count != null) {
                BigDecimal weight = toBigDecimal(weights.get(category));
                score = score.add(weight.multiply(BigDecimal.valueOf(count)));
            }
        }
        
        // Clamp to min/max
        if (score.compareTo(minScore) < 0) {
            score = minScore;
        }
        if (score.compareTo(maxScore) > 0) {
            score = maxScore;
        }
        
        return score.setScale(2, RoundingMode.HALF_UP);
    }
    
    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        return parameters != null && parameters.containsKey("weights") && parameters.get("weights") instanceof Map;
    }
    
    private BigDecimal getParameterOrDefault(Map<String, Object> parameters, String key, BigDecimal defaultValue) {
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
