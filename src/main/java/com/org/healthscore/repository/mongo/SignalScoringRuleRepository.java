package com.org.healthscore.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SignalScoringRuleRepository extends MongoRepository<SignalScoringRuleDocument, String> {
    
    List<SignalScoringRuleDocument> findByEnabled(boolean enabled);
    
    List<SignalScoringRuleDocument> findByDimensionAndEnabled(String dimension, boolean enabled);
    
    SignalScoringRuleDocument findByMetricKey(String metricKey);
    
    List<SignalScoringRuleDocument> findByMetricKeyIn(List<String> metricKeys);
}
