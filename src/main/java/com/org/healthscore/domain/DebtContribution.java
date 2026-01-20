package com.org.healthscore.domain;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Represents how a signal contributes to technical debt.
 * 
 * Debt contributions are computed based on signal values and
 * debt_signal_contributions configuration from MongoDB.
 */
@Data
@Builder
public class DebtContribution {
    
    private String signalId;
    private String metricKey;
    private String dimension;
    private BigDecimal contribution;
    private String severity;
    private String description;
}
