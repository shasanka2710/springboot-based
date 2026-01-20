package com.org.healthscore.repository.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB document defining dimension weights for overall score calculation.
 * 
 * All weights must sum to 1.0 for a given entity type.
 */
@Data
@Document(collection = "debt_dimension_weights")
public class DebtDimensionWeightDocument {
    
    @Id
    private String id;
    
    /**
     * Entity type these weights apply to (e.g., "project", "team")
     */
    private String entityType;
    
    /**
     * Dimension name (e.g., "code_quality", "reliability", "security")
     */
    private String dimension;
    
    /**
     * Weight for this dimension (0.0 - 1.0)
     * All dimension weights for an entity type must sum to 1.0
     */
    private Double weight;
    
    /**
     * Display order for UI purposes
     */
    private Integer displayOrder;
    
    /**
     * Human-readable description
     */
    private String description;
}
