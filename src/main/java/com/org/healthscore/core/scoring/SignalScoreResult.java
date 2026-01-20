package com.org.healthscore.core.scoring;

import com.org.healthscore.domain.CanonicalForm;
import com.org.healthscore.domain.Signal;
import java.math.BigDecimal;

/**
 * Result of scoring a single signal.
 */
public record SignalScoreResult(
        String signalId,
        String metricKey,
        CanonicalForm canonicalForm,
        String operator,
        String dimension,
        BigDecimal score,
        BigDecimal weight,
        BigDecimal weightedScore
) {
    public static SignalScoreResult of(Signal signal, String operator, String dimension, 
                                        BigDecimal score, BigDecimal weight) {
        return new SignalScoreResult(
                signal.getId(),
                signal.getMetricKey(),
                signal.getCanonicalForm(),
                operator,
                dimension,
                score,
                weight,
                score.multiply(weight)
        );
    }
}
