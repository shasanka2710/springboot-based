package com.org.healthscore.core.scoring;

import com.org.healthscore.core.operators.OperatorRegistry;
import com.org.healthscore.core.operators.ScoringOperator;
import com.org.healthscore.domain.CanonicalForm;
import com.org.healthscore.domain.Signal;
import com.org.healthscore.repository.mongo.SignalScoringRuleDocument;
import com.org.healthscore.repository.mongo.SignalScoringRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Scores individual signals using configuration-driven rules.
 * 
 * Uses Strategy pattern with operators fixed in code.
 * Parameters come from MongoDB (signal_scoring_rules collection).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignalScoringService {
    
    private final OperatorRegistry operatorRegistry;
    private final SignalScoringRuleRepository scoringRuleRepository;
    
    /**
     * Score a signal using the configured rule.
     * 
     * @param signal The normalized signal to score
     * @return Score result, or empty if no rule exists for this signal
     */
    public Optional<SignalScoreResult> scoreSignal(Signal signal) {
        SignalScoringRuleDocument rule = scoringRuleRepository.findByMetricKey(signal.getMetricKey());
        
        if (rule == null || !rule.isEnabled()) {
            log.debug("No active scoring rule for metric: {}", signal.getMetricKey());
            return Optional.empty();
        }
        
        return scoreSignalWithRule(signal, rule);
    }
    
    /**
     * Score a signal using a specific rule.
     */
    public Optional<SignalScoreResult> scoreSignalWithRule(Signal signal, SignalScoringRuleDocument rule) {
        // Validate canonical form compatibility
        CanonicalForm requiredForm = CanonicalForm.valueOf(rule.getRequiredCanonicalForm());
        if (signal.getCanonicalForm() != requiredForm) {
            log.warn("Signal {} has form {} but rule requires {}", 
                    signal.getMetricKey(), signal.getCanonicalForm(), requiredForm);
            return Optional.empty();
        }
        
        // Get the operator
        Optional<ScoringOperator> operatorOpt = operatorRegistry.getOperator(rule.getOperator());
        if (operatorOpt.isEmpty()) {
            log.error("Unknown operator: {} for metric: {}", rule.getOperator(), signal.getMetricKey());
            return Optional.empty();
        }
        
        ScoringOperator operator = operatorOpt.get();
        
        // Validate operator supports this canonical form
        boolean supportsForm = Arrays.stream(operator.getSupportedForms())
                .anyMatch(f -> f == signal.getCanonicalForm());
        
        if (!supportsForm) {
            log.error("Operator {} does not support canonical form {}", 
                    rule.getOperator(), signal.getCanonicalForm());
            return Optional.empty();
        }
        
        // Validate parameters
        if (!operator.validateParameters(rule.getParameters())) {
            log.error("Invalid parameters for operator {} on metric {}", 
                    rule.getOperator(), signal.getMetricKey());
            return Optional.empty();
        }
        
        // Compute score
        try {
            BigDecimal score = operator.compute(signal, rule.getParameters());
            BigDecimal weight = BigDecimal.valueOf(rule.getWeight());
            
            return Optional.of(SignalScoreResult.of(
                    signal,
                    rule.getOperator(),
                    rule.getDimension(),
                    score,
                    weight
            ));
        } catch (Exception e) {
            log.error("Error computing score for signal {}: {}", signal.getMetricKey(), e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Score multiple signals.
     */
    public List<SignalScoreResult> scoreSignals(List<Signal> signals) {
        List<SignalScoreResult> results = new ArrayList<>();
        
        for (Signal signal : signals) {
            scoreSignal(signal).ifPresent(results::add);
        }
        
        return results;
    }
}
