package com.org.healthscore.repository.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * MongoDB document storing computed health scores.
 */
@Data
@Document(collection = "scores")
public class ScoreDocument {
    
    @Id
    private String id;
    
    private String entityType;
    private String entityId;
    private BigDecimal overallScore;
    private Map<String, BigDecimal> dimensionScores;
    private List<DebtContributionEmbedded> debtContributions;
    private Instant computedAt;
    private String computationVersion;
    
    @Data
    public static class DebtContributionEmbedded {
        private String signalId;
        private String metricKey;
        private String dimension;
        private BigDecimal contribution;
        private String severity;
        private String description;
    }
}
