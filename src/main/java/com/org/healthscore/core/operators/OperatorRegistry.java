package com.org.healthscore.core.operators;

import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of all available scoring operators.
 * 
 * Operators are auto-discovered via Spring DI.
 * This ensures only code-defined operators are available.
 */
@Component
public class OperatorRegistry {
    
    private final Map<String, ScoringOperator> operators = new HashMap<>();
    private final List<ScoringOperator> operatorBeans;
    
    public OperatorRegistry(List<ScoringOperator> operatorBeans) {
        this.operatorBeans = operatorBeans;
    }
    
    @PostConstruct
    public void init() {
        for (ScoringOperator operator : operatorBeans) {
            operators.put(operator.getOperatorId(), operator);
        }
    }
    
    /**
     * Get an operator by its ID.
     */
    public Optional<ScoringOperator> getOperator(String operatorId) {
        return Optional.ofNullable(operators.get(operatorId));
    }
    
    /**
     * Check if an operator exists.
     */
    public boolean hasOperator(String operatorId) {
        return operators.containsKey(operatorId);
    }
    
    /**
     * Get all registered operator IDs.
     */
    public List<String> getAvailableOperatorIds() {
        return List.copyOf(operators.keySet());
    }
}
