package com.org.healthscore.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * API request for integrating external tools and computing health scores.
 * 
 * This request triggers the full flow:
 * External Tool → Parser → Adapter → Scoring → Debt → HealthScore
 */
@Data
public class ToolIntegrationRequest {
    
    @NotBlank(message = "Entity type is required")
    private String entityType;
    
    @NotBlank(message = "Entity ID is required")
    private String entityId;
    
    @NotEmpty(message = "At least one tool must be specified")
    private List<String> tools;
    
    /**
     * Tool-specific configuration.
     * For SonarQube: {"sonarqube": {"componentKey": "my-project"}}
     */
    private Map<String, Map<String, String>> toolConfig;
}
