package com.org.healthscore.repository.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Map;

/**
 * MongoDB document defining how signals contribute to technical debt.
 * 
 * This configuration determines debt calculation without code changes.
 */
@Data
@Document(collection = "debt_signal_contributions")
public class DebtSignalContributionDocument {
    
    @Id
    private String id;
    
    /**
     * The metric key this contribution applies to
     */
    private String metricKey;
    
    /**
     * Dimension this contributes debt to (e.g., "maintainability", "security")
     */
    private String dimension;
    
    /**
     * Threshold-based severity mappings
     * For SCALAR: {"critical": 20, "high": 50, "medium": 70}
     * For COUNTABLE_CATEGORY: category-specific thresholds
     */
    private Map<String, Object> severityThresholds;
    
    /**
     * Description template for debt items
     * Supports placeholders: {value}, {threshold}, {metricKey}
     */
    private String descriptionTemplate;
    
    /**
     * Whether this contribution is active
     */
    private boolean enabled;
}
