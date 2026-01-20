package com.org.healthscore.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScoreRepository extends MongoRepository<ScoreDocument, String> {
    
    Optional<ScoreDocument> findTopByEntityTypeAndEntityIdOrderByComputedAtDesc(
            String entityType, String entityId);
    
    List<ScoreDocument> findByEntityTypeAndEntityIdOrderByComputedAtDesc(
            String entityType, String entityId);
}
