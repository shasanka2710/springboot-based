package com.org.healthscore.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface SignalRepository extends MongoRepository<SignalDocument, String> {
    
    List<SignalDocument> findByEntityTypeAndEntityId(String entityType, String entityId);
    
    List<SignalDocument> findByEntityTypeAndEntityIdAndTimestampAfter(
            String entityType, String entityId, Instant after);
    
    List<SignalDocument> findBySourceTypeAndSourceId(String sourceType, String sourceId);
    
    SignalDocument findByEntityTypeAndEntityIdAndMetricKey(
            String entityType, String entityId, String metricKey);
}
