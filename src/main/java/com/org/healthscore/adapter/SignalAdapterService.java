package com.org.healthscore.adapter;

import com.org.healthscore.domain.CanonicalForm;
import com.org.healthscore.domain.Signal;
import com.org.healthscore.repository.mongo.AdapterSignalDefinitionDocument;
import com.org.healthscore.repository.mongo.AdapterSignalDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Base adapter service for normalizing external tool data into canonical signals.
 * 
 * Adapters are config-driven:
 * - Extraction rules come from MongoDB (adapter_signal_definitions)
 * - Adapters only normalize data, no scoring or business meaning
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignalAdapterService {
    
    private final AdapterSignalDefinitionRepository definitionRepository;
    
    /**
     * Adapt raw tool data into canonical signals.
     * 
     * @param sourceType The source tool (e.g., "sonarqube", "jira")
     * @param sourceId Unique identifier from the source
     * @param entityType Entity type (e.g., "project")
     * @param entityId Entity identifier
     * @param rawData Raw data from the external tool
     * @return List of normalized signals
     */
    public List<Signal> adaptToSignals(String sourceType, String sourceId, 
                                        String entityType, String entityId,
                                        Map<String, Object> rawData) {
        List<Signal> signals = new ArrayList<>();
        
        // Get all active signal definitions for this source
        List<AdapterSignalDefinitionDocument> definitions = 
                definitionRepository.findBySourceTypeAndEnabled(sourceType, true);
        
        if (definitions.isEmpty()) {
            log.warn("No signal definitions found for source type: {}", sourceType);
            return signals;
        }
        
        for (AdapterSignalDefinitionDocument definition : definitions) {
            try {
                Signal signal = adaptSignal(definition, sourceId, entityType, entityId, rawData);
                if (signal != null && signal.isValid()) {
                    signals.add(signal);
                }
            } catch (Exception e) {
                log.error("Error adapting signal {} from {}: {}", 
                        definition.getMetricKey(), sourceType, e.getMessage(), e);
            }
        }
        
        return signals;
    }
    
    private Signal adaptSignal(AdapterSignalDefinitionDocument definition,
                               String sourceId, String entityType, String entityId,
                               Map<String, Object> rawData) {
        // Extract value using configured path
        Object extractedValue = extractValue(rawData, definition.getExtractionPath());
        if (extractedValue == null) {
            log.debug("No value extracted for metric {} using path {}", 
                    definition.getMetricKey(), definition.getExtractionPath());
            return null;
        }
        
        CanonicalForm form = CanonicalForm.valueOf(definition.getCanonicalForm());
        
        Signal.SignalBuilder builder = Signal.builder()
                .id(UUID.randomUUID().toString())
                .sourceType(definition.getSourceType())
                .sourceId(sourceId)
                .metricKey(definition.getMetricKey())
                .canonicalForm(form)
                .timestamp(Instant.now())
                .metadata(Map.of("entityType", entityType, "entityId", entityId));
        
        // Set value based on canonical form
        switch (form) {
            case COUNTABLE_CATEGORY -> builder.countableValue(
                    normalizeToCountable(extractedValue, definition.getCategoryMappings()));
            case SCALAR -> builder.scalarValue(
                    normalizeToScalar(extractedValue, definition.getTransformation()));
            case BOOLEAN -> builder.booleanValue(normalizeToBoolean(extractedValue));
            case ENUM -> builder.enumValue(normalizeToEnum(extractedValue));
        }
        
        return builder.build();
    }
    
    /**
     * Extract value from raw data using a simple path expression.
     * Supports dot notation for nested access (e.g., "metrics.coverage")
     */
    private Object extractValue(Map<String, Object> data, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        
        String[] parts = path.split("\\.");
        Object current = data;
        
        for (String part : parts) {
            if (current instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) current;
                current = map.get(part);
            } else {
                return null;
            }
        }
        
        return current;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Integer> normalizeToCountable(Object value, Map<String, String> mappings) {
        Map<String, Integer> result = new HashMap<>();
        
        if (value instanceof Map) {
            // Handle pre-aggregated map (legacy support)
            Map<String, Object> map = (Map<String, Object>) value;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                // Apply category mapping if configured
                if (mappings != null && mappings.containsKey(key)) {
                    key = mappings.get(key);
                }
                if (entry.getValue() instanceof Number) {
                    result.put(key, ((Number) entry.getValue()).intValue());
                }
            }
        } else if (value instanceof List) {
            // Handle list of values - count occurrences per category
            // This is the config-driven aggregation for COUNTABLE_CATEGORY
            List<?> list = (List<?>) value;
            for (Object item : list) {
                if (item != null) {
                    String key = item.toString();
                    // Apply category mapping if configured
                    if (mappings != null && mappings.containsKey(key)) {
                        key = mappings.get(key);
                    }
                    result.merge(key, 1, Integer::sum);
                }
            }
        }
        
        return result;
    }
    
    private BigDecimal normalizeToScalar(Object value, 
                                          AdapterSignalDefinitionDocument.TransformationConfig transform) {
        BigDecimal decimal;
        
        if (value instanceof Number) {
            decimal = BigDecimal.valueOf(((Number) value).doubleValue());
        } else if (value instanceof String) {
            decimal = new BigDecimal((String) value);
        } else {
            return null;
        }
        
        // Apply transformation if configured
        if (transform != null) {
            decimal = applyTransformation(decimal, transform);
        }
        
        return decimal;
    }
    
    private BigDecimal applyTransformation(BigDecimal value, 
                                           AdapterSignalDefinitionDocument.TransformationConfig transform) {
        if (transform.getType() == null) {
            return value;
        }
        
        return switch (transform.getType()) {
            case "percentage" -> value; // Already percentage
            case "invert" -> BigDecimal.valueOf(100).subtract(value);
            case "scale" -> transform.getFactor() != null 
                    ? value.multiply(BigDecimal.valueOf(transform.getFactor()))
                    : value;
            default -> value;
        };
    }
    
    private Boolean normalizeToBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        } else if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        return null;
    }
    
    private String normalizeToEnum(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString().toUpperCase();
    }
}
