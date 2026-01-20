package com.org.healthscore.repository.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.Map;

/**
 * MongoDB document storing normalized signals.
 * 
 * Signals are the output of adapters after processing external tool data.
 */
@Data
@Document(collection = "signals")
public class SignalDocument {
    
    @Id
    private String id;
    
    private String sourceType;
    private String sourceId;
    private String metricKey;
    private String canonicalForm;
    
    /**
     * Value storage - structure depends on canonicalForm:
     * - COUNTABLE_CATEGORY: {"CRITICAL": 5, "HIGH": 10}
     * - SCALAR: {"value": 85.5}
     * - BOOLEAN: {"value": true}
     * - ENUM: {"value": "HIGH"}
     */
    private Map<String, Object> value;
    
    private Instant timestamp;
    private Map<String, Object> metadata;
    
    /**
     * Entity this signal belongs to (project, team, etc.)
     */
    private String entityType;
    private String entityId;
}
