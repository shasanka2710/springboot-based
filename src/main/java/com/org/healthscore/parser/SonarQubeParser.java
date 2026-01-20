package com.org.healthscore.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for SonarQube API responses.
 * 
 * Responsibility: Handle SonarQube-specific API syntax and structure.
 * Boundary: MUST NOT count, aggregate, or apply business logic.
 *           Only extracts and normalizes payload structure.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SonarQubeParser implements ToolDataParser {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public String getSourceType() {
        return "sonarqube";
    }
    
    @Override
    public Map<String, Object> parse(String rawResponse) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(
                    rawResponse, new TypeReference<Map<String, Object>>() {});
            return parse(parsed);
        } catch (Exception e) {
            log.error("Failed to parse SonarQube response: {}", e.getMessage(), e);
            return Map.of();
        }
    }
    
    @Override
    public Map<String, Object> parse(Map<String, Object> rawResponse) {
        Map<String, Object> normalized = new HashMap<>();
        
        // Extract metrics from SonarQube's component measures response
        if (rawResponse.containsKey("component")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> component = (Map<String, Object>) rawResponse.get("component");
            
            if (component.containsKey("measures")) {
                @SuppressWarnings("unchecked")
                var measures = (java.util.List<Map<String, Object>>) component.get("measures");
                
                Map<String, Object> metrics = new HashMap<>();
                for (Map<String, Object> measure : measures) {
                    String metric = (String) measure.get("metric");
                    Object value = measure.get("value");
                    metrics.put(metric, value);
                }
                normalized.put("metrics", metrics);
            }
        }
        
        // Extract issues as a LIST (not aggregated) - aggregation happens in adapter
        // Parser only normalizes structure, does NOT count or group
        if (rawResponse.containsKey("issues")) {
            @SuppressWarnings("unchecked")
            var issues = (java.util.List<Map<String, Object>>) rawResponse.get("issues");
            
            // Extract severity values as a flat list for adapter to process
            List<String> severities = new ArrayList<>();
            for (Map<String, Object> issue : issues) {
                String severity = (String) issue.get("severity");
                if (severity != null) {
                    severities.add(severity);
                }
            }
            normalized.put("issues_severities", severities);
            
            // Also include raw issue count for reference
            normalized.put("issues_total", issues.size());
        }
        
        return normalized;
    }
}
