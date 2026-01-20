package com.org.healthscore.repository.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Map;

/**
 * MongoDB document defining how to extract and normalize signals from external tools.
 * 
 * This configuration drives adapter behavior without code changes.
 * All business meaning lives in MongoDB - adapters only normalize data.
 */
@Data
@Document(collection = "adapter_signal_definitions")
public class AdapterSignalDefinitionDocument {
    
    @Id
    private String id;
    
    /**
     * Source tool identifier (e.g., "sonarqube", "jira", "github")
     */
    private String sourceType;
    
    /**
     * Metric key for this signal (e.g., "code_coverage", "open_bugs")
     */
    private String metricKey;
    
    /**
     * The canonical form this signal normalizes to.
     * Must be one of: COUNTABLE_CATEGORY, SCALAR, BOOLEAN, ENUM
     */
    private String canonicalForm;
    
    /**
     * JSON path or extraction configuration for the source data
     */
    private String extractionPath;
    
    /**
     * Mapping rules for COUNTABLE_CATEGORY (e.g., severity mappings)
     */
    private Map<String, String> categoryMappings;
    
    /**
     * Transformation rules (e.g., unit conversion for SCALAR)
     */
    private TransformationConfig transformation;
    
    /**
     * Whether this signal definition is active
     */
    private boolean enabled;
    
    /**
     * Description for documentation purposes
     */
    private String description;
    
    @Data
    public static class TransformationConfig {
        private String type; // "percentage", "invert", "scale"
        private Double factor;
        private Double min;
        private Double max;
    }
}
