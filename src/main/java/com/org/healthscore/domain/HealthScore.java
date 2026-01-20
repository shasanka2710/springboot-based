package com.org.healthscore.domain;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents the final health score for an entity (project, team, etc.).
 * 
 * Health scores are computed from signals using scoring rules defined in MongoDB.
 */
@Data
@Builder
public class HealthScore {
    
    private String id;
    private String entityType;
    private String entityId;
    private BigDecimal overallScore;
    private Map<String, BigDecimal> dimensionScores;
    private List<DebtContribution> debtContributions;
    private Instant computedAt;
    private String computationVersion;
}
