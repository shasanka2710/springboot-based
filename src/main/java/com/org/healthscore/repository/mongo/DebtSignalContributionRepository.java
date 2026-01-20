package com.org.healthscore.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DebtSignalContributionRepository extends MongoRepository<DebtSignalContributionDocument, String> {
    
    List<DebtSignalContributionDocument> findByEnabled(boolean enabled);
    
    DebtSignalContributionDocument findByMetricKey(String metricKey);
    
    List<DebtSignalContributionDocument> findByDimensionAndEnabled(String dimension, boolean enabled);
}
