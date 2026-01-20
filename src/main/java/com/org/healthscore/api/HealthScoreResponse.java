package com.org.healthscore.api;

import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * API response for health score.
 */
@Data
public class HealthScoreResponse {
    private String entityType;
    private String entityId;
    private BigDecimal overallScore;
    private Map<String, BigDecimal> dimensionScores;
    private List<DebtContributionDto> debtContributions;
    private Instant computedAt;
    
    @Data
    public static class DebtContributionDto {
        private String metricKey;
        private String dimension;
        private BigDecimal contribution;
        private String severity;
        private String description;
    }
}
