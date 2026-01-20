package com.org.healthscore.core.operators;

import com.org.healthscore.domain.CanonicalForm;
import com.org.healthscore.domain.Signal;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * Scores SCALAR signals based on threshold ranges.
 * 
 * Parameters:
 * - thresholds: List of {min, max, score} objects defining score ranges
 * - defaultScore: Score if value doesn't match any threshold (optional, defaults to 0)
 * 
 * Example config:
 * {
 *   "thresholds": [
 *     {"min": 80, "max": 100, "score": 100},
 *     {"min": 60, "max": 79.99, "score": 75},
 *     {"min": 40, "max": 59.99, "score": 50},
 *     {"min": 0, "max": 39.99, "score": 25}
 *   ],
 *   "defaultScore": 0
 * }
 */
@Component
public class ThresholdScoreOperator implements ScoringOperator {
    
    public static final String OPERATOR_ID = "THRESHOLD_SCORE";
    
    @Override
    public String getOperatorId() {
        return OPERATOR_ID;
    }
    
    @Override
    public CanonicalForm[] getSupportedForms() {
        return new CanonicalForm[]{CanonicalForm.SCALAR};
    }
    
    @Override
    public BigDecimal compute(Signal signal, Map<String, Object> parameters) {
        if (signal.getCanonicalForm() != CanonicalForm.SCALAR || signal.getScalarValue() == null) {
            throw new IllegalArgumentException("THRESHOLD_SCORE requires SCALAR signal with value");
        }
        
        BigDecimal value = signal.getScalarValue();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> thresholds = (List<Map<String, Object>>) parameters.get("thresholds");
        
        if (thresholds == null || thresholds.isEmpty()) {
            throw new IllegalArgumentException("THRESHOLD_SCORE requires non-empty thresholds parameter");
        }
        
        for (Map<String, Object> threshold : thresholds) {
            BigDecimal min = toBigDecimal(threshold.get("min"));
            BigDecimal max = toBigDecimal(threshold.get("max"));
            BigDecimal score = toBigDecimal(threshold.get("score"));
            
            if (value.compareTo(min) >= 0 && value.compareTo(max) <= 0) {
                return score.setScale(2, RoundingMode.HALF_UP);
            }
        }
        
        // Return default score if no threshold matched
        Object defaultScore = parameters.get("defaultScore");
        return defaultScore != null 
                ? toBigDecimal(defaultScore).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }
    
    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        if (parameters == null || !parameters.containsKey("thresholds")) {
            return false;
        }
        
        Object thresholds = parameters.get("thresholds");
        if (!(thresholds instanceof List)) {
            return false;
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> thresholdList = (List<Map<String, Object>>) thresholds;
        
        for (Map<String, Object> threshold : thresholdList) {
            if (!threshold.containsKey("min") || !threshold.containsKey("max") || !threshold.containsKey("score")) {
                return false;
            }
        }
        
        return true;
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
