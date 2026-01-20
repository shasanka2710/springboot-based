package com.org.healthscore.core.operators;

import com.org.healthscore.domain.CanonicalForm;
import com.org.healthscore.domain.Signal;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Scores ENUM signals by mapping enum values to scores.
 * 
 * Parameters:
 * - mapping: Map of enum value to score
 * - defaultScore: Score for unmapped values (default 0)
 * 
 * Example config for risk level:
 * {
 *   "mapping": {"LOW": 100, "MEDIUM": 60, "HIGH": 30, "CRITICAL": 0},
 *   "defaultScore": 50
 * }
 */
@Component
public class EnumMappingOperator implements ScoringOperator {
    
    public static final String OPERATOR_ID = "ENUM_MAPPING";
    
    @Override
    public String getOperatorId() {
        return OPERATOR_ID;
    }
    
    @Override
    public CanonicalForm[] getSupportedForms() {
        return new CanonicalForm[]{CanonicalForm.ENUM};
    }
    
    @Override
    public BigDecimal compute(Signal signal, Map<String, Object> parameters) {
        if (signal.getCanonicalForm() != CanonicalForm.ENUM || signal.getEnumValue() == null) {
            throw new IllegalArgumentException("ENUM_MAPPING requires ENUM signal with value");
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> mapping = (Map<String, Object>) parameters.get("mapping");
        
        if (mapping == null || mapping.isEmpty()) {
            throw new IllegalArgumentException("ENUM_MAPPING requires non-empty mapping parameter");
        }
        
        String enumValue = signal.getEnumValue();
        
        if (mapping.containsKey(enumValue)) {
            return toBigDecimal(mapping.get(enumValue)).setScale(2, RoundingMode.HALF_UP);
        }
        
        // Return default score for unmapped values
        Object defaultScore = parameters.get("defaultScore");
        return defaultScore != null 
                ? toBigDecimal(defaultScore).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }
    
    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        return parameters != null && parameters.containsKey("mapping") && parameters.get("mapping") instanceof Map;
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
