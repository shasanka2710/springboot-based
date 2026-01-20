package com.org.healthscore.domain;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Represents a normalized signal in canonical form.
 * 
 * Signals are the output of adapters after normalizing external tool data.
 * All signals must conform to one of the defined canonical forms.
 */
@Data
@Builder
public class Signal {
    
    private String id;
    private String sourceType;
    private String sourceId;
    private String metricKey;
    private CanonicalForm canonicalForm;
    
    // Value holders - only one should be set based on canonicalForm
    private Map<String, Integer> countableValue;
    private BigDecimal scalarValue;
    private Boolean booleanValue;
    private String enumValue;
    
    private Instant timestamp;
    private Map<String, Object> metadata;
    
    /**
     * Validates that the signal value matches its declared canonical form.
     */
    public boolean isValid() {
        return switch (canonicalForm) {
            case COUNTABLE_CATEGORY -> countableValue != null && !countableValue.isEmpty();
            case SCALAR -> scalarValue != null;
            case BOOLEAN -> booleanValue != null;
            case ENUM -> enumValue != null && !enumValue.isBlank();
        };
    }
}
