package com.org.healthscore.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AdapterSignalDefinitionRepository extends MongoRepository<AdapterSignalDefinitionDocument, String> {
    
    List<AdapterSignalDefinitionDocument> findBySourceTypeAndEnabled(String sourceType, boolean enabled);
    
    List<AdapterSignalDefinitionDocument> findByEnabled(boolean enabled);
    
    AdapterSignalDefinitionDocument findBySourceTypeAndMetricKey(String sourceType, String metricKey);
}
