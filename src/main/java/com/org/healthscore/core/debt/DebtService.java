package com.org.healthscore.core.debt;

import com.org.healthscore.domain.DebtContribution;
import com.org.healthscore.domain.Signal;
import com.org.healthscore.repository.mongo.DebtSignalContributionDocument;
import com.org.healthscore.repository.mongo.DebtSignalContributionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Computes technical debt contributions from signals.
 * 
 * Debt rules are configuration-driven from MongoDB.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DebtService {
    
    private final DebtSignalContributionRepository debtContributionRepository;
    
    /**
     * Compute debt contributions for a list of signals.
     */
    public List<DebtContribution> computeDebtContributions(List<Signal> signals) {
        List<DebtContribution> contributions = new ArrayList<>();
        
        for (Signal signal : signals) {
            computeDebtForSignal(signal).ifPresent(contributions::add);
        }
        
        return contributions;
    }
    
    private Optional<DebtContribution> computeDebtForSignal(Signal signal) {
        DebtSignalContributionDocument config = 
                debtContributionRepository.findByMetricKey(signal.getMetricKey());
        
        if (config == null || !config.isEnabled()) {
            return Optional.empty();
        }
        
        return switch (signal.getCanonicalForm()) {
            case SCALAR -> computeScalarDebt(signal, config);
            case COUNTABLE_CATEGORY -> computeCountableDebt(signal, config);
            case BOOLEAN -> computeBooleanDebt(signal, config);
            case ENUM -> computeEnumDebt(signal, config);
        };
    }
    
    private Optional<DebtContribution> computeScalarDebt(Signal signal, DebtSignalContributionDocument config) {
        BigDecimal value = signal.getScalarValue();
        if (value == null) {
            return Optional.empty();
        }
        
        Map<String, Object> thresholds = config.getSeverityThresholds();
        if (thresholds == null) {
            return Optional.empty();
        }
        
        String severity = determineSeverity(value.doubleValue(), thresholds);
        if (severity == null) {
            return Optional.empty();
        }
        
        BigDecimal contribution = calculateContribution(value, severity);
        String description = formatDescription(config.getDescriptionTemplate(), value, signal.getMetricKey());
        
        return Optional.of(DebtContribution.builder()
                .signalId(signal.getId())
                .metricKey(signal.getMetricKey())
                .dimension(config.getDimension())
                .contribution(contribution)
                .severity(severity)
                .description(description)
                .build());
    }
    
    private Optional<DebtContribution> computeCountableDebt(Signal signal, DebtSignalContributionDocument config) {
        Map<String, Integer> counts = signal.getCountableValue();
        if (counts == null || counts.isEmpty()) {
            return Optional.empty();
        }
        
        // Sum all category counts for a total contribution
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            return Optional.empty();
        }
        
        String severity = determineCategorySeverity(counts);
        BigDecimal contribution = BigDecimal.valueOf(total);
        String description = formatDescription(config.getDescriptionTemplate(), 
                BigDecimal.valueOf(total), signal.getMetricKey());
        
        return Optional.of(DebtContribution.builder()
                .signalId(signal.getId())
                .metricKey(signal.getMetricKey())
                .dimension(config.getDimension())
                .contribution(contribution)
                .severity(severity)
                .description(description)
                .build());
    }
    
    private Optional<DebtContribution> computeBooleanDebt(Signal signal, DebtSignalContributionDocument config) {
        Boolean value = signal.getBooleanValue();
        if (value == null || value) {
            // No debt if true or null
            return Optional.empty();
        }
        
        return Optional.of(DebtContribution.builder()
                .signalId(signal.getId())
                .metricKey(signal.getMetricKey())
                .dimension(config.getDimension())
                .contribution(BigDecimal.ONE)
                .severity("MEDIUM")
                .description(formatDescription(config.getDescriptionTemplate(), 
                        BigDecimal.ZERO, signal.getMetricKey()))
                .build());
    }
    
    private Optional<DebtContribution> computeEnumDebt(Signal signal, DebtSignalContributionDocument config) {
        String value = signal.getEnumValue();
        if (value == null) {
            return Optional.empty();
        }
        
        Map<String, Object> thresholds = config.getSeverityThresholds();
        if (thresholds == null || !thresholds.containsKey(value)) {
            return Optional.empty();
        }
        
        return Optional.of(DebtContribution.builder()
                .signalId(signal.getId())
                .metricKey(signal.getMetricKey())
                .dimension(config.getDimension())
                .contribution(BigDecimal.ONE)
                .severity(value)
                .description(formatDescription(config.getDescriptionTemplate(), 
                        BigDecimal.ZERO, signal.getMetricKey()))
                .build());
    }
    
    private String determineSeverity(double value, Map<String, Object> thresholds) {
        Double critical = getThreshold(thresholds, "critical");
        Double high = getThreshold(thresholds, "high");
        Double medium = getThreshold(thresholds, "medium");
        
        if (critical != null && value <= critical) return "CRITICAL";
        if (high != null && value <= high) return "HIGH";
        if (medium != null && value <= medium) return "MEDIUM";
        return null;
    }
    
    private String determineCategorySeverity(Map<String, Integer> counts) {
        if (counts.getOrDefault("CRITICAL", 0) > 0) return "CRITICAL";
        if (counts.getOrDefault("HIGH", 0) > 0) return "HIGH";
        if (counts.getOrDefault("MEDIUM", 0) > 0) return "MEDIUM";
        return "LOW";
    }
    
    private Double getThreshold(Map<String, Object> thresholds, String key) {
        Object value = thresholds.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }
    
    private BigDecimal calculateContribution(BigDecimal value, String severity) {
        // Simple contribution based on severity
        return switch (severity) {
            case "CRITICAL" -> BigDecimal.valueOf(100).subtract(value);
            case "HIGH" -> BigDecimal.valueOf(80).subtract(value);
            case "MEDIUM" -> BigDecimal.valueOf(60).subtract(value);
            default -> BigDecimal.ZERO;
        };
    }
    
    private String formatDescription(String template, BigDecimal value, String metricKey) {
        if (template == null || template.isBlank()) {
            return String.format("Debt contribution from %s", metricKey);
        }
        return template
                .replace("{value}", value.toString())
                .replace("{metricKey}", metricKey);
    }
}
