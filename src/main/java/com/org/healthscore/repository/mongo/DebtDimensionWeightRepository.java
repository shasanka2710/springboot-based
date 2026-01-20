package com.org.healthscore.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DebtDimensionWeightRepository extends MongoRepository<DebtDimensionWeightDocument, String> {
    
    List<DebtDimensionWeightDocument> findByEntityTypeOrderByDisplayOrder(String entityType);
    
    DebtDimensionWeightDocument findByEntityTypeAndDimension(String entityType, String dimension);
}
