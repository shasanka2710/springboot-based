package com.org.healthscore.core.operators;

import com.org.healthscore.domain.CanonicalForm;
import com.org.healthscore.domain.Signal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WeightedCategorySumOperatorTest {

    private final WeightedCategorySumOperator operator = new WeightedCategorySumOperator();

    @Test
    void shouldCalculateWeightedSum() {
        Signal signal = Signal.builder()
                .metricKey("bugs_by_severity")
                .canonicalForm(CanonicalForm.COUNTABLE_CATEGORY)
                .countableValue(Map.of("CRITICAL", 2, "HIGH", 5, "MEDIUM", 10))
                .build();

        Map<String, Object> parameters = Map.of(
                "weights", Map.of("CRITICAL", -20, "HIGH", -10, "MEDIUM", -5),
                "baseScore", 100
        );

        BigDecimal score = operator.compute(signal, parameters);
        // 100 + (2 * -20) + (5 * -10) + (10 * -5) = 100 - 40 - 50 - 50 = -40 -> clamped to 0
        assertEquals(BigDecimal.ZERO.setScale(2), score);
    }

    @Test
    void shouldClampToMinMax() {
        Signal signal = Signal.builder()
                .metricKey("bugs_by_severity")
                .canonicalForm(CanonicalForm.COUNTABLE_CATEGORY)
                .countableValue(Map.of("LOW", 5))
                .build();

        Map<String, Object> parameters = Map.of(
                "weights", Map.of("LOW", 10),
                "baseScore", 100,
                "maxScore", 100
        );

        BigDecimal score = operator.compute(signal, parameters);
        assertEquals(BigDecimal.valueOf(100).setScale(2), score);
    }
}
