package com.org.healthscore.core.scoring;

import com.org.healthscore.domain.HealthScore;
import com.org.healthscore.domain.Signal;
import com.org.healthscore.repository.mongo.DebtDimensionWeightDocument;
import com.org.healthscore.repository.mongo.DebtDimensionWeightRepository;
import com.org.healthscore.repository.mongo.ScoreDocument;
import com.org.healthscore.repository.mongo.ScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates health score computation from signals.
 * 
 * Uses dimension weights from MongoDB to compute overall score.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthScoreEngine {
    
    private static final String COMPUTATION_VERSION = "1.0.0";
    
    private final SignalScoringService signalScoringService;
    private final DebtDimensionWeightRepository dimensionWeightRepository;
    private final ScoreRepository scoreRepository;
    
    /**
     * Compute health score for an entity from its signals.
     * 
     * @param entityType Type of entity (e.g., "project", "team")
     * @param entityId Unique identifier of the entity
     * @param signals All signals for this entity
     * @return Computed health score
     */
    public HealthScore computeHealthScore(String entityType, String entityId, List<Signal> signals) {
        // Score all signals
        List<SignalScoreResult> signalScores = signalScoringService.scoreSignals(signals);
        
        // Group by dimension
        Map<String, List<SignalScoreResult>> byDimension = signalScores.stream()
                .collect(Collectors.groupingBy(SignalScoreResult::dimension));
        
        // Compute dimension scores (weighted average within dimension)
        Map<String, BigDecimal> dimensionScores = new HashMap<>();
        
        for (Map.Entry<String, List<SignalScoreResult>> entry : byDimension.entrySet()) {
            String dimension = entry.getKey();
            List<SignalScoreResult> scores = entry.getValue();
            
            BigDecimal totalWeight = scores.stream()
                    .map(SignalScoreResult::weight)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal weightedSum = scores.stream()
                    .map(SignalScoreResult::weightedScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal dimensionScore = totalWeight.compareTo(BigDecimal.ZERO) > 0
                    ? weightedSum.divide(totalWeight, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            
            dimensionScores.put(dimension, dimensionScore);
        }
        
        // Get dimension weights from config
        List<DebtDimensionWeightDocument> dimensionWeights = 
                dimensionWeightRepository.findByEntityTypeOrderByDisplayOrder(entityType);
        
        // Compute overall score
        BigDecimal overallScore = computeOverallScore(dimensionScores, dimensionWeights);
        
        // Build health score
        HealthScore healthScore = HealthScore.builder()
                .id(UUID.randomUUID().toString())
                .entityType(entityType)
                .entityId(entityId)
                .overallScore(overallScore)
                .dimensionScores(dimensionScores)
                .debtContributions(Collections.emptyList()) // Computed by DebtService
                .computedAt(Instant.now())
                .computationVersion(COMPUTATION_VERSION)
                .build();
        
        // Persist the score
        saveScore(healthScore);
        
        return healthScore;
    }
    
    private BigDecimal computeOverallScore(Map<String, BigDecimal> dimensionScores,
                                           List<DebtDimensionWeightDocument> weights) {
        if (weights.isEmpty()) {
            // No weights configured - use simple average
            if (dimensionScores.isEmpty()) {
                return BigDecimal.ZERO;
            }
            return dimensionScores.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(dimensionScores.size()), 2, RoundingMode.HALF_UP);
        }
        
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal weightedSum = BigDecimal.ZERO;
        
        for (DebtDimensionWeightDocument weightDoc : weights) {
            BigDecimal dimensionScore = dimensionScores.getOrDefault(
                    weightDoc.getDimension(), BigDecimal.ZERO);
            BigDecimal weight = BigDecimal.valueOf(weightDoc.getWeight());
            
            totalWeight = totalWeight.add(weight);
            weightedSum = weightedSum.add(dimensionScore.multiply(weight));
        }
        
        return totalWeight.compareTo(BigDecimal.ZERO) > 0
                ? weightedSum.divide(totalWeight, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }
    
    private void saveScore(HealthScore healthScore) {
        ScoreDocument doc = new ScoreDocument();
        doc.setId(healthScore.getId());
        doc.setEntityType(healthScore.getEntityType());
        doc.setEntityId(healthScore.getEntityId());
        doc.setOverallScore(healthScore.getOverallScore());
        doc.setDimensionScores(healthScore.getDimensionScores());
        doc.setComputedAt(healthScore.getComputedAt());
        doc.setComputationVersion(healthScore.getComputationVersion());
        
        scoreRepository.save(doc);
        log.info("Saved health score {} for {}/{}", 
                healthScore.getOverallScore(), healthScore.getEntityType(), healthScore.getEntityId());
    }
}
