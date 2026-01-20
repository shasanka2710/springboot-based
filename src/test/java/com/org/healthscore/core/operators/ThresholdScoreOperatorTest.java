package com.org.healthscore.core.operators;

import com.org.healthscore.domain.CanonicalForm;
import com.org.healthscore.domain.Signal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ThresholdScoreOperatorTest {

    private final ThresholdScoreOperator operator = new ThresholdScoreOperator();

    @Test
    void shouldScoreWithinThreshold() {
        Signal signal = Signal.builder()
                .metricKey("coverage")
                .canonicalForm(CanonicalForm.SCALAR)
                .scalarValue(BigDecimal.valueOf(85.0))
                .build();

        Map<String, Object> parameters = Map.of(
                "thresholds", List.of(
                        Map.of("min", 80, "max", 100, "score", 100),
                        Map.of("min", 60, "max", 79.99, "score", 75),
                        Map.of("min", 0, "max", 59.99, "score", 50)
                )
        );

        BigDecimal score = operator.compute(signal, parameters);
        assertEquals(BigDecimal.valueOf(100).setScale(2), score);
    }

    @Test
    void shouldReturnDefaultScoreWhenNoMatch() {
        Signal signal = Signal.builder()
                .metricKey("coverage")
                .canonicalForm(CanonicalForm.SCALAR)
                .scalarValue(BigDecimal.valueOf(-5.0))
                .build();

        Map<String, Object> parameters = Map.of(
                "thresholds", List.of(
                        Map.of("min", 0, "max", 100, "score", 100)
                ),
                "defaultScore", 25
        );

        BigDecimal score = operator.compute(signal, parameters);
        assertEquals(BigDecimal.valueOf(25).setScale(2), score);
    }

    @Test
    void shouldValidateParameters() {
        assertTrue(operator.validateParameters(Map.of(
                "thresholds", List.of(Map.of("min", 0, "max", 100, "score", 100))
        )));
        
        assertFalse(operator.validateParameters(Map.of()));
        assertFalse(operator.validateParameters(null));
    }
}
